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

import com.fbu.pbluc.artgal.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateAccountActivity extends AppCompatActivity {

    private final static String TAG = "CreateAccountActivity";

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    private EditText etEmail;
    private EditText etPassword;
    private EditText etFirstName;
    private EditText etLastName;
    private EditText etUsername;
    private Button btnLogin;
    private Button btnCreateAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize an instance of Cloud Firestore
        firebaseFirestore = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etUsername = findViewById(R.id.etUsername);
        btnLogin = findViewById(R.id.btnLogin);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goLoginActivity();
            }
        });

        btnCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                String fName = etFirstName.getText().toString().trim();
                String lName = etLastName.getText().toString().trim();
                String username = etUsername.getText().toString().trim();

                firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(CreateAccountActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(task.isSuccessful()) {
                                    // Sign up success, update UI with the signed in user's information
                                    Log.i(TAG, "createdUserWithEmail:success");
                                    Toast.makeText(CreateAccountActivity.this, "Successfully created account!", Toast.LENGTH_SHORT).show();

                                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                                    // Create user document
                                    Map<String, Object> name = new HashMap<>();
                                    name.put("fName", fName);
                                    name.put("lName", lName);

                                    User user = new User(email, name, username, password, FieldValue.serverTimestamp(), FieldValue.serverTimestamp());

                                    DocumentReference currentUserDoc = firebaseFirestore.collection("users").document(firebaseUser.getUid());
                                    currentUserDoc.set(user);
                                    user.setUid(firebaseUser.getUid());

                                    goMainActivity();
                                } else {
                                    // If sign up fails, display a message to the user
                                    Log.e(TAG, "createdUserWithEmail:failure", task.getException());
                                    Toast.makeText(CreateAccountActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                                }
                                etEmail.setText("");
                                etPassword.setText("");
                                etFirstName.setText("");
                                etLastName.setText("");
                                etUsername.setText("");
                            }
                        });
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if(currentUser != null) {
            goMainActivity();
        }
    }

    private void goLoginActivity() {
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
        finish();
    }

    private void goMainActivity() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }
}