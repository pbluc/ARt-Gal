package com.fbu.pbluc.artgal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.fbu.pbluc.artgal.adapters.MarkersAdapter;
import com.fbu.pbluc.artgal.listeners.EndlessRecyclerViewScrollListener;
import com.fbu.pbluc.artgal.models.Marker;
import com.fbu.pbluc.artgal.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class UploadedMarkersActivity extends AppCompatActivity implements MarkersAdapter.ListItemClickListener {

  private static final String TAG = "UploadedMarkersActivity";

  private static final int NEW_MARKER_REQUEST_CODE = 20;
  private static final int DELETE_MARKER_CODE = 30;
  public final int QUERY_LIMIT = 8;

  private RecyclerView rvMarkers;
  private ImageView ivAddMarker;
  private SwipeRefreshLayout swipeContainer;

  private EndlessRecyclerViewScrollListener scrollListener;

  private List<Marker> markers;
  private MarkersAdapter adapter;

  private FirebaseAuth firebaseAuth;
  private FirebaseUser currentUser;
  private FirebaseFirestore firebaseFirestore;
  private CollectionReference markersRef;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_uploaded_markers);

    firebaseAuth = FirebaseAuth.getInstance();
    currentUser = firebaseAuth.getCurrentUser();
    firebaseFirestore = FirebaseFirestore.getInstance();

    // Create a reference to the uploadedMarkers subcollection
    markersRef = firebaseFirestore
        .collection(User.KEY_USERS)
        .document(currentUser.getUid())
        .collection(Marker.KEY_UPLOADED_MARKERS);

    rvMarkers = findViewById(R.id.rvMarkers);
    ivAddMarker = findViewById(R.id.ivAddMarker);
    swipeContainer = findViewById(R.id.swipeContainer);

    // Initialize markers
    markers = new CopyOnWriteArrayList<>();
    // Create adapter
    adapter = new MarkersAdapter(markers, UploadedMarkersActivity.this, this);
    // Attach the adapter to the recyclerview to populate items
    rvMarkers.setAdapter(adapter);
    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
    // Set layout manager to position the items
    rvMarkers.setLayoutManager(linearLayoutManager);

    scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
      @Override
      public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
        Log.i(TAG, "Loading data using scrollListener");
        loadNextDataFromDatabase();
      }
    };

    swipeContainer.setOnRefreshListener(() -> fetchUploadedMarkersAsync());

    ivAddMarker.setOnClickListener(v -> goToAddMarkerActivity());

    rvMarkers.addOnScrollListener(scrollListener);

    queryMarkers();
  }

  private void queryMarkers() {
    // Create a query against the collection
    Task<QuerySnapshot> query = markersRef
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
            Log.i(TAG, "markers after query: " + markers.toString());
          } else {
            Log.e(TAG, "Error getting marker documents", task.getException());
          }
        });
  }

  private void loadNextDataFromDatabase() {
    // Send the request
    Task<QuerySnapshot> query = markersRef
        .orderBy(Marker.KEY_CREATED_AT, Query.Direction.DESCENDING)
        .whereLessThan(Marker.KEY_CREATED_AT, markers.get(markers.size() - 1).getCreatedAt())
        .limit(QUERY_LIMIT)
        .get()
        .addOnCompleteListener(task -> {
          if (task.isSuccessful()) {
            // Deserialize and construct new model objects from the query response
            List<Marker> resultMarkers = new ArrayList<>();
            for (QueryDocumentSnapshot document : task.getResult()) {
              Marker resultMarker = document.toObject(Marker.class);
              resultMarkers.add(resultMarker);
            }
            int positionInserted = markers.size();
            // Append the new data objects to the existing set of items inside the array of items
            markers.addAll(resultMarkers);
            // Notify the adapter of the new items made with 'notifyItemRangeInserted()'
            adapter.notifyItemRangeInserted(positionInserted, resultMarkers.size());
            Log.i(TAG, "markers after scrolling: " + markers.toString());
          } else {
            Log.e(TAG, "Error getting marker documents", task.getException());
          }
        });
  }

  private void fetchUploadedMarkersAsync() {
    Task<QuerySnapshot> query = markersRef
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

  private void goToAddMarkerActivity() {
    Intent intent = new Intent(UploadedMarkersActivity.this, AddMarkerActivity.class);
    intent.putExtra(getString(R.string.flag), getString(R.string.uploaded_markers_activity));
    startActivityForResult(intent, NEW_MARKER_REQUEST_CODE);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (resultCode == RESULT_OK && data != null) {
      switch (requestCode) {
        case NEW_MARKER_REQUEST_CODE:
          // Get data from the intent (marker)
          String newMarkerUid = data.getStringExtra(getString(R.string.new_marker_uid));
          Log.i(TAG, "newMarkerUid: " + newMarkerUid);
          markersRef
              .document(newMarkerUid)
              .get()
              .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                  DocumentSnapshot documentSnapshot = task.getResult();
                  if (documentSnapshot.exists()) {
                    Marker newMarker = documentSnapshot.toObject(Marker.class);

                    // Update the recycler view with the marker
                    // Modify data source of tweets
                    markers.add(0, newMarker);
                    // Update the adapter
                    adapter.notifyItemInserted(0);
                    rvMarkers.smoothScrollToPosition(0);
                    Log.i(TAG, "Markers: " + markers.toString());
                  } else {
                    Log.e(TAG, "No such document");
                  }
                } else {
                  Log.e(TAG, "get new marker document failed with ", task.getException());
                }
              });
          break;
        case DELETE_MARKER_CODE:
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
        default:
          break;
      }
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public void onListItemClick(int position) {
    // Start intent to go to marker details activity
    Marker clickedMarker = markers.get(position);

    Intent intent = new Intent(UploadedMarkersActivity.this, MarkerDetailsActivity.class);
    intent.putExtra(getString(R.string.user_marker_uid), clickedMarker.getUser().getId());
    intent.putExtra(getString(R.string.clicked_marker_uid), clickedMarker.getMarkerImg().get(Marker.KEY_FILENAME).toString().substring(29, 49));

    startActivityForResult(intent, DELETE_MARKER_CODE);
  }
}