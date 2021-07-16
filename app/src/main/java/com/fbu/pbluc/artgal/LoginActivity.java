package com.fbu.pbluc.artgal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

  private final static String TAG = "LoginActivity";

  private FirebaseAuth firebaseAuth;

  private EditText etEmail;
  private EditText etPassword;
  private Button btnLogin;
  private Button btnCreateAccount;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);

    // Initialize Firebase Auth
    firebaseAuth = FirebaseAuth.getInstance();

    etEmail = findViewById(R.id.etEmail);
    etPassword = findViewById(R.id.etPassword);
    btnLogin = findViewById(R.id.btnLogin);
    btnCreateAccount = findViewById(R.id.btnCreateAccount);

    btnLogin.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
              @Override
              public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                  // Log in success, update UI with the signed-in user's information
                  Log.i(TAG, "signInWithEmail:success");
                  Toast.makeText(LoginActivity.this, "Successfully logged in!", Toast.LENGTH_SHORT).show();
                  goMainActivity();
                } else {
                  // If sign in fails, display a message to the user.
                  Log.i(TAG, "signInWthEmail:failure", task.getException());
                  Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                }
                etEmail.setText("");
                etPassword.setText("");
              }
            });

      }
    });

    btnCreateAccount.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        goCreateAccountActivity();
      }
    });
  }

  @Override
  protected void onStart() {
    super.onStart();
    // Check if user is signed in (non-null) and update UI accordingly.
    FirebaseUser currentUser = firebaseAuth.getCurrentUser();
    if (currentUser != null) {
      goMainActivity();
    }
  }

  private void goMainActivity() {
    Intent i = new Intent(this, MainActivity.class);
    startActivity(i);
    finish();
  }

  private void goCreateAccountActivity() {
    Intent i = new Intent(this, CreateAccountActivity.class);
    startActivity(i);
  }
}