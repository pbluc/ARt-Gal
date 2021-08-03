package com.fbu.pbluc.artgal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.camera2.params.SessionConfiguration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.fbu.pbluc.artgal.models.Marker;
import com.fbu.pbluc.artgal.models.User;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ArSceneView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
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
  private static final int NEW_MARKER_LOC_REQUEST_CODE = 3;

  private FirebaseAuth firebaseAuth;
  private FirebaseUser currentUser;
  private FirebaseStorage firebaseStorage;
  private FirebaseFirestore firebaseFirestore;
  private StorageReference storageReference;
  private StorageReference referenceImagesReference;
  private StorageReference augmentedObjReference;

  private DocumentReference editingMarkerRef;

  private EditText etTitle;
  private EditText etDescription;
  private Button btnFindReferenceImg;
  private Button btnFindAugmentedObject;
  private Button btnSubmit;
  private Button btnOpenMaps;
  private ImageView ivReferenceImage;
  private TextView tvSelectedAugmentedObject;
  private RadioButton rbAddLocation;

  private Uri referenceImgUri;
  private Uri augmentedObjUri;

  private LatLng markerLoc;

  private Marker editingMarker;

  private String userUidEditingMarker;
  private String markerUidEditingMarker;

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
    btnOpenMaps = findViewById(R.id.btnOpenMaps);
    ivReferenceImage = findViewById(R.id.ivReferenceImage);
    tvSelectedAugmentedObject = findViewById(R.id.tvSelectedAugmentedObject);
    rbAddLocation = findViewById(R.id.rbAddLocation);

    userUidEditingMarker = getIntent().getStringExtra(getString(R.string.user_uid_editing_marker));
    markerUidEditingMarker = getIntent().getStringExtra(getString(R.string.marker_uid_editing_marker));

    if (userUidEditingMarker != null && markerUidEditingMarker != null) {
      editingMarkerRef = firebaseFirestore
          .collection(User.KEY_USERS)
          .document(userUidEditingMarker)
          .collection(Marker.KEY_UPLOADED_MARKERS)
          .document(markerUidEditingMarker);

      editingMarkerRef
          .get()
          .addOnSuccessListener(documentSnapshot -> {
            editingMarker = documentSnapshot.toObject(Marker.class);

            etTitle.setText(editingMarker.getTitle());
            etDescription.setText(editingMarker.getDescription());
            Glide
                .with(AddMarkerActivity.this)
                .load(Uri.parse(editingMarker.getMarkerImg().get(Marker.KEY_URI).toString()))
                .into(ivReferenceImage);
            tvSelectedAugmentedObject.setText(editingMarker.getAugmentedObj().get(Marker.KEY_FILENAME).toString().substring(49));

            if (editingMarker.getLocation() != null && editingMarker.getLocation().size() != 0) {
              rbAddLocation.setChecked(true);
              rbAddLocation.setSelected(true);

              btnOpenMaps.setVisibility(View.VISIBLE);

              markerLoc = new LatLng((double) editingMarker.getLocation().get(Marker.KEY_LATITUDE), (double) editingMarker.getLocation().get(Marker.KEY_LONGITUDE));
            }
          })
          .addOnFailureListener(e -> Log.i(TAG, "Could not retrieved editing marker document", e));
    }

    btnFindReferenceImg.setOnClickListener(v -> openFileChooser(v));

    btnFindAugmentedObject.setOnClickListener(v -> openFileChooser(v));

    btnSubmit.setOnClickListener(v -> {
      if (getCallingActivity() != null && getCallingActivity().getClassName().equals(getString(R.string.marker_details_activity))) {
        updateCurrentMarkerDocument();
      } else {
        addMarkerDocument();
      }
    });

    rbAddLocation.setOnClickListener(v -> {
      if (!rbAddLocation.isSelected()) {
        rbAddLocation.setChecked(true);
        rbAddLocation.setSelected(true);
        // Reveal button to open map view
        btnOpenMaps.setVisibility(View.VISIBLE);
      } else {
        rbAddLocation.setChecked(false);
        rbAddLocation.setSelected(false);
        // Hide button to open map view
        btnOpenMaps.setVisibility(View.GONE);
      }
    });

    btnOpenMaps.setOnClickListener(v -> {
      // Go to map view
      goToMarkerMapActivity();
    });

  }

  private void updateCurrentMarkerDocument() {
    String title = etTitle.getText().toString().trim();
    String description = etDescription.getText().toString().trim();

    if (title.isEmpty() || description.isEmpty() || (markerLoc == null && rbAddLocation.isChecked() && rbAddLocation.isSelected())) {
      Toast.makeText(this, "One more fields empty!", Toast.LENGTH_LONG).show();
      return;
    }

    editingMarker.setTitle(title);
    editingMarker.setDescription(description);

    String fileNamePrefix = currentUser.getUid() + "_" + markerUidEditingMarker;

    String previousReferenceObjFileName = editingMarker.getMarkerImg().get(Marker.KEY_FILENAME).toString();
    if (referenceImgUri != null) {
      Map<String, Object> markerImg = new HashMap<>();
      markerImg.put(Marker.KEY_URI, "");
      markerImg.put(Marker.KEY_FILENAME, fileNamePrefix + getFileName(referenceImgUri));

      editingMarker.setMarkerImg(markerImg);
    }

    String previousAugmentedObjFileName = editingMarker.getAugmentedObj().get(Marker.KEY_FILENAME).toString();
    if (augmentedObjUri != null) {
      Map<String, Object> augmentedObj = new HashMap<>();
      augmentedObj.put(Marker.KEY_URI, augmentedObjUri.toString());
      augmentedObj.put(Marker.KEY_FILENAME, fileNamePrefix + getFileName(augmentedObjUri));

      editingMarker.setAugmentedObj(augmentedObj);
    }

    Map<String, Object> location = null;
    if (markerLoc != null && rbAddLocation.isChecked() && rbAddLocation.isSelected()) {
      location = new HashMap<>();
      location.put(Marker.KEY_LATITUDE, markerLoc.latitude);
      location.put(Marker.KEY_LONGITUDE, markerLoc.longitude);
    }
    editingMarker.setLocation(location);

    editingMarkerRef
        .set(editingMarker)
        .addOnCompleteListener(task -> {
          if (task.isSuccessful()) {
            if (augmentedObjUri != null && referenceImgUri != null) {
              // Both augmented object file and reference images need to be updated
              updateExistingFileInStorage(
                  augmentedObjUri,
                  previousAugmentedObjFileName,
                  editingMarker.getAugmentedObj().get(Marker.KEY_FILENAME).toString(),
                  getString(R.string.augmented_object_ref));

              updateExistingFileInStorage(
                  referenceImgUri,
                  previousReferenceObjFileName,
                  editingMarker.getMarkerImg().get(Marker.KEY_FILENAME).toString(),
                  getString(R.string.reference_images_ref));

            } else if (augmentedObjUri != null || referenceImgUri != null) {
              // Either the augmented object file or reference images need to be updated
              if (augmentedObjUri != null) {
                updateExistingFileInStorage(
                    augmentedObjUri,
                    previousAugmentedObjFileName,
                    editingMarker.getAugmentedObj().get(Marker.KEY_FILENAME).toString(),
                    getString(R.string.augmented_object_ref));
              } else {
                updateExistingFileInStorage(
                    referenceImgUri,
                    previousReferenceObjFileName,
                    editingMarker.getMarkerImg().get(Marker.KEY_FILENAME).toString(),
                    getString(R.string.reference_images_ref));
              }
            } else {
              // User did not change augmented object file nor reference image
              updateUpdatedAtField(editingMarker.getUser(), null);
            }
          } else {
            Log.e(TAG, "onFailure: Could not update existing marker document", task.getException());
          }
        });
  }

  private void updateUpdatedAtField(DocumentReference currentUserDocEditingMarker, String updatedReferenceImgFileName) {
    Object newUpdateDateTime = FieldValue.serverTimestamp();

    if (updatedReferenceImgFileName != null) { // Update fields updatedAt and markerImg.uri
      // Get the uri of updated reference image file from storage
      storageReference.child(getString(R.string.reference_images_ref) + updatedReferenceImgFileName)
          .getDownloadUrl()
          .addOnSuccessListener(uri -> {
            String updatedReferenceImageUriString = uri.toString();

            // Update in user document
            currentUserDocEditingMarker
                .update(Marker.KEY_UPDATED_AT, newUpdateDateTime)
                .addOnSuccessListener(unused -> {
                  // Update in marker document
                  currentUserDocEditingMarker
                      .collection(Marker.KEY_UPLOADED_MARKERS)
                      .document(markerUidEditingMarker)
                      .update(
                          Marker.KEY_UPDATED_AT, newUpdateDateTime,
                          Marker.KEY_MARKER_IMG + "." + Marker.KEY_URI, updatedReferenceImageUriString
                      )
                      .addOnSuccessListener(unused12 -> {
                        editingMarker.setUpdatedAt(newUpdateDateTime);

                        Map<String, Object> updatedMarkerImg = editingMarker.getMarkerImg();
                        updatedMarkerImg.put(Marker.KEY_FILENAME, updatedReferenceImgFileName);
                        updatedMarkerImg.put(Marker.KEY_URI, updatedReferenceImageUriString);
                        editingMarker.setMarkerImg(updatedMarkerImg);

                        goToMarkerDetailsActivity();
                      })
                      .addOnFailureListener(e -> Log.e(TAG, "onFailure: Could not update field updatedAt for marker document", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "onFailure: Could not update field updatedAt for user document", e));
          })
          .addOnFailureListener(e -> Log.e(TAG, "onFailure: Could not get uri of updated reference image", e));
    } else { // Update fields updatedAt
      // Update in user document
      currentUserDocEditingMarker
          .update(Marker.KEY_UPDATED_AT, newUpdateDateTime)
          .addOnSuccessListener(unused -> {
            // Update in marker document
            currentUserDocEditingMarker
                .collection(Marker.KEY_UPLOADED_MARKERS)
                .document(markerUidEditingMarker)
                .update(Marker.KEY_UPDATED_AT, newUpdateDateTime)
                .addOnSuccessListener(unused1 -> {
                  editingMarker.setUpdatedAt(newUpdateDateTime);

                  goToMarkerDetailsActivity();
                })
                .addOnFailureListener(e -> Log.e(TAG, "onFailure: Could not update field updatedAt for marker document", e));
          })
          .addOnFailureListener(e -> Log.e(TAG, "onFailure: Could not update field updatedAt for user document", e));
    }
  }

  private void goToMarkerDetailsActivity() {
    Intent returnIntent = new Intent();
    setResult(Activity.RESULT_OK, returnIntent);

    Toast.makeText(this, "Successfully updated existing marker!", Toast.LENGTH_SHORT).show();

    finish();
  }

  private void updateExistingFileInStorage(Uri updatedUri, String previousFileName, String updatedFileName, String storageReferenceFolder) {
    StorageReference updateReference = storageReference.child(storageReferenceFolder);

    // Delete existing file from storage
    updateReference
        .child(previousFileName)
        .delete()
        .addOnCompleteListener(task -> {
          if (task.isSuccessful()) {
            // Add updated file to storage
            updateReference
                .child(updatedFileName)
                .putFile(updatedUri)
                .addOnSuccessListener(taskSnapshot -> {
                  // Update field updatedAt and if referenceImage was updated, update markerImg.uri in Firestore
                  if (storageReferenceFolder.equals(getString(R.string.reference_images_ref))) {
                    updateUpdatedAtField(editingMarker.getUser(), updatedFileName);
                  } else {
                    updateUpdatedAtField(editingMarker.getUser(), null);
                  }
                })
                .addOnFailureListener(e -> Log.e(TAG, "onFailure: Could not upload updated uri to Storage", e));
          } else {
            Log.i(TAG, "onDelete: Could not delete previous file from storage", task.getException());
          }
        });
  }

  private void addMarkerDocument() {

    String title = etTitle.getText().toString().trim();
    String description = etDescription.getText().toString().trim();

    DocumentReference currentUserDoc = firebaseFirestore.collection(User.KEY_USERS).document(currentUser.getUid());

    Map<String, Object> markerImg = new HashMap<>();
    markerImg.put(Marker.KEY_URI, "");
    markerImg.put(Marker.KEY_FILENAME, currentUser.getUid() + getFileName(referenceImgUri));

    Map<String, Object> augmentedObj = new HashMap<>();
    augmentedObj.put(Marker.KEY_URI, augmentedObjUri.toString());
    augmentedObj.put(Marker.KEY_FILENAME, currentUser.getUid() + getFileName(augmentedObjUri));

    if (!title.isEmpty() && !description.isEmpty() &&
        currentUserDoc != null &&
        referenceImgUri != null && augmentedObjUri != null &&
        markerLoc == null && rbAddLocation.isChecked() && rbAddLocation.isSelected()) {
      Marker marker = new Marker();
      marker.setTitle(title);
      marker.setDescription(description);
      marker.setUser(currentUserDoc);
      marker.setCreatedAt(FieldValue.serverTimestamp());
      marker.setUpdatedAt(FieldValue.serverTimestamp());
      marker.setMarkerImg(markerImg);
      marker.setAugmentedObj(augmentedObj);

      if (markerLoc != null && rbAddLocation.isChecked() && rbAddLocation.isSelected()) {
        Map<String, Object> location = new HashMap<>();
        location.put(Marker.KEY_LATITUDE, markerLoc.latitude);
        location.put(Marker.KEY_LONGITUDE, markerLoc.longitude);
        marker.setLocation(location);
      }

      currentUserDoc.
          collection(Marker.KEY_UPLOADED_MARKERS)
          .add(marker)
          .addOnSuccessListener(documentReference -> {
            Log.i(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());

            uploadFilesToStorage(referenceImgUri, augmentedObjUri, documentReference.getId(), currentUserDoc, marker);
          })
          .addOnFailureListener(e -> {
            Log.e(TAG, "Error adding document", e);
            return;
          });
    } else {
      Toast.makeText(this, "One more fields empty!", Toast.LENGTH_LONG).show();
      return;
    }

  }

  private void uploadFilesToStorage(Uri referenceUri, Uri augmentedUri, String markerId, DocumentReference currentUserDoc, Marker marker) {
    // Create child references
    referenceImagesReference = storageReference.child(getString(R.string.reference_images_ref) + currentUser.getUid() + "_" + markerId + getFileName(referenceImgUri));
    augmentedObjReference = storageReference.child(getString(R.string.augmented_object_ref) + currentUser.getUid() + "_" + markerId + getFileName(augmentedObjUri));

    referenceImagesReference
        .putFile(referenceUri)
        .addOnFailureListener(e -> {
          // Handle unsuccessful uploads
          Log.e(TAG, "Unable to upload reference image to storage", e);
          // Files were not added to storage and must delete created marker document
          deleteCreatedMarkerDocument(currentUserDoc, markerId);
        })
        .addOnSuccessListener(taskSnapshot -> augmentedObjReference
            .putFile(augmentedUri)
            .addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Unable to upload augmented object to storage", e);
                deleteCreatedMarkerDocument(currentUserDoc, markerId);
              }
            })
            .addOnSuccessListener(taskSnapshot1 -> {
              Log.i(TAG, "Both files were successfully added to storage");
              // Files were added to storage and update field updatedAt for user doc
              //  and rename file names of marker and augmented object in marker document
              //  as well as its updatedAt field.
              updateCreatedMarkerDocument(currentUserDoc, markerId, marker);
            }));

    etTitle.setText("");
    etDescription.setText("");
    ivReferenceImage.setImageResource(0);
    tvSelectedAugmentedObject.setText(R.string.only_glb_assets);
  }

  private void updateCreatedMarkerDocument(DocumentReference currentUserDoc, String markerId, Marker marker) {

    referenceImagesReference
        .getDownloadUrl()
        .addOnSuccessListener(uri -> {
          String updatedReferenceImageUriString = uri.toString();

          Object newUpdateDateTime = FieldValue.serverTimestamp();
          currentUserDoc
              .update(Marker.KEY_UPDATED_AT, newUpdateDateTime)
              .addOnSuccessListener(unused -> {
                String updatedReferenceImgFileName = currentUser.getUid() + "_" + markerId + getFileName(referenceImgUri);
                String updatedAugmentedObjectFileName = currentUser.getUid() + "_" + markerId + getFileName(augmentedObjUri);
                currentUserDoc
                    .collection(Marker.KEY_UPLOADED_MARKERS)
                    .document(markerId)
                    .update(
                        Marker.KEY_AUGMENTED_OBJ + "." + Marker.KEY_FILENAME, updatedAugmentedObjectFileName,
                        Marker.KEY_MARKER_IMG + "." + Marker.KEY_FILENAME, updatedReferenceImgFileName,
                        Marker.KEY_MARKER_IMG + "." + Marker.KEY_URI, updatedReferenceImageUriString,
                        Marker.KEY_UPDATED_AT, newUpdateDateTime
                    )
                    .addOnSuccessListener(unused1 -> {
                      Map<String, Object> updatedMarkerImg = marker.getMarkerImg();
                      updatedMarkerImg.put(Marker.KEY_FILENAME, updatedReferenceImgFileName);
                      updatedMarkerImg.put(Marker.KEY_URI, updatedReferenceImageUriString);
                      marker.setMarkerImg(updatedMarkerImg);

                      Map<String, Object> updatedAugmentedObj = marker.getAugmentedObj();
                      updatedAugmentedObj.put(Marker.KEY_FILENAME, updatedAugmentedObjectFileName);
                      marker.setAugmentedObj(updatedAugmentedObj);

                      Log.i(TAG, "Successfully updated both user and marker documents!");

                      Toast.makeText(AddMarkerActivity.this, "Marker successfully uploaded!", Toast.LENGTH_LONG).show();

                      String checkFlag = getIntent().getStringExtra(getString(R.string.flag));
                      Intent intent;
                      if (checkFlag != null && checkFlag.equals(getString(R.string.uploaded_markers_activity))) {
                        // Came from UploadedMarkersActivity
                        intent = new Intent();
                        Log.i(TAG, "marker id to uploaded markers: " + markerId);
                        intent.putExtra(getString(R.string.new_marker_uid), markerId);
                        setResult(RESULT_OK, intent);
                      } else {
                        intent = new Intent(AddMarkerActivity.this, UploadedMarkersActivity.class);
                        startActivity(intent);
                      }
                      finish();
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating marker document", e));
              })
              .addOnFailureListener(e -> Log.e(TAG, "Error updating user document", e));
        })
        .addOnFailureListener(e -> Log.e(TAG, "onFailure: could not get download url of newly uploaded reference image"));
  }

  private void deleteCreatedMarkerDocument(DocumentReference currentUserDoc, String markerId) {
    currentUserDoc.collection(Marker.KEY_UPLOADED_MARKERS).document(markerId)
        .delete()
        .addOnSuccessListener(unused -> Log.i(TAG, "DocumentSnapshot successfully deleted!"))
        .addOnFailureListener(e -> Log.e(TAG, "Error deleting document", e));
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

  private void openFileChooser(View v) {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

    switch (v.getId()) {
      case R.id.btnFindReferenceImg:
        String[] referenceImgMimeTypes = {getString(R.string.mime_type_jpeg), getString(R.string.mime_type_png)};
        intent.setType(getString(R.string.type_images));
        intent.putExtra(Intent.EXTRA_MIME_TYPES, referenceImgMimeTypes);
        startActivityForResult(intent, REFERENCE_IMG_REQUEST_CODE);
        break;
      case R.id.btnFindAugmentedObject:
        intent.setType(getString(R.string.type_3d_models));
        startActivityForResult(intent, AUGMENTED_OBJ_REQUEST_CODE);
        break;
      default:
        break;
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if(resultCode == RESULT_OK && data != null) {
      switch (requestCode) {
        case REFERENCE_IMG_REQUEST_CODE:
          // Get the URI of the selected file
          referenceImgUri = data.getData();
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            this.getContentResolver().takePersistableUriPermission(referenceImgUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
          }
          Log.i(TAG, "Uri: " + referenceImgUri.toString());
          try {
            // Set selected image to imageView
            Glide.with(this).load(referenceImgUri).into(ivReferenceImage);

          } catch (Exception e) {
            Log.e(TAG, "File select error", e);
          }
          break;
        case AUGMENTED_OBJ_REQUEST_CODE:
          // Get the URI of the selected file
          augmentedObjUri = data.getData();
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            this.getContentResolver().takePersistableUriPermission(augmentedObjUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
          }
          Log.i(TAG, "Uri: " + augmentedObjUri.toString());
          try {
            // Set selected augmented object file name to TextView
            tvSelectedAugmentedObject.setText("Selected: " + getFileName(augmentedObjUri));

          } catch (Exception e) {
            Log.e(TAG, "File select error", e);
          }
          break;
        case NEW_MARKER_LOC_REQUEST_CODE:
          double[] latLng = data.getDoubleArrayExtra(getString(R.string.new_marker_latlng));

          if (latLng[0] != 0 && latLng[1] != 0) {
            markerLoc = new LatLng(latLng[0], latLng[1]);
            Toast.makeText(AddMarkerActivity.this, "Got location!", Toast.LENGTH_SHORT).show();
          } else {
            Log.e(TAG, "Invalid latitude and longitude values");
          }
          break;
        default:
          break;
      }
    }
  }

  private String getFileName(Uri uri) {
    String result = null;
    if (uri != null && uri.getScheme().equals(getString(R.string.content))) {
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

  private void goToLoginActivity() {
    Intent i = new Intent(this, LoginActivity.class);
    startActivity(i);
    finish();
  }

  private void goToMarkerMapActivity() {
    Intent i = new Intent(this, MarkerMapActivity.class);
    startActivityForResult(i, NEW_MARKER_LOC_REQUEST_CODE);
  }
}