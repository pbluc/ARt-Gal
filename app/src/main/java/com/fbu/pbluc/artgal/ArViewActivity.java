package com.fbu.pbluc.artgal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.fbu.pbluc.artgal.callbacks.FirebaseCallback;
import com.fbu.pbluc.artgal.models.Marker;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;

import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.ImageInsufficientQualityException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.assets.RenderableSource;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import io.grpc.Context;


public class ArViewActivity extends AppCompatActivity implements Scene.OnUpdateListener {

  private static final String TAG = "ArViewActivity";

  private CustomArFragment arFragment;

  private FirebaseStorage firebaseStorage;
  private StorageReference storageReference;
  private StorageReference augmentedObjRef;
  private FirebaseFirestore firebaseFirestore;

  private DocumentReference trackedMarkerDoc;

  private AugmentedImageDatabase augmentedImageDatabase;

  private Marker trackedMarker;
  private String trackedAugmentedObjUri;

  private Anchor anchor;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_ar_view);

    arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.ar_fragment);
    arFragment.getArSceneView().getScene().addOnUpdateListener(this);

    firebaseStorage = FirebaseStorage.getInstance();
    storageReference = firebaseStorage.getReference();
  }

  public void setUpImageDatabase(Config config, Session session) {
    firebaseFirestore = FirebaseFirestore.getInstance();

    augmentedImageDatabase = new AugmentedImageDatabase(session);

    firebaseFirestore
        .collectionGroup(Marker.KEY_UPLOADED_MARKERS)
        .get()
        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
          @Override
          public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
            final int[] totalReferenceImages = {queryDocumentSnapshots.size()};
            Log.i(TAG, "number of reference images: ");
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
              Marker resultMarker = document.toObject(Marker.class);

              String referenceImgFileName = resultMarker.getMarkerImg().get(Marker.KEY_FILENAME).toString();
              Uri referenceImgUri = Uri.parse(resultMarker.getMarkerImg().get(Marker.KEY_URI).toString());

              convertUriToBitmap(referenceImgUri, new FirebaseCallback() {
                @Override
                public void onSuccess(Bitmap bitmap) {
                  try {
                    if(bitmap != null) {
                      augmentedImageDatabase.addImage(referenceImgFileName, bitmap);
                    }
                  } catch (ImageInsufficientQualityException e) {
                    Log.i(TAG, "Image quality was insufficient! fileName: " + referenceImgFileName);
                    totalReferenceImages[0] -= 1;
                  }
                  Log.i(TAG, "Size of augmented image database: " + augmentedImageDatabase.getNumImages());

                  if(augmentedImageDatabase.getNumImages() == totalReferenceImages[0]) {
                    // TODO: Update session and configuration
                    Log.i(TAG, "All images have been added to augmented image database");

                    Config changedConfig = arFragment.getArSceneView().getSession().getConfig();
                    changedConfig.setAugmentedImageDatabase(augmentedImageDatabase);
                    arFragment.getArSceneView().getSession().configure(changedConfig);
                  }
                }
              });
            }
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {
            Log.e(TAG, "Could not get all documents across all subcollections of uploadedMarkers", e);
          }
        });
  }

  public void convertUriToBitmap(Uri uri, final FirebaseCallback firebaseCallback) {
    Glide.with(this)
        .asBitmap()
        .load(uri)
        .into(new CustomTarget<Bitmap>() {
          @Override
          public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
            firebaseCallback.onSuccess(resource);
          }

          @Override
          public void onLoadCleared(@Nullable Drawable placeholder) {
          }
        });
  }

  @Override
  public void onUpdate(FrameTime frameTime) {

    Frame frame = arFragment.getArSceneView().getArFrame();
    Collection<AugmentedImage> images = frame.getUpdatedTrackables(AugmentedImage.class);
    for (AugmentedImage image : images) {
      if (image.getTrackingState() == TrackingState.TRACKING) {
        Log.i(TAG, "Tracked image file name: " + image.getName());
        trackedMarkerDoc = firebaseFirestore
            .collection("users")
            .document(image.getName().substring(0, 28))
            .collection("uploadedMarkers")
            .document(image.getName().substring(29, 49));

        trackedMarkerDoc
            .get()
            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
              @Override
              public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(documentSnapshot.exists()) {
                  trackedMarker = documentSnapshot.toObject(Marker.class);
                  trackedAugmentedObjUri = trackedMarker.getAugmentedObj().get(Marker.KEY_FILENAME).toString();

                  anchor = image.createAnchor(image.getCenterPose());

                  createModel(anchor, trackedAugmentedObjUri);
                }
              }
            })
            .addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "onFailure: getting tracked marker document failed", e);
              }
            });
      }
    }

  }

  private void createModel(Anchor anchor, String fileName) {
    augmentedObjRef = storageReference.child("augmentedObjects/" + fileName);

      augmentedObjRef
          .getDownloadUrl()
          .addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri augmentedObjUri) {
              Log.i(TAG, "onSuccess: Retrieved augmented object file");

              buildModel(augmentedObjUri, anchor);
            }
          })
          .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
              Log.e(TAG, "onFailure: Could not get augmented object file", e);
            }
          });


  }

  private void buildModel(Uri augmentedObjUri, Anchor anchor) {

    /* When you build a Renderable, Sceneform loads model and related resources
     * in the background while returning a CompletableFuture.
     * Call thenAccept(), handle(), or check isDone() before calling get().
     */

    //Log.i(TAG, "augmented object file path: " + augmentedObjFile.getPath());
    RenderableSource renderableSource = RenderableSource
        .builder()
        .setSource(ArViewActivity.this, augmentedObjUri, RenderableSource.SourceType.GLB)
        .setScale(0.25f)  // Scale the original model to 50%
        .setRecenterMode(RenderableSource.RecenterMode.ROOT)
        .build();


    ModelRenderable
        .builder()
        .setSource(this, renderableSource)
        .setRegistryId(augmentedObjUri)
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

  @Override
  protected void onPause() {
    arFragment.getArSceneView().getSession().pause();
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    AsyncTask.execute(new Runnable() {
      @Override
      public void run() {
        arFragment.getArSceneView().getSession().close();
      }
    });
    super.onDestroy();
  }
}