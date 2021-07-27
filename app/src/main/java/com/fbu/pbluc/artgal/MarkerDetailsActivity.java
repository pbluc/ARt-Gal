package com.fbu.pbluc.artgal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MarkerDetailsActivity extends AppCompatActivity {
  private final static String TAG = "MarkerDetailsActivity";

  private static final int EDIT_CURRENT_MARKER_REQUEST_CODE = 1;

  private FirebaseAuth firebaseAuth;
  private FirebaseStorage firebaseStorage;
  private FirebaseFirestore firebaseFirestore;
  private FirebaseUser currentUser;
  private StorageReference storageReference;
  private StorageReference markerImgReference;
  private StorageReference augmentedObjReference;


  private DocumentReference markerRef;

  private TextView tvTitle;
  private TextView tvDescription;
  private TextView tvAugmentedObjectFileName;
  private TextView tvCreatedAt;
  private TextView tvUserFullName;
  private TextView tvUsername;
  private ImageView ivReferenceImageMedia;
  private ImageView ivOriginalReferenceImageMedia;
  private ImageView ivDownloadImgUrl;
  private LinearLayout deleteMarkerLayoutContainer;
  private LinearLayout editMarkerLayoutContainer;

  private Marker marker;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_marker_details);

    firebaseAuth = FirebaseAuth.getInstance();
    firebaseStorage = FirebaseStorage.getInstance();
    firebaseFirestore = FirebaseFirestore.getInstance();
    currentUser = firebaseAuth.getCurrentUser();
    storageReference = firebaseStorage.getReference();

    tvTitle = findViewById(R.id.tvTitle);
    tvDescription = findViewById(R.id.tvDescription);
    tvAugmentedObjectFileName = findViewById(R.id.tvAugmentedObjectFileName);
    tvCreatedAt = findViewById(R.id.tvCreatedAt);
    tvUserFullName = findViewById(R.id.tvUserFullName);
    tvUsername = findViewById(R.id.tvUsername);
    ivReferenceImageMedia = findViewById(R.id.ivReferenceImageMedia);
    ivOriginalReferenceImageMedia = findViewById(R.id.ivOriginalReferenceImageMedia);
    ivDownloadImgUrl = findViewById(R.id.ivDownloadImgUrl);
    deleteMarkerLayoutContainer = findViewById(R.id.deleteMarkerLayoutContainer);
    editMarkerLayoutContainer = findViewById(R.id.editMarkerLayoutContainer);

    String userUid = getIntent().getStringExtra(getString(R.string.user_marker_uid));
    String markerUid = getIntent().getStringExtra(getString(R.string.clicked_marker_uid));


    markerRef = firebaseFirestore
        .collection(User.KEY_USERS)
        .document(userUid)
        .collection(Marker.KEY_UPLOADED_MARKERS)
        .document(markerUid);

    markerRef
        .get()
        .addOnSuccessListener(documentSnapshot -> {
          marker = documentSnapshot.toObject(Marker.class);

          DocumentReference markerUser = marker.getUser();

          tvTitle.setText(marker.getTitle());
          tvDescription.setText(marker.getDescription());
          tvAugmentedObjectFileName.setText(marker.getAugmentedObj().get(Marker.KEY_FILENAME).toString().substring(49));
          tvCreatedAt.setText(marker.formattedCreatedAt());

          markerUser
              .get()
              .addOnSuccessListener(documentSnapshot1 -> {
                User user = documentSnapshot1.toObject(User.class);
                tvUserFullName.setText(user.getName().get(User.KEY_FIRST_NAME).toString() + " " + user.getName().get(User.KEY_LAST_NAME).toString());
                tvUsername.setText(user.getUsername());
              });

          markerImgReference = storageReference.child(getString(R.string.reference_images_ref) + marker.getMarkerImg().get(Marker.KEY_FILENAME).toString());

          // Check if the augmented object file exists in Firebase Storage
          augmentedObjReference = storageReference.child(getString(R.string.augmented_object_ref) + marker.getAugmentedObj().get(Marker.KEY_FILENAME).toString());
          augmentedObjReference
              .getDownloadUrl()
              .addOnSuccessListener(uri -> Log.i(TAG, "Augmented object file found!"))
              .addOnFailureListener(e -> {
                // File not found
                Log.e(TAG, "Augmented object file not found", e);
              });

          markerImgReference
              .getDownloadUrl()
              .addOnSuccessListener(uri -> {
                Glide.with(MarkerDetailsActivity.this).load(uri).into(ivReferenceImageMedia);
                Glide.with(MarkerDetailsActivity.this).load(uri).into(ivOriginalReferenceImageMedia);
              })
              .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                  Log.e(TAG, "Could not get download url of marker img", e);
                }
              });
        });

    if (userUid.equals(currentUser.getUid())) {
      deleteMarkerLayoutContainer.setVisibility(View.VISIBLE);
      editMarkerLayoutContainer.setVisibility(View.VISIBLE);

      deleteMarkerLayoutContainer.setOnClickListener(v -> deleteMarkerFilesFromStorage());
      editMarkerLayoutContainer.setOnClickListener(v -> editCurrentMarker(userUid, markerUid));
    } else {
      deleteMarkerLayoutContainer.setVisibility(View.GONE);
      editMarkerLayoutContainer.setVisibility(View.GONE);
    }

    ivDownloadImgUrl.setOnClickListener(v -> {
      ActivityCompat.requestPermissions(MarkerDetailsActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
      ActivityCompat.requestPermissions(MarkerDetailsActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

      saveImageToGallery();
    });
  }

  private void editCurrentMarker(String userUid, String markerUid) {
    Intent intent = new Intent(this, AddMarkerActivity.class);
    intent.putExtra(getString(R.string.user_uid_editing_marker), userUid);
    intent.putExtra(getString(R.string.marker_uid_editing_marker), markerUid);
    startActivityForResult(intent, EDIT_CURRENT_MARKER_REQUEST_CODE);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
  }

  private void deleteMarkerFilesFromStorage() {
    // Delete the file
    markerImgReference
        .delete()
        .addOnSuccessListener(unused -> {
          // File deleted successfully
          Log.i(TAG, "Reference image file was successfully deleted");
          augmentedObjReference
              .delete()
              .addOnSuccessListener(unused1 -> {
                Log.i(TAG, "Both files were successfully deleted");
                deleteMarkerDocument();
              })
              .addOnFailureListener(e -> Log.e(TAG, "Augmented object file was not deleted!", e));
        })
        .addOnFailureListener(e -> {
          // Uh-oh, an error occurred!
          Log.e(TAG, "Reference image file was not deleted!", e);
        });
  }

  private void deleteMarkerDocument() {
    String deletedMarkerId = markerRef.getId();
    markerRef
        .delete()
        .addOnSuccessListener(unused -> {
          Log.i(TAG, "Marker document successfully deleted!");
          goToUploadedMarkersActivity(deletedMarkerId);
        })
        .addOnFailureListener(e -> Log.e(TAG, "Error deleting marker document", e));
  }

  private void goToUploadedMarkersActivity(String deletedMarkerId) {
    Intent i = new Intent();
    i.putExtra(getString(R.string.deleted_marker_uid), deletedMarkerId);
    setResult(RESULT_OK, i);
    finish();
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
    Intent i = new Intent(this, LoginActivity.class);
    startActivity(i);
    finish();
  }

  private void saveImageToGallery() {
    // Get the image from the ImageView
    BitmapDrawable markerImg = (BitmapDrawable) ivOriginalReferenceImageMedia.getDrawable();
    Bitmap markerImgBitmap = markerImg.getBitmap();

    String markerFileName = marker.getMarkerImg().get(Marker.KEY_FILENAME).toString();
    String markerFileExtension = markerFileName.substring(markerFileName.lastIndexOf(".") + 1);

    FileOutputStream outputStream = null;
    File storageLoc = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    File dir = new File(storageLoc.getAbsolutePath() + getString(R.string.art_gal));
    dir.mkdirs();

    String fileName = String.format("%d." + markerFileExtension, System.currentTimeMillis());
    File outFile = new File(dir, fileName);

    try {
      outputStream = new FileOutputStream(outFile);

      if (markerFileExtension.equals(getString(R.string.jpeg)) || markerFileExtension.equals(getString(R.string.jpg))) {
        markerImgBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
      } else if (markerFileExtension.equals(getString(R.string.png))) {
        markerImgBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
      }

      outputStream.flush();
      outputStream.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Refresh gallery
    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
    intent.setData(Uri.fromFile(outFile));
    this.sendBroadcast(intent);


    Toast.makeText(this, "Image saved to gallery!", Toast.LENGTH_SHORT).show();
    Log.i(TAG, "Saved image to gallery");
  }
}