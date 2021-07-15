package com.fbu.pbluc.artgal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.fbu.pbluc.artgal.adapters.MarkersAdapter;
import com.fbu.pbluc.artgal.listeners.EndlessRecyclerViewScrollListener;
import com.fbu.pbluc.artgal.models.Marker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

public class UploadedMarkersActivity extends AppCompatActivity implements MarkersAdapter.ListItemClickListener {

    private static final String TAG = "UploadedMarkersActivity";
    public final int QUERY_LIMIT = 5;

    private RecyclerView rvMarkers;
    private ImageView ivAddMarker;

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
                .collection("users")
                .document(currentUser.getUid())
                .collection("uploadedMarkers");

        rvMarkers = findViewById(R.id.rvMarkers);
        ivAddMarker = findViewById(R.id.ivAddMarker);

        // Initialize markers
        markers = new ArrayList<>();
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

        ivAddMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goAddMarkerActivity();
            }
        });

        rvMarkers.addOnScrollListener(scrollListener);

        queryMarkers();
    }

    private void loadNextDataFromDatabase() {
        // Send the request
        Task<QuerySnapshot> query = markersRef
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .whereLessThan("createdAt", markers.get(markers.size() - 1).getCreatedAt())
                .limit(QUERY_LIMIT)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
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
                        } else {
                            Log.e(TAG, "Error getting marker documents", task.getException());
                        }
                    }
                });
    }

    private void goAddMarkerActivity() {
        Intent intent = new Intent(UploadedMarkersActivity.this, AddMarkerActivity.class);;
        startActivity(intent);
    }

    private void queryMarkers() {
        // Create a query against the collection
        Task<QuerySnapshot> query = markersRef
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(QUERY_LIMIT)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Marker> resultMarkers = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Marker resultMarker = document.toObject(Marker.class);
                                resultMarkers.add(resultMarker);
                            }
                            markers.addAll(resultMarkers);
                            adapter.notifyDataSetChanged();
                        } else {
                            Log.e(TAG, "Error getting marker documents", task.getException());
                        }
                    }
                });
    }

    @Override
    public void onListItemClick(int position) {
        // Start intent to go to marker details activity
        Marker clickedMarker = markers.get(position);
        Intent intent = new Intent(UploadedMarkersActivity.this, MarkerDetailsActivity.class);
        intent.putExtra("userMarkerUid", clickedMarker.getMarkerImg().get("fileName").toString().substring(0, 28));
        intent.putExtra("clickedMarkerUid", clickedMarker.getMarkerImg().get("fileName").toString().substring(29, 49));
        startActivity(intent);
    }
}