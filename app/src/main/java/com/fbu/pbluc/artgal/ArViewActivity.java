package com.fbu.pbluc.artgal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.fbu.pbluc.artgal.models.Marker;
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
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.assets.RenderableSource;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.common.io.Files;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;


public class ArViewActivity extends AppCompatActivity implements Scene.OnUpdateListener {

  private static final String TAG = "ArViewActivity";

  private CustomArFragment arFragment;

  private FirebaseStorage firebaseStorage;
  private StorageReference storageReference;

  private ModelRenderable renderable;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_ar_view);

    arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.ar_fragment);
    arFragment.getArSceneView().getScene().addOnUpdateListener(this);

    firebaseStorage = FirebaseStorage.getInstance();
    storageReference = firebaseStorage.getReference();

  }

  public void setUpDatabase(Config config, Session session) {
    Bitmap heartsCradleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.hearts_cradle);
    AugmentedImageDatabase augmentedImageDatabase = new AugmentedImageDatabase(session);
    augmentedImageDatabase.addImage("heart's cradle", heartsCradleBitmap);
    config.setAugmentedImageDatabase(augmentedImageDatabase);
  }


  @Override
  public void onUpdate(FrameTime frameTime) {

    Frame frame = arFragment.getArSceneView().getArFrame();
    Collection<AugmentedImage> images = frame.getUpdatedTrackables(AugmentedImage.class);

    for (AugmentedImage image : images) {
      if (image.getTrackingState() == TrackingState.TRACKING) {
        if (image.getName().equals("heart's cradle")) {
          //Log.i(TAG, "Detected image");
          Anchor anchor = image.createAnchor(image.getCenterPose());

          createModel(anchor);
        }
      }
    }

  }

  private void createModel(Anchor anchor) {
    StorageReference augmentedObjRef = storageReference.child("augmentedObjects/scene.glb");

    try {
      File augmentedObjFile = File.createTempFile("scene", "glb");

      augmentedObjRef
          .getFile(augmentedObjFile)
          .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
              Log.i(TAG, "onSuccess: Retrieved augmented object file");

              buildModel(augmentedObjFile, anchor);
            }
          })
          .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
              Log.e(TAG, "onFailure: Could not get augmented object file", e);
            }
          });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void buildModel(File augmentedObjFile, Anchor anchor) {
    final String GLTF_ASSET =
        "https://github.com/KhronosGroup/glTF-Sample-Models/raw/master/2.0/Duck/glTF/Duck.gltf";

    /* When you build a Renderable, Sceneform loads model and related resources
     * in the background while returning a CompletableFuture.
     * Call thenAccept(), handle(), or check isDone() before calling get().
     */

    Log.i(TAG, "augmented object file path: " + augmentedObjFile.getPath());
    RenderableSource renderableSource = RenderableSource
        .builder()
        .setSource(ArViewActivity.this, Uri.parse(GLTF_ASSET), RenderableSource.SourceType.GLTF2)
        .setScale(0.1f)  // Scale the original model to 10%
        .setRecenterMode(RenderableSource.RecenterMode.ROOT)
        .build();


    ModelRenderable
        .builder()
        .setSource(this, renderableSource)
        .setRegistryId(GLTF_ASSET)
        .build()
        .thenAccept(modelRenderable -> {
          Log.i(TAG, "onSuccess: Model built");
          Toast.makeText(ArViewActivity.this, "Model built", Toast.LENGTH_SHORT).show();
          placeModel(modelRenderable, anchor);
        })
        .exceptionally(
            throwable -> {
              Log.e(TAG, "onFailure: model not built" + throwable.getMessage());
              throwable.printStackTrace();
              return null;
            });
  }

  private void placeModel(ModelRenderable modelRenderable, Anchor anchor) {
    Log.i(TAG, "modelRenderable: " + modelRenderable.toString());

    AnchorNode anchorNode = new AnchorNode(anchor);
    anchorNode.setRenderable(modelRenderable);
    arFragment.getArSceneView().getScene().addChild(anchorNode);
  }

}