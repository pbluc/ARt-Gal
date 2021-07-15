package com.fbu.pbluc.artgal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.fbu.pbluc.artgal.models.Marker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class AddMarkerActivity extends AppCompatActivity {

    private static final String TAG = "AddMarkerActivity";

    private static final int REFERENCE_IMG_REQUEST_CODE = 1; // onActivityResult request
    private static final int AUGMENTED_OBJ_REQUEST_CODE = 2;
    private int UPLOAD_STORAGE_STATUS = 0;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private FirebaseStorage firebaseStorage;
    private FirebaseFirestore firebaseFirestore;
    private StorageReference storageReference;
    private StorageReference referenceImagesReference;
    private StorageReference augmentedObjReference;

    private EditText etTitle;
    private EditText etDescription;
    private Button btnFindReferenceImg;
    private Button btnFindAugmentedObject;
    private Button btnSubmit;
    private ImageView ivReferenceImage;
    private TextView tvSelectedAugmentedObject;

    private Uri referenceImgUri;
    private Uri augmentedObjUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_marker);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        firebaseStorage = FirebaseStorage.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        // Create storage references from our app
        storageReference = firebaseStorage.getReference();

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        btnFindReferenceImg = findViewById(R.id.btnFindReferenceImg);
        btnFindAugmentedObject = findViewById(R.id.btnFindAugmentedObject);
        btnSubmit = findViewById(R.id.btnSubmit);
        ivReferenceImage = findViewById(R.id.ivReferenceImage);
        tvSelectedAugmentedObject = findViewById(R.id.tvSelectedAugmentedObject);

        btnFindReferenceImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser(v);
            }
        });

        btnFindAugmentedObject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser(v);
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addMarkerDocument();
            }
        });

    }

    private void addMarkerDocument() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        DocumentReference currentUserDoc = firebaseFirestore.collection("users").document(currentUser.getUid());
        Map<String, Object> markerImg = new HashMap<>();
        markerImg.put("uri", referenceImgUri.toString());
        markerImg.put("fileName", currentUser.getUid() + getFileName(referenceImgUri));
        Map<String, Object> augmentedObj = new HashMap<>();
        augmentedObj.put("uri", augmentedObjUri.toString());
        augmentedObj.put("fileName", currentUser.getUid() + getFileName(augmentedObjUri));

        if (!title.isEmpty() && !description.isEmpty() && currentUserDoc != null && referenceImgUri != null && augmentedObjUri != null) {
            Marker marker = new Marker();
            marker.setTitle(title);
            marker.setDescription(description);
            marker.setUser(currentUserDoc);
            marker.setCreatedAt(FieldValue.serverTimestamp());
            marker.setUpdatedAt(FieldValue.serverTimestamp());
            marker.setMarkerImg(markerImg);
            marker.setAugmentedObj(augmentedObj);

            currentUserDoc.
                    collection("uploadedMarkers")
                    .add(marker)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.i(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());

                            if (uploadFilesToStorage(referenceImgUri, augmentedObjUri, documentReference.getId()) != 0) {
                                // Files were not added to storage and must delete created marker document
                                currentUserDoc.collection("uploadedMarkers").document(documentReference.getId())
                                        .delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                Log.i(TAG, "DocumentSnapshot successfully deleted!");
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.e(TAG, "Error deleting document", e);
                                            }
                                        });
                            } else {
                                // Files were added to storage and update field updatedAt for user doc
                                //  and rename file names of marker and augmented object in marker document
                                //  as well as its updatedAt field.
                                Object newUpdateDateTime = FieldValue.serverTimestamp();
                                currentUserDoc
                                        .update("updatedAt", newUpdateDateTime)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                String updatedReferenceImgFileName = currentUser.getUid() + "_" + documentReference.getId() + getFileName(referenceImgUri);
                                                String updatedAugmentedObjectFileName = currentUser.getUid() + "_" + documentReference.getId() + getFileName(augmentedObjUri);
                                                currentUserDoc.collection("uploadedMarkers").document(documentReference.getId())
                                                        .update(
                                                                "augmentedObj.fileName", updatedAugmentedObjectFileName,
                                                                "markerImg.fileName", updatedReferenceImgFileName,
                                                                "updatedAt", newUpdateDateTime
                                                        )
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void unused) {
                                                                Map<String, Object> updatedMarkerImg = marker.getMarkerImg();
                                                                updatedMarkerImg.put("fileName", updatedReferenceImgFileName);
                                                                marker.setMarkerImg(updatedMarkerImg);
                                                                Map<String, Object> updatedAugmentedObj = marker.getAugmentedObj();
                                                                updatedAugmentedObj.put("fileName", updatedAugmentedObjectFileName);
                                                                marker.setAugmentedObj(updatedAugmentedObj);
                                                                Log.i(TAG, "Successfully updated both user and marker documents!");

                                                                Toast.makeText(AddMarkerActivity.this, "Marker successfully uploaded!", Toast.LENGTH_LONG).show();

                                                                Intent intent = new Intent(AddMarkerActivity.this, UploadedMarkersActivity.class);
                                                                startActivity(intent);
                                                                finish();
                                                            }
                                                        })
                                                        .addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Log.e(TAG, "Error updating marker document", e);
                                                            }
                                                        });
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.e(TAG, "Error updating user document", e);
                                            }
                                        });
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Error adding document", e);
                            return;
                        }
                    });
        } else {
            Toast.makeText(this, "One more fields empty!", Toast.LENGTH_LONG).show();
            return;
        }

    }

    private int uploadFilesToStorage(Uri referenceUri, Uri augmentedUri, String id) {
        // Create child references
        referenceImagesReference = storageReference.child("referenceImages/" + currentUser.getUid() + "_" + id + getFileName(referenceImgUri));
        augmentedObjReference = storageReference.child("augmentedObjects/" + currentUser.getUid() + "_" + id + getFileName(augmentedObjUri));

        UploadTask uploadReferenceImgTask = (UploadTask) referenceImagesReference.putFile(referenceUri)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle unsuccessful uploads
                        Log.e(TAG, "Unable to upload reference image to storage", e);
                        UPLOAD_STORAGE_STATUS = -1;
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        UploadTask uploadAugmentedObjTask = (UploadTask) augmentedObjReference.putFile(augmentedUri)
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e(TAG, "Unable to upload augmented object to storage", e);
                                        UPLOAD_STORAGE_STATUS = -1;
                                    }
                                });
                    }
                });
        etTitle.setText("");
        etDescription.setText("");
        ivReferenceImage.setImageResource(0);
        tvSelectedAugmentedObject.setText("Only .fbx, .obj, .gltf, .glb assets");

        return UPLOAD_STORAGE_STATUS;
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

    private void openFileChooser(View v) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        switch (v.getId()) {
            case R.id.btnFindReferenceImg:
                String[] referenceImgMimeTypes = {"image/jpeg", "image/png"};
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, referenceImgMimeTypes);
                startActivityForResult(intent, REFERENCE_IMG_REQUEST_CODE);
                break;
            case R.id.btnFindAugmentedObject:
                intent.setType("application/octet-stream");
                startActivityForResult(intent, AUGMENTED_OBJ_REQUEST_CODE);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REFERENCE_IMG_REQUEST_CODE:
                // If the file selection was successful
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        // Get the URI of the selected file
                        referenceImgUri = data.getData();
                        Log.i(TAG, "Uri: " + referenceImgUri.toString());
                        try {
                            // Get the file path from the URI
                            final String path = referenceImgUri.getPath();
                            Toast.makeText(this, "File selected: " + path, Toast.LENGTH_SHORT).show();

                            // Set selected image to imageView
                            Glide.with(this).load(referenceImgUri).into(ivReferenceImage);

                        } catch (Exception e) {
                            Log.e(TAG, "File select error", e);
                        }
                    } else {
                        return;
                    }
                }
                break;
            case AUGMENTED_OBJ_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        // Get the URI of the selected file
                        augmentedObjUri = data.getData();
                        Log.i(TAG, "Uri: " + augmentedObjUri.toString());
                        try {
                            // Set selected augmented object file name to TextView
                            tvSelectedAugmentedObject.setText("Selected: " + getFileName(augmentedObjUri));

                        } catch (Exception e) {
                            Log.e(TAG, "File select error", e);
                        }
                    } else {
                        return;
                    }
                }
                break;
            default:
                break;
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri != null && uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }

        if (uri != null && result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void goLoginActivity() {
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
        finish();
    }

    private void goMainActivity() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }
}