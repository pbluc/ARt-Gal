package com.fbu.pbluc.artgal;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fbu.pbluc.artgal.fragments.AddMarkerFragment;
import com.fbu.pbluc.artgal.fragments.FeedFragment;
import com.fbu.pbluc.artgal.fragments.UploadedMarkersFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.ar.core.ArCoreApk;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

  private final static String TAG = "MainActivity";
  private static final int VIEW_ALL_MARKERS = 0;

  private FirebaseAuth firebaseAuth;

  final FragmentManager fragmentManager = getSupportFragmentManager();
  private BottomNavigationView bottomNavigationView;

  private ImageView ivLogout;
  private TextView arCoreUserPrivacyDisclosure;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    firebaseAuth = FirebaseAuth.getInstance();

    bottomNavigationView = findViewById(R.id.bottomNavigation);

    ivLogout = findViewById(R.id.ivLogout);
    arCoreUserPrivacyDisclosure = findViewById(R.id.ArCoreUserPrivacyDisclosure);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      arCoreUserPrivacyDisclosure.setText(Html.fromHtml(getString(R.string.ar_core_user_privacy_disclosure), Html.FROM_HTML_MODE_COMPACT));
    } else {
      arCoreUserPrivacyDisclosure.setText(Html.fromHtml(getString(R.string.ar_core_user_privacy_disclosure)));
    }
    Linkify.addLinks(arCoreUserPrivacyDisclosure, Linkify.ALL);
    arCoreUserPrivacyDisclosure.setMovementMethod(LinkMovementMethod.getInstance());

    // Enable AR-related functionality on ARCore supported devices only.
    maybeEnableArButton();

    ivLogout.setOnClickListener(v -> logoutUser());

    bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
      Fragment fragment = null;
      switch (item.getItemId()) {
        case R.id.action_upload:
          fragment = new AddMarkerFragment();
          break;
        case R.id.action_feed:
          fragment = new FeedFragment();
          break;
        case R.id.action_ar:
          goToArViewActivity();
          break;
        case R.id.action_map:
          goToMarkerMapActivity();
          break;
        case R.id.action_markers:
          fragment = new UploadedMarkersFragment();
          break;
        default:
          fragment = new UploadedMarkersFragment();
          break;
      }

      Bundle extras = getIntent().getExtras();
      if (extras != null && extras.containsKey(getString(R.string.user_uid_editing_marker)) && extras.containsKey(getString(R.string.marker_uid_editing_marker))) {
        fragment = new AddMarkerFragment();
        fragment.setArguments(extras);
      }

      if (fragment != null) {
        fragmentManager.beginTransaction().replace(R.id.flContainer, fragment).commit();
      }
      return true;
    });
    // Set default selection
    bottomNavigationView.setSelectedItemId(R.id.action_feed);
  }

  void maybeEnableArButton() {
    ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(MainActivity.this);
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
      bottomNavigationView.getMenu().getItem(2).setVisible(true);
    } else { // The device is unsupported or unknown.
      bottomNavigationView.getMenu().getItem(2).setVisible(false);
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

  private void goToLoginActivity() {
    Intent i = new Intent(MainActivity.this, LoginActivity.class);
    startActivity(i);
    finish();
  }

  private void goToArViewActivity() {
    Intent i = new Intent(MainActivity.this, ArViewActivity.class);
    startActivity(i);
  }

  private void goToMarkerMapActivity() {
    Intent i = new Intent(MainActivity.this, MarkerMapActivity.class);
    startActivityFromChild(this, i, VIEW_ALL_MARKERS, null);
  }

  private void logoutUser() {
    FirebaseAuth.getInstance().signOut();
    goToLoginActivity();
  }
}