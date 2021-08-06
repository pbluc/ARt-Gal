package com.fbu.pbluc.artgal;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.fbu.pbluc.artgal.adapters.CustomWindowAdapter;
import com.fbu.pbluc.artgal.fragments.AddMarkerFragment;
import com.fbu.pbluc.artgal.models.Marker;

import com.fbu.pbluc.artgal.trees.LatLngKDTree;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;



import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

@RuntimePermissions
public class MarkerMapActivity extends AppCompatActivity implements GoogleMap.OnMarkerDragListener {

  private static final String TAG = "MarkerMapActivity";

  private static final double EPSILON = 0.0000000001;
  private static final double MILES_TO_METERS_RATIO = 1609.34;

  private FirebaseFirestore firebaseFirestore;

  private ImageButton btnDoneSettingLoc;
  private Button btnChangeMarkerViewRadius;
  private TextView tvMarkersWithinRadius;
  private EditText etMarkersWithinRadius;

  private BitmapDescriptor customMarkerIcon;

  private SupportMapFragment mapFragment;
  private GoogleMap map;
  private LocationRequest mLocationRequest;
  Location mCurrentLocation;

  private List<LatLng> allMapMarkerLatLngs;
  private List<Marker> allMarkerDocuments;

  private long UPDATE_INTERVAL = 60000;  /* 60 secs */
  private long FASTEST_INTERVAL = 5000; /* 5 secs */
  private int CHANGE_RADIUS_BTN_STATE = 1;
  private double SHOW_MARKER_WITHIN_RADIUS = 0.2;

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
    btnChangeMarkerViewRadius = findViewById(R.id.btnChangeMarkerViewRadius);
    tvMarkersWithinRadius = findViewById(R.id.tvMarkersWithinRadius);
    etMarkersWithinRadius = findViewById(R.id.etMarkersWithinRadius);

    tvMarkersWithinRadius.setText(Double.toString(SHOW_MARKER_WITHIN_RADIUS) + getString(R.string.miles));

    firebaseFirestore = FirebaseFirestore.getInstance();

    // customMarkerIcon = BitmapDescriptorFactory.fromResource(R.drawable.canvas_jellyfish);

    if (savedInstanceState != null && savedInstanceState.keySet().contains(KEY_LOC)) {
      // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation
      // is not null.
      mCurrentLocation = savedInstanceState.getParcelable(KEY_LOC);
    }

    allMapMarkerLatLngs = new ArrayList<>();
    allMarkerDocuments = new ArrayList<>();

    btnDoneSettingLoc.setOnClickListener(v -> {
      Intent returnIntent = new Intent();
      if (placedMarker != null) {
        double[] latLng = {placedMarker.getPosition().latitude, placedMarker.getPosition().longitude};
        returnIntent.putExtra(getString(R.string.new_marker_latlng), latLng);
        setResult(Activity.RESULT_OK, returnIntent);
      } else {
        setResult(Activity.RESULT_CANCELED,returnIntent);
      }
      finish();
    });

    btnChangeMarkerViewRadius.setOnClickListener(v -> {
      if (CHANGE_RADIUS_BTN_STATE % 2 == 0) { // Button is being clicked to send new value of radius
        tvMarkersWithinRadius.setVisibility(View.VISIBLE);
        etMarkersWithinRadius.setVisibility(View.GONE);

        SHOW_MARKER_WITHIN_RADIUS = Double.parseDouble(etMarkersWithinRadius.getText().toString().trim());
        tvMarkersWithinRadius.setText(SHOW_MARKER_WITHIN_RADIUS + getString(R.string.miles));

        addNearestMarkersToMap();

        CHANGE_RADIUS_BTN_STATE--;
      } else { // Button is being clicked to enter a new radius
        tvMarkersWithinRadius.setVisibility(View.GONE);
        etMarkersWithinRadius.setVisibility(View.VISIBLE);

        etMarkersWithinRadius.setText(Double.toString(SHOW_MARKER_WITHIN_RADIUS));

        CHANGE_RADIUS_BTN_STATE++;
      }
    });

    setUpMapIfNeeded();
  }

  protected void setUpMapIfNeeded() {
    // Do a null check to confirm that we have not already instantiated the map
    if (mapFragment == null) {
      mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment));
      // Check if we were successful in obtaining the map
      if (mapFragment != null) {
        mapFragment.getMapAsync(googleMap -> loadMap(googleMap));
      }
    }
  }

  // The Map is verified. It is now safe to manipulate the map.
  protected void loadMap(GoogleMap googleMap) {
    map = googleMap;
    if (googleMap != null) {
      // Map is ready

      String callingActivity = getIntent().getStringExtra(getString(R.string.flag));
      double[] detailMarkerLatLng = getIntent().getDoubleArrayExtra(getString(R.string.marker_detail_lat_lng));

      if (callingActivity != null && callingActivity.equals(AddMarkerFragment.TAG)) {
        MarkerMapActivityPermissionsDispatcher.getMyLocationWithPermissionCheck(this);
        MarkerMapActivityPermissionsDispatcher.startLocationUpdatesWithPermissionCheck(this);

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
      } else if(detailMarkerLatLng != null) {

        map.addMarker(new MarkerOptions().position(new LatLng(detailMarkerLatLng[0], detailMarkerLatLng[1])));
        displayLocation(new LatLng(detailMarkerLatLng[0], detailMarkerLatLng[1]));
      } else {
        MarkerMapActivityPermissionsDispatcher.getMyLocationWithPermissionCheck(this);
        MarkerMapActivityPermissionsDispatcher.startLocationUpdatesWithPermissionCheck(this);

        //map.setInfoWindowAdapter(new CustomWindowAdapter(getLayoutInflater()));

        btnChangeMarkerViewRadius.setVisibility(View.VISIBLE);
        tvMarkersWithinRadius.setVisibility(View.VISIBLE);

        addAllMarkerDocs();

          /*
          // Attach marker click listener to map here
          map.setOnMarkerClickListener(marker -> {
            // Handle marker click here
            Marker clicked = (Marker) marker.getTag();
            return true;
          });*/
      }
    } else {
      //Toast.makeText(this, "Error - Map was null!!", Toast.LENGTH_SHORT).show();
    }
  }

  private void addAllMarkerDocs() {

    firebaseFirestore
        .collectionGroup(Marker.KEY_UPLOADED_MARKERS)
        .whereNotEqualTo(Marker.KEY_LOCATION, null)
        .get()
        .addOnSuccessListener(queryDocumentSnapshots -> {
          int expectedMarkerDocumentCount = queryDocumentSnapshots.size();
          for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
            Marker retrievedMarker = documentSnapshot.toObject(Marker.class);

            allMarkerDocuments.add(retrievedMarker);

            Double lat = (Double) retrievedMarker.getLocation().get(Marker.KEY_LATITUDE);
            Double lng = (Double) retrievedMarker.getLocation().get(Marker.KEY_LONGITUDE);

            // listingPosition is a LatLng point
            LatLng listingPosition = new LatLng(lat, lng);
            allMapMarkerLatLngs.add(listingPosition);

            if (allMarkerDocuments.size() == expectedMarkerDocumentCount && allMapMarkerLatLngs.size() == expectedMarkerDocumentCount) {
              addNearestMarkersToMap();
            }
          }
        })
        .addOnFailureListener(e -> Log.e(TAG, "Could not retrieve all markers", e));

  }

  private void addNearestMarkersToMap() {
    map.clear();

    LatLngKDTree latLngKDTree = new LatLngKDTree(allMapMarkerLatLngs);
    List<LatLng> nearestMarkers = latLngKDTree.findNearestMarkers(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), SHOW_MARKER_WITHIN_RADIUS);
    for (LatLng withinRadiusMarker :
        nearestMarkers) {
      int index = findIndexInAllLatLngs(withinRadiusMarker);

      if (index != -1) {
        String title = allMarkerDocuments.get(index).getTitle();
        String description = allMarkerDocuments.get(index).getDescription();

        com.google.android.gms.maps.model.Marker mapMarker = map.addMarker(new MarkerOptions()
            .position(withinRadiusMarker)
            .title(title)
            .snippet(description));
        mapMarker.setTag(allMarkerDocuments.get(index));
        Log.i(TAG, "Successfully added marker to map");
      }
    }

    // Instantiates a new CircleOptions object and defines the center and radius
    CircleOptions circleOptions = new CircleOptions()
        .center(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
        .radius(SHOW_MARKER_WITHIN_RADIUS * MILES_TO_METERS_RATIO); // In meters

    // Add the circle onto the map
    Circle circle = map.addCircle(circleOptions);
    map.animateCamera(CameraUpdateFactory.newLatLngZoom(circleOptions.getCenter(), getZoomLevel(circle)));
  }

  private int getZoomLevel(Circle circle) {
    int zoomLevel = 17;
    if (circle != null) {
      double radius = circle.getRadius() + circle.getRadius() / 2;
      double scale = radius / 500;

      zoomLevel = (int) (16 - Math.log(scale) / Math.log(2));
    }
    return zoomLevel;
  }

  private int findIndexInAllLatLngs(LatLng withinRadiusMarker) {
    for (int i = 0; i < allMapMarkerLatLngs.size(); i++) {
      if (isEqual(withinRadiusMarker.latitude, allMapMarkerLatLngs.get(i).latitude) &&
          isEqual(withinRadiusMarker.longitude, allMapMarkerLatLngs.get(i).longitude)) {
        return i;
      }
    }
    return -1;
  }

  private boolean isEqual(double d1, double d2) {
    return d1 == d2 || isRelativelyEqual(d1,d2);
  }

  private boolean isRelativelyEqual(double d1, double d2) {
    return EPSILON > Math.abs(d1 - d2) / Math.max(Math.abs(d1), Math.abs(d2));
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

  private void displayLocation(LatLng point) {
    LatLng latLng = new LatLng(point.latitude, point.longitude);
    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
    map.animateCamera(cameraUpdate);
  }

  public void onSaveInstanceState(Bundle savedInstanceState) {
    savedInstanceState.putParcelable(KEY_LOC, mCurrentLocation);
    super.onSaveInstanceState(savedInstanceState);
  }

  @Override
  protected void onResume() {
    super.onResume();

    MarkerMapActivityPermissionsDispatcher.startLocationUpdatesWithPermissionCheck(this);
  }

  /*
   * Called when the Activity becomes visible.
   */
  @Override
  protected void onStart() {
    super.onStart();
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    if (currentUser == null) {
      goToLoginActivity();
    }
  }

  private void goToLoginActivity() {
    Intent i = new Intent(this, LoginActivity.class);
    startActivity(i);
    finish();
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