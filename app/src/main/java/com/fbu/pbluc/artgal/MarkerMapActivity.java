package com.fbu.pbluc.artgal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.icu.text.Transliterator;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.fbu.pbluc.artgal.models.Marker;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

@RuntimePermissions
public class MarkerMapActivity extends AppCompatActivity implements GoogleMap.OnMarkerDragListener {

  private static final String TAG = "MarkerMapActivity";
  private FirebaseFirestore firebaseFirestore;

  private Button btnDoneSettingLoc;

  private SupportMapFragment mapFragment;
  private GoogleMap map;
  private LocationRequest mLocationRequest;
  Location mCurrentLocation;
  private long UPDATE_INTERVAL = 60000;  /* 60 secs */
  private long FASTEST_INTERVAL = 5000; /* 5 secs */

  private final static String KEY_LOC = "location";

  private com.google.android.gms.maps.model.Marker placedMarker;

  /*
   * Define a request code to send to Google Play services This code is
   * returned in Activity.onActivityResult
   */
  private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_marker_map);

    btnDoneSettingLoc = findViewById(R.id.btnDoneSettingLoc);

    firebaseFirestore = FirebaseFirestore.getInstance();

    if (savedInstanceState != null && savedInstanceState.keySet().contains(KEY_LOC)) {
      // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation
      // is not null.
      mCurrentLocation = savedInstanceState.getParcelable(KEY_LOC);
    }

    btnDoneSettingLoc.setOnClickListener(v -> {
      double[] latLng = {placedMarker.getPosition().latitude, placedMarker.getPosition().longitude};
      Intent returnIntent = new Intent();
      returnIntent.putExtra(getString(R.string.new_marker_latlng), latLng);
      setResult(Activity.RESULT_OK, returnIntent);
      finish();
    });

    setUpMapIfNeeded();
  }

  protected void setUpMapIfNeeded() {
    // Do a null check to confirm that we have not already instantiated the map
    if (mapFragment == null) {
      mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment));
      // Check if we were successful in obtaining the map
      if (mapFragment != null) {
        mapFragment.getMapAsync(new OnMapReadyCallback() {
          @Override
          public void onMapReady(GoogleMap googleMap) {
            loadMap(googleMap);
          }
        });
      }
    }
  }

  // The Map is verified. It is now safe to manipulate the map.
  protected void loadMap(GoogleMap googleMap) {
    map = googleMap;
    if (googleMap != null) {
      // Map is ready
      //Toast.makeText(this, "Map Fragment was loaded properly!", Toast.LENGTH_SHORT).show();

      MarkerMapActivityPermissionsDispatcher.getMyLocationWithPermissionCheck(this);
      MarkerMapActivityPermissionsDispatcher.startLocationUpdatesWithPermissionCheck(this);

      // Attach marker click listener to map here
      map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(com.google.android.gms.maps.model.Marker marker) {
          // Handle marker click here
          Marker clicked = (Marker) marker.getTag();
          return true;
        }
      });


      if (getCallingActivity() != null) {
        if (getCallingActivity().getClassName().equals(getString(R.string.main_activity))) {
          addAllMarkersToMap();
        } else if (getCallingActivity().getClassName().equals(getString(R.string.add_marker_activity))) {
          btnDoneSettingLoc.setVisibility(View.VISIBLE);

          map.setOnMapClickListener(placedLatLng -> {
            map.clear();
            placedMarker = map.addMarker(new MarkerOptions()
                .position(placedLatLng));

            map.setOnMarkerDragListener(this);

            placedMarker.setDraggable(true);
          });

          map.setOnMyLocationButtonClickListener(() -> {
            if (placedMarker != null) {
              placedMarker.setPosition(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
            }
            return false;
          });
        }
      }
    } else {
      //Toast.makeText(this, "Error - Map was null!!", Toast.LENGTH_SHORT).show();
    }
  }

  private void addAllMarkersToMap() {
    firebaseFirestore
        .collectionGroup(Marker.KEY_UPLOADED_MARKERS)
        .whereNotEqualTo(Marker.KEY_LOCATION, null)
        .get()
        .addOnSuccessListener(queryDocumentSnapshots -> {
          for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
            Marker retrievedMarker = documentSnapshot.toObject(Marker.class);

            String title = retrievedMarker.getTitle();
            String description = retrievedMarker.getDescription();

            Double lat = (Double) retrievedMarker.getLocation().get(Marker.KEY_LATITUDE);
            Double lng = (Double) retrievedMarker.getLocation().get(Marker.KEY_LONGITUDE);

            // listingPosition is a LatLng point
            LatLng listingPosition = new LatLng(lat, lng);

            // Create the marker on the fragment
            com.google.android.gms.maps.model.Marker mapMarker = map.addMarker(new MarkerOptions()
                .position(listingPosition)
                .title(title)
                .snippet(description));
            mapMarker.setTag(retrievedMarker);
            Log.i(TAG, "Successfully added marker to map");
          }
        })
        .addOnFailureListener(e -> Log.e(TAG, "Could not retrieve all markers", e));

  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    MarkerMapActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
  }

  @SuppressWarnings({"MissingPermission"})
  @NeedsPermission({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
  void getMyLocation() {
    map.setMyLocationEnabled(true);
    map.getUiSettings().setMyLocationButtonEnabled(true);
    map.setIndoorEnabled(true);

    FusedLocationProviderClient locationClient = getFusedLocationProviderClient(this);
    locationClient.getLastLocation()
        .addOnSuccessListener(location -> {
          if (location != null) {
            onLocationChanged(location);
            displayLocation();
          }
        })
        .addOnFailureListener(e -> Log.e("MapDemoActivity", "Error trying to get last GPS location", e));
  }

  @NeedsPermission({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
  protected void startLocationUpdates() {
    mLocationRequest = new LocationRequest();
    mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    mLocationRequest.setInterval(UPDATE_INTERVAL);
    mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
    builder.addLocationRequest(mLocationRequest);
    LocationSettingsRequest locationSettingsRequest = builder.build();

    SettingsClient settingsClient = LocationServices.getSettingsClient(this);
    settingsClient.checkLocationSettings(locationSettingsRequest);
    //noinspection MissingPermission
    getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
          @Override
          public void onLocationResult(LocationResult locationResult) {
            onLocationChanged(locationResult.getLastLocation());
          }
        },
        Looper.myLooper());
  }

  public void onLocationChanged(Location location) {
    // GPS may be turned off
    if (location == null) {
      return;
    }

    mCurrentLocation = location;
    String msg = "Updated Location: " +
        Double.toString(location.getLatitude()) + "," +
        Double.toString(location.getLongitude());
    //Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    //displayLocation();
  }

  private void displayLocation() {
    if (mCurrentLocation != null) {
      //Toast.makeText(this, "GPS location was found!", Toast.LENGTH_SHORT).show();
      LatLng latLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
      CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
      map.animateCamera(cameraUpdate);
    } else {
      //Toast.makeText(this, "Current location was null, enable GPS on emulator!", Toast.LENGTH_SHORT).show();
    }
  }

  public void onSaveInstanceState(Bundle savedInstanceState) {
    savedInstanceState.putParcelable(KEY_LOC, mCurrentLocation);
    super.onSaveInstanceState(savedInstanceState);
  }

  @Override
  protected void onResume() {
    super.onResume();

    displayLocation();

    MarkerMapActivityPermissionsDispatcher.startLocationUpdatesWithPermissionCheck(this);
  }

  /*
   * Called when the Activity becomes visible.
   */
  @Override
  protected void onStart() {
    super.onStart();
  }

  /*
   * Called when the Activity is no longer visible.
   */
  @Override
  protected void onStop() {
    super.onStop();
  }

  @Override
  public void onMarkerDragStart(com.google.android.gms.maps.model.Marker marker) {

  }

  @Override
  public void onMarkerDrag(com.google.android.gms.maps.model.Marker marker) {

  }

  @Override
  public void onMarkerDragEnd(com.google.android.gms.maps.model.Marker marker) {
    placedMarker.setPosition(marker.getPosition());
  }
}