package com.fbu.pbluc.artgal;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

import com.google.ar.core.ArCoreApk;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

  private final static String TAG = "MainActivity";
  private static final int VIEW_ALL_MARKERS = 0;

  private FirebaseAuth firebaseAuth;

  private Button btnLogout;
  private Button btnUploadMarker;
  private Button btnViewMarker;
  private Button btnArView;
  private Button btnMarkerMap;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    firebaseAuth = FirebaseAuth.getInstance();

    btnLogout = findViewById(R.id.btnLogout);
    btnUploadMarker = findViewById(R.id.btnUploadMarker);
    btnViewMarker = findViewById(R.id.btnViewMarkers);
    btnArView = findViewById(R.id.btnArView);
    btnMarkerMap = findViewById(R.id.btnMarkerMap);

    // Enable AR-related functionality on ARCore supported devices only.
    maybeEnableArButton();

    btnLogout.setOnClickListener(v -> logoutUser());

    btnUploadMarker.setOnClickListener(v -> goToUploadActivity());

    btnViewMarker.setOnClickListener(v -> goToMarkersActivity());

    btnArView.setOnClickListener(v -> goToArViewActivity());

    btnMarkerMap.setOnClickListener(v -> {
      goToMarkerMapActivity();
    });

  }

  void maybeEnableArButton() {
    ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
    if (availability.isTransient()) {
      // Continue to query availability at 5Hz while compatibility is checked in the background.
      new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
          maybeEnableArButton();
        }
      }, 200);
    }
    if (availability.isSupported()) {
      btnArView.setVisibility(View.VISIBLE);
      btnArView.setEnabled(true);
    } else { // The device is unsupported or unknown.
      btnArView.setVisibility(View.GONE);
      btnArView.setEnabled(false);
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    // Check if user is signed in (non-null) and update UI accordingly.
    FirebaseUser currentUser = firebaseAuth.getCurrentUser();
    if (currentUser == null) {
      goToLoginActivity();
    }
  }

  private void goToMarkersActivity() {
    Intent i = new Intent(this, UploadedMarkersActivity.class);
    startActivity(i);
  }

  private void goToUploadActivity() {
    Intent i = new Intent(this, AddMarkerActivity.class);
    startActivity(i);
  }

  private void goToArViewActivity() {
    Intent i = new Intent(this, ArViewActivity.class);
    startActivity(i);
  }

  private void goToLoginActivity() {
    Intent i = new Intent(this, LoginActivity.class);
    startActivity(i);
    finish();
  }

  private void goToMarkerMapActivity() {
    Intent i = new Intent(this, MarkerMapActivity.class);
    startActivityFromChild(this, i, VIEW_ALL_MARKERS, null);
  }

  private void logoutUser() {
    FirebaseAuth.getInstance().signOut();
    goToLoginActivity();
  }
}