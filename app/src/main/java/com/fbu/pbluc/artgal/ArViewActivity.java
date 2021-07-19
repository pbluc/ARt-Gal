package com.fbu.pbluc.artgal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;

import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.assets.RenderableSource;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;


public class ArViewActivity extends AppCompatActivity implements Scene.OnUpdateListener {

  private static final String TAG = "ArViewActivity";

  private FirebaseStorage firebaseStorage;
  private StorageReference storageReference;
  private StorageReference referenceImagesRef;
  private StorageReference augmentedObjRef;

  private CustomArFragment arFragment;

  private File augmentedObjFile;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_ar_view);

    arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.ar_fragment);
    arFragment.getArSceneView().getScene().addOnUpdateListener(this);

    firebaseStorage = FirebaseStorage.getInstance();
    storageReference = firebaseStorage.getReference();
    referenceImagesRef = storageReference.child("referenceImages/");
    augmentedObjRef = storageReference.child("augmentedObjects/");

  }

  public void setUpDatabase(Config config, Session session) {
    AugmentedImageDatabase augmentedImageDatabase = new AugmentedImageDatabase(session);
    // Create bitmaps for all markers in Firebase Storage database
    referenceImagesRef
        .listAll()
        .addOnSuccessListener(new OnSuccessListener<ListResult>() {
          @Override
          public void onSuccess(ListResult listResult) {
            for(StorageReference fileRef : listResult.getItems()) {
              // Download the file using its reference (fileRef)
              String markerFileName = fileRef.getName();
              fileRef
                  .getDownloadUrl()
                  .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri markerImgUri) {
                      Bitmap markerImgBitmap = null;
                      try {
                        ParcelFileDescriptor parcelFileDescriptor =
                            getContentResolver().openFileDescriptor(markerImgUri, "r");
                        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        markerImgBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);

                        if(markerImgBitmap == null) {
                          Log.i(TAG, "markerImgBitmap is null");
                        } else {
                          Log.i(TAG, "markerImgBitmap is NOT null");
                          augmentedImageDatabase.addImage(markerFileName, markerImgBitmap);
                        }

                        parcelFileDescriptor.close();
                      } catch (IOException e) {
                        e.printStackTrace();
                      }
                    }
                  })
                  .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                      Log.e(TAG, "File not found", e);
                    }
                  });
            }
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {
            // Handle any errors
            Log.e(TAG, "Could not retrieve all reference image files", e);
          }
        });
    // Add that augmented image database to our AR session
    config.setAugmentedImageDatabase(augmentedImageDatabase);
  }

  @Override
  public void onUpdate(FrameTime frameTime) {
    // Every time there is something new to our scene we will create a frame
    Frame frame = arFragment.getArSceneView().getArFrame();
    // From this frame we will collect all the augmented images that are being tracked
    Collection<AugmentedImage> images = frame.getUpdatedTrackables(AugmentedImage.class);
    Log.i(TAG, "images collection size: " + images.size());

    // Go through each image in a for each loop and check if our current marker image is being tracked
    // And if that marker image is being tracked then we will create an anchor and place the 3D model
    for (AugmentedImage image:images) {
      if (image.getTrackingState() == TrackingState.TRACKING) {
        Anchor anchor = image.createAnchor(image.getCenterPose());

        // TODO: Find corresponding augmented object file in Firebase Storage
        // Find and retrieve 3D model from Firebase Storage
        augmentedObjRef
            .listAll()
            .addOnSuccessListener(new OnSuccessListener<ListResult>() {
              @Override
              public void onSuccess(ListResult listResult) {
                Log.i(TAG, "image name: " + image.getName());
                for(StorageReference fileRef : listResult.getItems()) {
                  String augmentedObjFileName = fileRef.getName();
                  Log.i(TAG, "augmentedObjFileName: " + augmentedObjFileName);
                  if(augmentedObjFileName.substring(0, 49).equals(image.getName().substring(0,49))) {
                    try {
                      // TODO: Remove suffix in the fileName string and determine correct suffix between gltf or glb
                      augmentedObjFile = File.createTempFile(augmentedObjFileName, "gltf");

                      augmentedObjRef
                          .getFile(augmentedObjFile)
                          .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                              // Now that we have the anchor we can create our 3D model and place it on top of the anchor
                              createModel(anchor, augmentedObjFile);
                            }
                          })
                          .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                              Log.e(TAG, "Augmented object file not found", e);
                            }
                          });
                    } catch (IOException e) {
                      e.printStackTrace();
                    }
                  }
                }
              }
            })
            .addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Could not retrieve all augmented object files", e);
              }
            });
      }
    }
  }

  private void createModel(Anchor anchor, File augmentedObjFile) {

    // TODO: Check if augmented object file is of tyoe GLTF2 or GLB
    RenderableSource renderableSource = RenderableSource
        .builder()
        .setSource(this, Uri.parse(augmentedObjFile.getPath()), RenderableSource.SourceType.GLTF2)
        .setRecenterMode(RenderableSource.RecenterMode.ROOT)
        .build();

    ModelRenderable.builder()
        .setSource(this, renderableSource)
        .build()
        .thenAccept(modelRenderable -> placeModel(modelRenderable, anchor));
  }

  private void placeModel(ModelRenderable modelRenderable, Anchor anchor) {
    AnchorNode anchorNode = new AnchorNode(anchor);
    anchorNode.setRenderable(modelRenderable);
    arFragment.getArSceneView().getScene().addChild(anchorNode);
  }
}