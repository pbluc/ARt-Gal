package com.fbu.pbluc.artgal;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";

    private FirebaseAuth firebaseAuth;

    private Button btnLogout;
    private Button btnUploadMarker;
    private Button btnViewMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();

        btnLogout = findViewById(R.id.btnLogout);
        btnUploadMarker = findViewById(R.id.btnUploadMarker);
        btnViewMarker = findViewById(R.id.btnViewMarkers);


        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });

        btnUploadMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goUploadActivity();
            }
        });

        btnViewMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goMarkersActivity();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if(currentUser == null) {
            goLoginActivity();
        }
    }

    private void goMarkersActivity() {
        Intent i = new Intent(this, UploadedMarkersActivity.class);
        startActivity(i);
    }

    private void goUploadActivity() {
        Intent i = new Intent(this, AddMarkerActivity.class);
        startActivity(i);
    }

    private void goLoginActivity() {
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
        finish();
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        goLoginActivity();
    }
}