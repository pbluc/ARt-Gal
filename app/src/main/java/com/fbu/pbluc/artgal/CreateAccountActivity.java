package com.fbu.pbluc.artgal;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fbu.pbluc.artgal.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateAccountActivity extends AppCompatActivity {

  private final static String TAG = "CreateAccountActivity"; // TODO: Make into a string xml value

  private FirebaseAuth firebaseAuth;
  private FirebaseFirestore firebaseFirestore;

  private EditText etEmail;
  private EditText etFirstName;
  private EditText etLastName;
  private EditText etUsername;
  private EditText etPassword;
  private TextView tvLogin;
  private Button btnCreateAccount;
  private ProgressBar progressBarLoading;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_create_account);

    firebaseAuth = FirebaseAuth.getInstance();

    // Initialize an instance of Cloud Firestore
    firebaseFirestore = FirebaseFirestore.getInstance();

    etEmail = findViewById(R.id.etEmail);
    etFirstName = findViewById(R.id.etFirstName);
    etLastName = findViewById(R.id.etLastName);
    etUsername = findViewById(R.id.etUsername);
    etPassword = findViewById(R.id.etPassword);
    tvLogin = findViewById(R.id.tvLogin);
    btnCreateAccount = findViewById(R.id.btnCreateAccount);
    progressBarLoading = findViewById(R.id.pbLoading);

    tvLogin.setOnClickListener(v -> goToLoginActivity());

    btnCreateAccount.setOnClickListener(v -> {
      progressBarLoading.setVisibility(ProgressBar.VISIBLE);

      String email = etEmail.getText().toString().trim();
      String fName = etFirstName.getText().toString().trim();
      String lName = etLastName.getText().toString().trim();
      String username = etUsername.getText().toString().trim();
      String password = etPassword.getText().toString().trim();

      firebaseAuth.createUserWithEmailAndPassword(email, password)
          .addOnCompleteListener(CreateAccountActivity.this, task -> {
            if (task.isSuccessful()) {
              // Sign up success, update UI with the signed in user's information
              Toast.makeText(CreateAccountActivity.this, "Successfully created account!", Toast.LENGTH_SHORT).show();

              FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

              // Create user document
              Map<String, Object> name = new HashMap<>();
              name.put(User.KEY_FIRST_NAME, fName);
              name.put(User.KEY_LAST_NAME, lName);

              User user = new User();
              user.setEmail(email);
              user.setName(name);
              user.setUsername(username);
              user.setPassword(password);
              user.setCreatedAt(FieldValue.serverTimestamp());
              user.setUpdatedAt(FieldValue.serverTimestamp());

              DocumentReference currentUserDoc = firebaseFirestore.collection(User.KEY_USERS).document(firebaseUser.getUid());
              currentUserDoc.set(user);

              goToMainActivity();
            } else {
              // If sign up fails, display a message to the user
              Toast.makeText(CreateAccountActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
            }
            progressBarLoading.setVisibility(ProgressBar.INVISIBLE);

            etEmail.setText("");
            etFirstName.setText("");
            etLastName.setText("");
            etUsername.setText("");
            etPassword.setText("");
          });
    });

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

  private void goToLoginActivity() {
    Intent i = new Intent(this, LoginActivity.class);
    startActivity(i);
    finish();
  }

  private void goToMainActivity() {
    Intent i = new Intent(this, MainActivity.class);
    startActivity(i);
    finish();
  }
}