package com.fbu.pbluc.artgal.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fbu.pbluc.artgal.MarkerDetailsActivity;
import com.fbu.pbluc.artgal.R;
import com.fbu.pbluc.artgal.adapters.MarkersAdapter;
import com.fbu.pbluc.artgal.listeners.EndlessRecyclerViewScrollListener;
import com.fbu.pbluc.artgal.models.Marker;
import com.fbu.pbluc.artgal.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static android.app.Activity.RESULT_OK;

public class UploadedMarkersFragment extends Fragment implements MarkersAdapter.ListItemClickListener {

  private static final String TAG = "UploadedMarkersFragment";

  private static final int GO_TO_MARKER_DETAILS_REQUEST_CODE = 30;
  public final int QUERY_LIMIT = 8;

  private RecyclerView rvMarkers;
  private Button btnDeleteSelectedMarkers;
  private SwipeRefreshLayout swipeContainer;
  private ProgressBar progressBarLoading;

  private EndlessRecyclerViewScrollListener scrollListener;

  private List<Marker> markers;
  private MarkersAdapter adapter;

  private FirebaseAuth firebaseAuth;
  private FirebaseUser currentUser;
  private FirebaseFirestore firebaseFirestore;
  private FirebaseStorage firebaseStorage;
  private StorageReference storageReference;
  private DocumentReference currentUserDoc;
  private CollectionReference markersRef;

  public UploadedMarkersFragment() {
    // Required empty public constructor
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_uploaded_markers, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    firebaseAuth = FirebaseAuth.getInstance();
    currentUser = firebaseAuth.getCurrentUser();
    firebaseFirestore = FirebaseFirestore.getInstance();
    firebaseStorage = FirebaseStorage.getInstance();
    storageReference = firebaseStorage.getReference();

    currentUserDoc = firebaseFirestore
        .collection(User.KEY_USERS)
        .document(currentUser.getUid());

    // Create a reference to the uploadedMarkers subcollection
    markersRef = currentUserDoc
        .collection(Marker.KEY_UPLOADED_MARKERS);

    rvMarkers = view.findViewById(R.id.rvMarkers);
    btnDeleteSelectedMarkers = view.findViewById(R.id.btnDeleteSelectedMarkers);
    swipeContainer = view.findViewById(R.id.swipeContainer);
    progressBarLoading = view.findViewById(R.id.pbLoading);

    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());

    scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
      @Override
      public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
        loadNextDataFromDatabase();
      }
    };
    // Set layout manager to position the items
    rvMarkers.setLayoutManager(linearLayoutManager);
    rvMarkers.addOnScrollListener(scrollListener);

    // Initialize markers
    markers = new CopyOnWriteArrayList<>();
    // Create adapter
    adapter = new MarkersAdapter(markers, getContext(), this);
    // Attach the adapter to the recyclerview to populate items
    rvMarkers.setAdapter(adapter);

    swipeContainer.setOnRefreshListener(() -> fetchUploadedMarkersAsync());

    btnDeleteSelectedMarkers.setOnClickListener(v -> deleteSelectedMarkers());

    progressBarLoading.setVisibility(ProgressBar.VISIBLE);
    queryMarkers();
  }

  private void deleteSelectedMarkers() {
    progressBarLoading.setVisibility(ProgressBar.VISIBLE);

    // Disable user interaction
    getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

    WriteBatch deleteBatch = firebaseFirestore.batch();
    List<Marker> selected = getSelectedItems();
    List<Integer> indicesToRemoveFromAdapter = new ArrayList<>();

    Log.i(TAG, "Selected items before removing: " + selected);
    Log.i(TAG, "Marker adapter list before removing: " + markers);

    StorageReference referenceImgsRef = storageReference.child(getString(R.string.reference_images_ref));
    StorageReference augmentedObjsRef = storageReference.child(getString(R.string.augmented_object_ref));

    for(int i = 0; i < selected.size(); i++) {
      Marker selectedMarker = selected.get(i);

      // Get file names of reference image and augmented object file
      String selectedMarkerReferenceImgFileName = selectedMarker.getMarkerImg().get(Marker.KEY_FILENAME).toString();
      String selectedMarkerAugmentedObjFileName = selectedMarker.getAugmentedObj().get(Marker.KEY_FILENAME).toString();
      // Get the marker uid of selected marker
      String selectedMarkerUid = selectedMarkerReferenceImgFileName.substring(29, 49);

      // Get selected marker document
      DocumentReference selectedMarkerDoc = markersRef.document(selectedMarkerUid);

      // Delete selected marker document
      deleteBatch.delete(selectedMarkerDoc);

      // Update list and notify adapter
      int indexRemove = markers.indexOf(selectedMarker);
      indicesToRemoveFromAdapter.add(indexRemove);

      // Delete files in Storage
      referenceImgsRef
          .child(selectedMarkerReferenceImgFileName)
          .delete()
          .addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
              augmentedObjsRef
                  .child(selectedMarkerAugmentedObjFileName)
                  .delete()
                  .addOnCompleteListener(task1 -> {
                    if(!task1.isSuccessful()) {
                      Log.e(TAG, "onFailure: Could not delete augmented object", task1.getException());
                    }
                  });
            } else {
              Log.e(TAG, "onFailure: Could not delete reference image", task.getException());
            }
          });
    }

    deleteBatch
        .commit()
        .addOnSuccessListener(unused -> {
          // Retain the user interaction
          getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
          // Refresh RecyclerView contents
          fetchUploadedMarkersAsync();

          // Update the adapter and list
          for (int i:
              indicesToRemoveFromAdapter) {
            markers.remove(i);
            adapter.notifyItemRemoved(i);
          }
          Log.i(TAG, "Selected items after removing: " + selected);
          Log.i(TAG, "Marker adapter list after removing: " + markers);

          // Update the current user's user document in Firestore
          updateUserDocumentUpdatedAtField();

          progressBarLoading.setVisibility(ProgressBar.GONE);
          Toast.makeText(getContext(), "Successfully deleted selected markers!", Toast.LENGTH_SHORT).show();
        })
        .addOnFailureListener(e -> Log.e(TAG, "Could not delete all selected documents", e));

  }

  private void updateUserDocumentUpdatedAtField() {
    firebaseFirestore
        .collection(User.KEY_USERS)
        .document(currentUser.getUid())
        .update(User.KEY_UPDATED_AT, FieldValue.serverTimestamp())
        .addOnCompleteListener(task -> {
          if (task.isSuccessful()) {
            Log.i(TAG, "onSuccess: Updated the updatedAt field on user document after removing selected uploaded markers");
          } else {
            Log.i(TAG, "onFailure: Could not update user document after removing selected markers", task.getException());
          }
        });
  }

  private void queryMarkers() {
    // Create a query against the collection
    markersRef
        .orderBy(Marker.KEY_CREATED_AT, Query.Direction.DESCENDING)
        .limit(QUERY_LIMIT)
        .get()
        .addOnCompleteListener(task -> {
          if (task.isSuccessful()) {
            List<Marker> resultMarkers = new ArrayList<>();
            for (QueryDocumentSnapshot document : task.getResult()) {
              Marker resultMarker = document.toObject(Marker.class);
              resultMarkers.add(resultMarker);
            }
            markers.addAll(resultMarkers);
            adapter.notifyDataSetChanged();
            Log.i(TAG, "markers after initial query: " + markers.toString());
          } else {
            Log.e(TAG, "Error getting marker documents", task.getException());
          }
          progressBarLoading.setVisibility(ProgressBar.GONE);
        });
  }

  private void loadNextDataFromDatabase() {
    // Send the request
    markersRef
        .orderBy(Marker.KEY_CREATED_AT, Query.Direction.DESCENDING)
        .whereLessThan(Marker.KEY_CREATED_AT, markers.get(markers.size() - 1).getCreatedAt())
        .limit(QUERY_LIMIT)
        .get()
        .addOnCompleteListener(task -> {
          int expectedLoadCount = task.getResult().size();
          if (task.isSuccessful()) {
            // Deserialize and construct new model objects from the query response
            List<Marker> resultMarkers = new ArrayList<>();
            for (QueryDocumentSnapshot document : task.getResult()) {
              Marker resultMarker = document.toObject(Marker.class);
              resultMarkers.add(resultMarker);

              if (resultMarkers.size() == expectedLoadCount) {
                int positionInserted = markers.size();
                // Append the new data objects to the existing set of items inside the array of items
                markers.addAll(resultMarkers);
                // Notify the adapter of the new items made with 'notifyItemRangeInserted()'
                adapter.notifyItemRangeInserted(positionInserted, resultMarkers.size());
              }
            }
          } else {
            Log.e(TAG, "Error getting marker documents", task.getException());
          }
        });
  }

  private void fetchUploadedMarkersAsync() {
    markersRef
        .orderBy(Marker.KEY_CREATED_AT, Query.Direction.DESCENDING)
        .limit(QUERY_LIMIT)
        .get()
        .addOnCompleteListener(task -> {
          if (task.isSuccessful()) {
            List<Marker> resultMarkers = new ArrayList<>();
            for (QueryDocumentSnapshot document : task.getResult()) {
              Marker resultMarker = document.toObject(Marker.class);
              resultMarkers.add(resultMarker);
            }
            adapter.clear();
            adapter.addAll(resultMarkers);
            scrollListener.resetState();
            Log.i(TAG, "markers after refresh: " + markers.toString());
          } else {
            Log.e(TAG, "Error getting marker documents", task.getException());
          }
          swipeContainer.setRefreshing(false);
        });
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (resultCode == RESULT_OK && data != null) {
      switch (requestCode) {
        case GO_TO_MARKER_DETAILS_REQUEST_CODE:
          if (getActivity().getIntent().getBooleanExtra(getString(R.string.edited_viewed_marker), false) == false) {
            String deletedMarkerUid = data.getStringExtra(getString(R.string.deleted_marker_uid));
            Log.i(TAG, "deletedMarkerUid: " + deletedMarkerUid);

            int index = 0;
            for (Marker m : markers) {
              if (m.getMarkerImg().get(Marker.KEY_FILENAME).toString().substring(29, 49).equals(deletedMarkerUid)) {
                markers.remove(m);
                adapter.notifyItemRemoved(index);
                Log.i(TAG, "removed marker at index: " + index);
              }
              index += 1;
            }

            Log.i(TAG, "Markers: " + markers.toString());
            break;
          } else {
            fetchUploadedMarkersAsync();
          }
        default:
          break;
      }
    }
  }

  @Override
  public void onListItemClick(int position) {
    // Start intent to go to marker details activity
    Marker clickedMarker = markers.get(position);

    Intent intent = new Intent(getActivity(), MarkerDetailsActivity.class);
    intent.putExtra(getString(R.string.user_marker_uid), clickedMarker.getUser().getId());
    intent.putExtra(getString(R.string.clicked_marker_uid), clickedMarker.getMarkerImg().get(Marker.KEY_FILENAME).toString().substring(29, 49));

    startActivityForResult(intent, GO_TO_MARKER_DETAILS_REQUEST_CODE);
  }

  @Override
  public void onListItemLongClick(int position, View view) {
    Marker longClickedMarker = markers.get(position);
    longClickedMarker.setSelected(!longClickedMarker.isSelected());
    view.setBackgroundColor(longClickedMarker.isSelected() ? Color.GRAY : Color.WHITE); // TODO: Change back to the original background color

    if (getSelectedItems().size() > 0) {
      // Show view of delete all button
      btnDeleteSelectedMarkers.setVisibility(View.VISIBLE);
    } else {
      // Hide view of delete all button
      btnDeleteSelectedMarkers.setVisibility(View.GONE);
    }
  }

  @Override
  public void onLikeClick(int position) {
    //  Add or remove marker document from current users likedMarkers array
    updateCurrentUsersLiked(position);
  }

  @Override
  public void onFavoriteClick(int position) {
    updateCurrentUsersFavorited(position);
  }

  private void updateCurrentUsersFavorited(int position) {
    String favoritedMarkerUid = markers.get(position).getMarkerImg().get(Marker.KEY_FILENAME).toString().substring(29, 49);
    String favoritedMarkerUserUid = markers.get(position).getUser().getId();

    DocumentReference favoritedMarkerDoc = firebaseFirestore
        .collection(User.KEY_USERS)
        .document(favoritedMarkerUserUid)
        .collection(Marker.KEY_UPLOADED_MARKERS)
        .document(favoritedMarkerUid);

    currentUserDoc
        .get()
        .addOnCompleteListener(task -> {
          if (task.isSuccessful()) {
            User currUser = task.getResult().toObject(User.class);

            // Determine if clicked marker has been favorited by this user
            if (currUser.getFavoritedMarkers() != null && currUser.getFavoritedMarkers().contains(favoritedMarkerDoc)) { // Has already been liked so we unlike
              currUser.removeFavoritedMarker(favoritedMarkerDoc);
            } else { // Other we add to the user's favorites
              if (currUser.getFavoritedMarkers() != null) {
                currUser.addFavoritedMarker(favoritedMarkerDoc);
              } else {
                ArrayList<DocumentReference> favorited = new ArrayList<>();
                favorited.add(favoritedMarkerDoc);
                currUser.setFavoritedMarkers(favorited);
              }
            }

            currentUserDoc
                .update(User.KEY_FAVORITED_MARKERS, currUser.getFavoritedMarkers())
                .addOnCompleteListener(task1 -> {
                  if (task1.isSuccessful()) {
                    adapter.notifyItemChanged(position);
                    updateUserDocumentUpdatedAtField();
                  } else {
                    Log.i(TAG, "onFailure: Could not add marker DocumentReference to current user document", task.getException());
                  }
                });
          } else {
            Log.i(TAG, "onFailure: Could not get current user Firestore document", task.getException());
          }
        });
  }

  private void updateCurrentUsersLiked(int position) {

    String likedMarkerUid = markers.get(position).getMarkerImg().get(Marker.KEY_FILENAME).toString().substring(29, 49);
    String likedMarkerUserUid = markers.get(position).getUser().getId();

    DocumentReference likedMarkerDoc = firebaseFirestore
        .collection(User.KEY_USERS)
        .document(likedMarkerUserUid)
        .collection(Marker.KEY_UPLOADED_MARKERS)
        .document(likedMarkerUid);

    currentUserDoc
        .get()
        .addOnCompleteListener(task -> {
          if (task.isSuccessful()) {
            User currUser = task.getResult().toObject(User.class);
            boolean likedAction;

            // Determine if clicked marker has been liked by this user
            if (currUser.getLikedMarkers() != null && currUser.getLikedMarkers().contains(likedMarkerDoc)) { // Has already been liked so we unlike
              currUser.removeLikedMarker(likedMarkerDoc);
              likedAction = false;
            } else { // Other we add to the user's liked
              if (currUser.getLikedMarkers() != null) {
                currUser.addLikedMarker(likedMarkerDoc);
              } else {
                ArrayList<DocumentReference> liked = new ArrayList<>();
                liked.add(likedMarkerDoc);
                currUser.setLikedMarkers(liked);
              }

              likedAction = true;
            }

            currentUserDoc
                .update(User.KEY_LIKED_MARKERS, currUser.getLikedMarkers())
                .addOnCompleteListener(task1 -> {
                  if (task1.isSuccessful()) {
                    //  Update the marker count on marker document
                    updatedLikeCountOnMarker(position, likedMarkerDoc, likedAction);
                  } else {
                    Log.i(TAG, "onFailure: Could not add marker DocumentReference to current user document", task.getException());
                  }
                });
          } else {
            Log.i(TAG, "onFailure: Could not get current user Firestore document", task.getException());
          }
        });
  }

  private void updatedLikeCountOnMarker(int position, DocumentReference likedMarkerDoc, boolean like) {
    if (markers.get(position).getLikedCount() == null) {
      markers.get(position).setLikedCount(0);
    }

    if (like) {
      markers.get(position).likeMarker();
    } else {
      markers.get(position).unlikeMarker();
    }

    int updatedLikeCount = markers.get(position).getLikedCount();
    likedMarkerDoc
        .update(Marker.KEY_LIKE_COUNT, updatedLikeCount)
        .addOnCompleteListener(task -> {
          if (task.isSuccessful()) {
            Log.i(TAG, "onSuccess: Updated like count");
            adapter.notifyItemChanged(position);
            updateUserDocumentUpdatedAtField();
          } else {
            Log.i(TAG, "onFailure: Could not update like count", task.getException());
          }
        });
  }

  private List<Marker> getSelectedItems() {
    List<Marker> selectedItems = new ArrayList<>();
    for(int i = 0; i < markers.size(); i++) {
      if (markers.get(i).isSelected()) {
        selectedItems.add(markers.get(i));
      }
    }
    return selectedItems;
  }
}
