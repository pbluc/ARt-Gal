package com.fbu.pbluc.artgal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

public class UploadMarkerActivity extends AppCompatActivity {

    private static final String TAG = "UploadMarkerActivity";

    private static final int REFERENCE_IMG_REQUEST_CODE = 1; // onActivityResult request
    private static final int AUGMENTED_OBJ_REQUEST_CODE = 2;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private FirebaseStorage firebaseStorage;
    private StorageReference referenceImgsReference;
    private StorageReference augmentedObjectsReference;

    private EditText etTitle;
    private EditText etDescription;
    private Button btnFindReferenceImg;
    private Button btnFindAugmentedObject;
    private Button btnSubmit;
    private ImageView ivReferenceImage;
    private TextView tvSelectedAugmentedObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_marker);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        firebaseStorage = FirebaseStorage.getInstance();
        referenceImgsReference = firebaseStorage.getReference().child("referenceImages");
        augmentedObjectsReference = firebaseStorage.getReference().child("augmentedObjects");

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

    private void openFileChooser(View v) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        switch(v.getId()) {
            case R.id.btnFindReferenceImg:
                String[] referenceImgMimeTypes = {"image/jpeg","image/png"};
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
        switch(requestCode) {
            case REFERENCE_IMG_REQUEST_CODE:
                // If the file selection was successful
                if(resultCode == RESULT_OK) {
                    if(data != null) {
                        // Get the URI of the selected file
                        final Uri uri = data.getData();
                        Log.i(TAG, "Uri: " + uri.toString());
                        try {
                            // Get the file path from the URI
                            final String path = uri.getPath();
                            Toast.makeText(this, "File selected: " + path, Toast.LENGTH_SHORT).show();

                            // Set selected image to imageView
                            Glide.with(this).load(uri).into(ivReferenceImage);

                        } catch(Exception e) {
                            Log.e(TAG, "File select error", e);
                        }
                    } else {
                        return;
                    }
                }
                break;
            case AUGMENTED_OBJ_REQUEST_CODE:
                if(resultCode == RESULT_OK) {
                    if(data != null) {
                        // Get the URI of the selected file
                        final Uri uri = data.getData();
                        Log.i(TAG, "Uri: " + uri.toString());
                        try {
                            // Set selected augmented object file name to TextView
                            tvSelectedAugmentedObject.setText("Selected: " + getFileName(uri));

                        } catch(Exception e) {
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
        if(uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if(cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }

        if(result == null) {
            result = uri.getPath();
            int cut =  result.lastIndexOf('/');
            if(cut != -1) {
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