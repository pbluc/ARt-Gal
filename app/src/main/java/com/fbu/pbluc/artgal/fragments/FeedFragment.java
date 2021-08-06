package com.fbu.pbluc.artgal.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.fbu.pbluc.artgal.MarkerDetailsActivity;
import com.fbu.pbluc.artgal.R;
import com.fbu.pbluc.artgal.adapters.MarkersAdapter;
import com.fbu.pbluc.artgal.listeners.EndlessRecyclerViewScrollListener;
import com.fbu.pbluc.artgal.models.Marker;
import com.fbu.pbluc.artgal.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FeedFragment extends Fragment implements MarkersAdapter.ListItemClickListener {

  private static final String TAG = "FeedFragment";
  public final int QUERY_LIMIT = 10;

  private RecyclerView rvMarkers;
  private SwipeRefreshLayout swipeContainer;
  private ProgressBar progressBarLoading;

  private EndlessRecyclerViewScrollListener scrollListener;

  private List<Marker> markers;
  private MarkersAdapter adapter;

  private FirebaseAuth firebaseAuth;
  private FirebaseFirestore firebaseFirestore;
  private FirebaseUser currentUser;
  private DocumentReference currentUserDoc;

  public FeedFragment() {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_feed, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    firebaseAuth = FirebaseAuth.getInstance();
    firebaseFirestore = FirebaseFirestore.getInstance();
    currentUser = firebaseAuth.getCurrentUser();

    currentUserDoc = firebaseFirestore
        .collection(User.KEY_USERS)
        .document(currentUser.getUid());

    rvMarkers = view.findViewById(R.id.rvMarkers);
    swipeContainer = view.findViewById(R.id.swipeContainer);
    progressBarLoading = view.findViewById(R.id.pbLoading);

    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());

    scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
      @Override
      public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
        loadNextDataFromDatabase();
      }
    };

    rvMarkers.setLayoutManager(linearLayoutManager);
    rvMarkers.addOnScrollListener(scrollListener);

    markers = new CopyOnWriteArrayList<>();
    adapter = new MarkersAdapter(markers, getContext(), this);
    rvMarkers.setAdapter(adapter);

    swipeContainer.setOnRefreshListener(() -> fetchUploadedMarkersAsync());

    progressBarLoading.setVisibility(ProgressBar.VISIBLE);
    queryMarkers();
  }

  private void queryMarkers() {
    firebaseFirestore
        .collectionGroup(Marker.KEY_UPLOADED_MARKERS)
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
    firebaseFirestore
        .collectionGroup(Marker.KEY_UPLOADED_MARKERS)
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
    firebaseFirestore
        .collectionGroup(Marker.KEY_UPLOADED_MARKERS);

    firebaseFirestore
        .collectionGroup(Marker.KEY_UPLOADED_MARKERS)
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
  public void onListItemClick(int position) {
    // Start intent to go to marker details activity
    Marker clickedMarker = markers.get(position);

    Intent intent = new Intent(getActivity(), MarkerDetailsActivity.class);
    intent.putExtra(getString(R.string.user_marker_uid), clickedMarker.getUser().getId());
    intent.putExtra(getString(R.string.clicked_marker_uid), clickedMarker.getMarkerImg().get(Marker.KEY_FILENAME).toString().substring(29, 49));

    startActivity(intent);
  }

  @Override
  public void onListItemLongClick(int position, View view) {
    // Do nothing as you shouldn't be able to multi-select markers in feed
  }

  @Override
  public void onLikeClick(int position) {
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
}