package com.fbu.pbluc.artgal.fragments;

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
import android.widget.Button;
import android.widget.ProgressBar;

import com.fbu.pbluc.artgal.R;
import com.fbu.pbluc.artgal.adapters.MarkersAdapter;
import com.fbu.pbluc.artgal.listeners.EndlessRecyclerViewScrollListener;
import com.fbu.pbluc.artgal.models.Marker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

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

  private FirebaseFirestore firebaseFirestore;

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

    firebaseFirestore = FirebaseFirestore.getInstance();

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
    // TODO: Go to MarkerDetails Activity
  }

  @Override
  public void onListItemLongClick(int position, View view) {
    // Do nothing as you shouldn't be able to multi-select markers in feed
  }

  @Override
  public void onLikeClick(int position) {
    // TODO: Make sure user is able to like markers that is not their own
  }
}