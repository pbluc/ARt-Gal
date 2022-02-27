package com.fbu.pbluc.artgal;

import androidx.annotation.NonNull;
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
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fbu.pbluc.artgal.fragments.AddMarkerFragment;
import com.fbu.pbluc.artgal.fragments.FeedFragment;
import com.fbu.pbluc.artgal.fragments.UploadedMarkersFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.ar.core.ArCoreApk;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

  private final static String TAG = "MainActivity"; // TODO: Make into a string xml value
  private static final int VIEW_ALL_MARKERS = 0;

  private FirebaseAuth firebaseAuth;

  final FragmentManager fragmentManager = getSupportFragmentManager();
  private BottomNavigationView bottomNavigationView;

  private ImageView ivLogout;
  private TextView arCoreUserPrivacyDisclosure;

  @Override
  @SuppressWarnings("deprecation")
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    firebaseAuth = FirebaseAuth.getInstance();

    bottomNavigationView = findViewById(R.id.bottomNavigation);

    ivLogout = findViewById(R.id.ivLogout);
    arCoreUserPrivacyDisclosure = findViewById(R.id.ArCoreUserPrivacyDisclosure);


    // Depending on the build version on the device, returns displayable styled text from the provided HTML string
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      arCoreUserPrivacyDisclosure.setText(Html.fromHtml(getString(R.string.ar_core_user_privacy_disclosure), Html.FROM_HTML_MODE_LEGACY));
    } else {
      arCoreUserPrivacyDisclosure.setText(Html.fromHtml(getString(R.string.ar_core_user_privacy_disclosure)));
    }
    // All links within the HTML strings of the privacy disclosure text view are hyperlinked
    Linkify.addLinks(arCoreUserPrivacyDisclosure, Linkify.ALL);
    arCoreUserPrivacyDisclosure.setMovementMethod(LinkMovementMethod.getInstance());

    // Enable AR-related functionality on ARCore supported devices only.
    maybeEnableArButton();

    ivLogout.setOnClickListener(v -> logoutUser());

    bottomNavigationView.setOnItemSelectedListener(item -> {
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
          fragment = new FeedFragment();
          break;
      }

      // TODO: Walk through logic here
      // Checks if the user is trying to edit an existing marker then opens up the Add Marker
      // Fragment and pushes the arguments value from the intent to the fragment
      Bundle extras = getIntent().getExtras();
      if (extras != null &&
          extras.containsKey(getString(R.string.user_uid_editing_marker)) &&
          extras.containsKey(getString(R.string.marker_uid_editing_marker))) {
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
    // Checking whether the device supports AR Core
    ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(MainActivity.this);
    if (availability.isTransient()) {
      // Continue to query availability at 5Hz while compatibility is checked in the background.
      new Handler().postDelayed(() -> maybeEnableArButton(), 200);
    }
    // If device doesn't support AR Core then the bottom nagivation view will not display AR view option
    if (availability.isSupported()) {
      bottomNavigationView.getMenu().getItem(2).setVisible(true);
    } else { // The device is unsupported or unknown.
      bottomNavigationView.getMenu().getItem(2).setVisible(false);
    }
  }

  @Override
  protected void onStart() {
    // Check if user is signed in (non-null) and update UI accordingly.
    FirebaseUser currentUser = firebaseAuth.getCurrentUser();
    if (currentUser == null) {
      goToLoginActivity();
    } else {
      super.onStart();
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
    // TODO: Walk through logic here
    startActivityFromChild(this, i, VIEW_ALL_MARKERS, null);
  }

  private void logoutUser() {
    FirebaseAuth.getInstance().signOut();
    goToLoginActivity();
  }
}