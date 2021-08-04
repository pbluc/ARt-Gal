package com.fbu.pbluc.artgal.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.fbu.pbluc.artgal.ArViewActivity;
import com.fbu.pbluc.artgal.R;
import com.fbu.pbluc.artgal.callbacks.GlideCallback;
import com.fbu.pbluc.artgal.models.Marker;
import com.fbu.pbluc.artgal.models.User;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.ImageInsufficientQualityException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.assets.RenderableSource;
import com.google.ar.sceneform.collision.Box;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Collection;

public class CustomArFragment extends ArFragment {

  private static final String TAG = "CustomArFragment";

  public static final float MAX_SCALE = 10f;
  public static final float MIN_SCALE = 0.01f;
  public boolean fitModelToView = true;

  private FirebaseStorage firebaseStorage;
  private StorageReference storageReference;
  private StorageReference augmentedObjRef;
  private FirebaseFirestore firebaseFirestore;

  private DocumentReference trackedMarkerDoc;

  private AugmentedImageDatabase augmentedImageDatabase;

  private Marker trackedMarker;

  private String trackedAugmentedObjUri;

  private Anchor autoScaledAnchor;
  private Anchor unscaledAnchor;
  private AnchorNode autoScaledAnchorNode;
  private AnchorNode unscaledAnchorNode;
  public TransformableNode transformableNode;

  private RenderableSource renderableSource;
  private ModelRenderable modelRenderable;

  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

    firebaseStorage = FirebaseStorage.getInstance();
    storageReference = firebaseStorage.getReference();

    autoScaledAnchorNode = new AnchorNode();
    unscaledAnchorNode = new AnchorNode();

    return super.onCreateView(inflater, container, savedInstanceState);
  }

  @Override
  protected Config getSessionConfiguration(Session session) {

    Config config = new Config(session);
    config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
    config.setFocusMode(Config.FocusMode.AUTO);

    session.configure(config);

    this.getArSceneView().setupSession(session);

    setUpImageDatabase(session);

    return config;
  }

  public void setUpImageDatabase(Session session) {
    this.getPlaneDiscoveryController().show();

    firebaseFirestore = FirebaseFirestore.getInstance();

    augmentedImageDatabase = new AugmentedImageDatabase(session);

    firebaseFirestore
        .collectionGroup(Marker.KEY_UPLOADED_MARKERS)
        .get()
        .addOnSuccessListener(queryDocumentSnapshots -> {
          final int[] totalReferenceImages = {queryDocumentSnapshots.size()};
          Log.i(TAG, "number of reference images: ");
          for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
            Marker resultMarker = document.toObject(Marker.class);

            String referenceImgFileName = resultMarker.getMarkerImg().get(Marker.KEY_FILENAME).toString();
            Uri referenceImgUri = Uri.parse(resultMarker.getMarkerImg().get(Marker.KEY_URI).toString());

            convertUriToBitmap(referenceImgUri, bitmap -> {
              try {
                if (bitmap != null) {
                  augmentedImageDatabase.addImage(referenceImgFileName, bitmap);
                }
              } catch (ImageInsufficientQualityException e) {
                Log.i(TAG, "Image quality was insufficient! fileName: " + referenceImgFileName);
                totalReferenceImages[0] -= 1;
              }
              Log.i(TAG, "Size of augmented image database: " + augmentedImageDatabase.getNumImages());

              if (augmentedImageDatabase.getNumImages() == totalReferenceImages[0]) {
                Log.i(TAG, "All images have been added to augmented image database");

                // Update session and configuration
                Config changedConfig = this.getArSceneView().getSession().getConfig();
                changedConfig.setAugmentedImageDatabase(augmentedImageDatabase);
                this.getArSceneView().getSession().configure(changedConfig);
              }
            });
          }
        })
        .addOnFailureListener(e -> Log.e(TAG, "Could not get all documents across all subcollections of uploadedMarkers", e));
  }

  public void convertUriToBitmap(Uri uri, final GlideCallback glideCallback) {
    Glide.with(this)
        .asBitmap()
        .load(uri)
        .into(new CustomTarget<Bitmap>() {
          @Override
          public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
            glideCallback.onSuccess(resource);
          }

          @Override
          public void onLoadCleared(@Nullable Drawable placeholder) {
          }
        });
  }

  @Override
  public void onUpdate(FrameTime frameTime) {
    Frame frame = this.getArSceneView().getArFrame();
    Collection<AugmentedImage> images = frame.getUpdatedTrackables(AugmentedImage.class);

    for (AugmentedImage image : images) {
      if (image.getTrackingState() == TrackingState.TRACKING && image.getTrackingMethod() == AugmentedImage.TrackingMethod.FULL_TRACKING) {
        this.getPlaneDiscoveryController().hide();
        this.getPlaneDiscoveryController().setInstructionView(null);

        Log.i(TAG, "Tracked image file name: " + image.getName());
        trackedMarkerDoc = firebaseFirestore
            .collection(User.KEY_USERS)
            .document(image.getName().substring(0, 28))
            .collection(Marker.KEY_UPLOADED_MARKERS)
            .document(image.getName().substring(29, 49));

        trackedMarkerDoc
            .get()
            .addOnSuccessListener(documentSnapshot -> {
              if (documentSnapshot.exists()) {
                trackedMarker = documentSnapshot.toObject(Marker.class);
                trackedAugmentedObjUri = trackedMarker.getAugmentedObj().get(Marker.KEY_FILENAME).toString();

                if (fitModelToView) {
                  autoScaledAnchor = image.createAnchor(image.getCenterPose());
                  autoScaledAnchorNode.setAnchor(autoScaledAnchor);
                  createModel(autoScaledAnchor, trackedAugmentedObjUri);
                } else {
                  unscaledAnchor = image.createAnchor(image.getCenterPose());
                  unscaledAnchorNode.setAnchor(unscaledAnchor);
                  createModel(unscaledAnchor, trackedAugmentedObjUri);
                }

              }
            })
            .addOnFailureListener(e -> Log.e(TAG, "onFailure: getting tracked marker document failed", e));
      }
    }
  }

  private void createModel(Anchor anchor, String fileName) {
    augmentedObjRef = storageReference.child(getString(R.string.augmented_object_ref) + fileName);

    augmentedObjRef
        .getDownloadUrl()
        .addOnSuccessListener(augmentedObjUri -> {
          Log.i(TAG, "onSuccess: Retrieved augmented object file");

          buildModel(augmentedObjUri, anchor);
        })
        .addOnFailureListener(e -> Log.e(TAG, "onFailure: Could not get augmented object file", e));
  }

  private void buildModel(Uri augmentedObjUri, Anchor anchor) {

    /* When you build a Renderable, Sceneform loads model and related resources
     * in the background while returning a CompletableFuture.
     * Call thenAccept(), handle(), or check isDone() before calling get().
     */

    //Log.i(TAG, "augmented object file path: " + augmentedObjFile.getPath());
    renderableSource = RenderableSource
        .builder()
        .setSource(getContext(), augmentedObjUri, RenderableSource.SourceType.GLB)
        .setRecenterMode(RenderableSource.RecenterMode.ROOT)
        .build();


    ModelRenderable
        .builder()
        .setSource(getContext(), renderableSource)
        .setRegistryId(augmentedObjUri)
        .build()
        .thenAccept(modelRenderable -> {
          this.modelRenderable = modelRenderable;
          Log.i(TAG, "onSuccess: Model built");
          placeModel(anchor);
        })
        .exceptionally(
            throwable -> {
              Log.e(TAG, "onFailure: model not built" + throwable.getMessage());
              throwable.printStackTrace();
              return null;
            });
  }

  private void placeModel(Anchor anchor) {
    Log.i(TAG, "modelRenderable: " + modelRenderable.toString());

    if (fitModelToView) {
      this.getArSceneView().getScene().removeChild(unscaledAnchorNode);
      if (unscaledAnchorNode.getAnchor() != null) {
        unscaledAnchorNode.getAnchor().detach();
      }
      unscaledAnchorNode.setParent(null);

      autoScaledAnchorNode.setAnchor(anchor);
      autoScaledAnchorNode.setParent(this.getArSceneView().getScene());

      scaleModel(autoScaledAnchorNode);

      // Enable object scaling
      transformableNode.setParent(autoScaledAnchorNode);
      transformableNode.setRenderable(modelRenderable);
      transformableNode.select();

    } else {
      this.getArSceneView().getScene().removeChild(autoScaledAnchorNode);
      if (autoScaledAnchorNode.getAnchor() != null) {
        autoScaledAnchorNode.getAnchor().detach();
      }
      autoScaledAnchorNode.setParent(null);

      unscaledAnchorNode.setAnchor(anchor);
      unscaledAnchorNode.setParent(this.getArSceneView().getScene());

      // TODO: Disable transformable node on unscaled anchor node
      transformableNode.setParent(unscaledAnchorNode);
      transformableNode.setRenderable(modelRenderable);
      transformableNode.select();
    }
  }

  private void scaleModel(AnchorNode node) {
    float wantedRealRadius = 0.5f;

    // Retrieving abstract collision shape (box) of model renderable
    Box startingCollisionShape = (Box) modelRenderable.getCollisionShape();

    // Checking the size of the renderable through the dimensions of the collision shape
    float xRadius = startingCollisionShape.getSize().x;
    float yRadius = startingCollisionShape.getSize().y;
    float zRadius = startingCollisionShape.getSize().z;

    // Largest dimension of box collision shape
    float encompassingRadius = Math.max(Math.max(xRadius, yRadius), zRadius);

    // Check if scaling is needed
    if (encompassingRadius > wantedRealRadius) {
      // Determine the unique scale ratio
      float scaleFactor = 1 / (encompassingRadius / wantedRealRadius);

      // Set a world scale on the anchor node using ratio
      node.setWorldScale(new Vector3(scaleFactor, scaleFactor, scaleFactor));
    }
  }

}