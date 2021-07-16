package com.fbu.pbluc.artgal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.fbu.pbluc.artgal.models.Marker;
import com.fbu.pbluc.artgal.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.parceler.Parcels;
import org.w3c.dom.Text;

public class MarkerDetailsActivity extends AppCompatActivity {
    private final static String TAG = "MarkerDetailsActivity";

    private FirebaseAuth firebaseAuth;
    private FirebaseStorage firebaseStorage;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseUser currentUser;

    private DocumentReference markerRef;

    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvAugmentedObjectFileName;
    private TextView tvCreatedAt;
    private TextView tvUserFullName;
    private TextView tvUsername;
    private ImageView ivReferenceImageMedia;
    private LinearLayout deleteMarkerLayoutContainer;

    private Marker marker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marker_details);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvAugmentedObjectFileName = findViewById(R.id.tvAugmentedObjectFileName);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvUserFullName = findViewById(R.id.tvUserFullName);
        tvUsername = findViewById(R.id.tvUsername);
        ivReferenceImageMedia = findViewById(R.id.ivReferenceImageMedia);
        deleteMarkerLayoutContainer = findViewById(R.id.deleteMarkerLayoutContainer);

        String userUid = getIntent().getStringExtra("userMarkerUid");
        String markerUid = getIntent().getStringExtra("clickedMarkerUid");


        markerRef = firebaseFirestore
                .collection("users")
                .document(userUid)
                .collection("uploadedMarkers")
                .document(markerUid);

        markerRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                marker = documentSnapshot.toObject(Marker.class);

                DocumentReference markerUser = marker.getUser();

                tvTitle.setText(marker.getTitle());
                tvDescription.setText(marker.getDescription());
                tvAugmentedObjectFileName.setText(marker.getAugmentedObj().get("fileName").toString().substring(49));
                tvCreatedAt.setText(marker.formattedCreatedAt());

                markerUser.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        User user = documentSnapshot.toObject(User.class);
                        tvUserFullName.setText(user.getName().get("fName").toString() + " " + user.getName().get("lName").toString());
                        tvUsername.setText(user.getUsername());
                    }
                });

                StorageReference storageReference = firebaseStorage.getReference();
                StorageReference markerImgReference = storageReference.child("referenceImages/" + marker.getMarkerImg().get("fileName").toString());

                // Check if the augmented object file exists in Firebase Storage
                StorageReference augmentedObjReference = storageReference.child("augmentedObjects/" + marker.getAugmentedObj().get("fileName").toString());
                augmentedObjReference
                        .getDownloadUrl()
                        .addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Log.i(TAG,"Augmented object file found!");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // File not found
                                Log.e(TAG, "Augmented object file not found", e);
                            }
                        });

                markerImgReference
                        .getDownloadUrl()
                        .addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Glide.with(MarkerDetailsActivity.this).load(uri).into(ivReferenceImageMedia);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "Could not get download url of marker img", e);
                            }
                        });
            }
        });

        if (userUid.equals(currentUser.getUid())) {
            deleteMarkerLayoutContainer.setVisibility(View.VISIBLE);
            deleteMarkerLayoutContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteMarkerFilesFromStorage();
                }
            });
        } else {
            deleteMarkerLayoutContainer.setVisibility(View.GONE);
        }
    }

    private void deleteMarkerFilesFromStorage() {
        // Create a storage reference from our app
        StorageReference storageReference = firebaseStorage.getReference();
        // Create a reference to the file to delete
        StorageReference referenceImg = storageReference.child("referenceImages/" + marker.getMarkerImg().get("fileName").toString());
        // Delete the file
        referenceImg
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // File deleted successfully
                        Log.i(TAG, "Reference image file was successfully deleted");
                        StorageReference augmentedObj = storageReference.child("augmentedObjects/" + marker.getAugmentedObj().get("fileName").toString());
                        augmentedObj
                                .delete()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.i(TAG, "Both files were successfully deleted");
                                        deleteMarkerDocument();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e(TAG, "Augmented object file was not deleted!", e);
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Uh-oh, an error occurred!
                        Log.e(TAG, "Reference image file was not deleted!", e);
                    }
                });
    }

    private void deleteMarkerDocument() {
        markerRef
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.i(TAG, "Marker document successfully deleted!");
                        goUploadedMarkersActivity();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error deleting marker document", e);
                    }
                });
    }

    private void goUploadedMarkersActivity() {
        Intent i = new Intent(MarkerDetailsActivity.this, UploadedMarkersActivity.class);
        startActivity(i);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            goLoginActivity();
        }
    }

    private void goLoginActivity() {
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
        finish();
    }

}