package com.fbu.pbluc.artgal;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

  private final static String TAG = "LoginActivity"; // TODO: Make into a string xml value

  private FirebaseAuth firebaseAuth;

  private EditText etEmail;
  private EditText etPassword;
  private Button btnLogin;
  private TextView tvCreateAccount;
  private ProgressBar progressBarLoading;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);

    // Initialize Firebase Auth
    firebaseAuth = FirebaseAuth.getInstance();

    etEmail = findViewById(R.id.etEmail);
    etPassword = findViewById(R.id.etPassword);
    btnLogin = findViewById(R.id.btnLogin);
    tvCreateAccount = findViewById(R.id.tvCreateAccount);
    progressBarLoading = findViewById(R.id.pbLoading);

    btnLogin.setOnClickListener(v -> {
      progressBarLoading.setVisibility(ProgressBar.VISIBLE);

      String email = etEmail.getText().toString().trim();
      String password = etPassword.getText().toString().trim();

      firebaseAuth.signInWithEmailAndPassword(email, password)
          .addOnCompleteListener(LoginActivity.this, task -> {
            if (task.isSuccessful()) {
              // Log in success, update UI with the signed-in user's information
              Toast.makeText(LoginActivity.this, "Successfully logged in!", Toast.LENGTH_SHORT).show();
              goToMainActivity();
            } else {
              // If sign in fails, display a message to the user.
              Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
            }
            progressBarLoading.setVisibility(ProgressBar.INVISIBLE);

            etEmail.setText("");
            etPassword.setText("");
          });

    });

    tvCreateAccount.setOnClickListener(v -> goToCreateAccountActivity());
  }

  @Override
  protected void onStart() {
    super.onStart();
    // Check if user is signed in (non-null) and update UI accordingly.
    FirebaseUser currentUser = firebaseAuth.getCurrentUser();
    if (currentUser != null) {
      goToMainActivity();
    }
  }

  private void goToMainActivity() {
    Intent i = new Intent(this, MainActivity.class);
    startActivity(i);
    finish();
  }

  private void goToCreateAccountActivity() {
    Intent i = new Intent(this, CreateAccountActivity.class);
    startActivity(i);
    finish();
  }
}