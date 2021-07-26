package com.fbu.pbluc.artgal;

import androidx.appcompat.app.AppCompatActivity;

import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

public class MarkerMapActivity extends AppCompatActivity {

  private SupportMapFragment mapFragment;
  private GoogleMap map;
  private LocationRequest mLocationRequest;
  Location mCurrentLocation;
  private long UPDATE_INTERVAL = 60000;  /* 60 secs */
  private long FASTEST_INTERVAL = 5000; /* 5 secs */

  private final static String KEY_LOCATION = "location";

  /*
   * Define a request code to send to Google Play services This code is
   * returned in Activity.onActivityResult
   */
  private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_marker_map);

    if (savedInstanceState != null && savedInstanceState.keySet().contains(KEY_LOCATION)) {
      // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation
      // is not null.
      mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
    }

    setUpMapIfNeeded();
  }

  protected void setUpMapIfNeeded() {
    // Do a null check to confirm that we have not already instantiated the map
    if(mapFragment == null) {
      mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment));
      // Check if we were successful in obtaining the map
      if(mapFragment != null) {
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
    if(googleMap != null) {
      // Map is ready
      Toast.makeText(this, "Map Fragment was loaded properly!", Toast.LENGTH_SHORT).show();

      //MapDemoActivityPermissionsDispatcher.getMyLocationWithPermissionCheck(this);
      //MapDemoActivityPermissionsDispatcher.startLocationUpdatesWithPermissionCheck(this);
    } else {
      Toast.makeText(this, "Error - Map was null!!", Toast.LENGTH_SHORT).show();
    }
  }
}