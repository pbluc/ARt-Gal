package com.fbu.pbluc.artgal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.CamcorderProfile;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.fbu.pbluc.artgal.callbacks.GlideCallback;
import com.fbu.pbluc.artgal.fragments.CustomArFragment;
import com.fbu.pbluc.artgal.helpers.CameraPermissionHelper;
import com.fbu.pbluc.artgal.helpers.VideoRecorder;
import com.fbu.pbluc.artgal.models.Marker;
import com.fbu.pbluc.artgal.models.User;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;

import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.ImageInsufficientQualityException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
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

import static com.fbu.pbluc.artgal.fragments.CustomArFragment.MAX_SCALE;
import static com.fbu.pbluc.artgal.fragments.CustomArFragment.MIN_SCALE;


public class ArViewActivity extends AppCompatActivity {

  private static final String TAG = "ArViewActivity";

  private CustomArFragment arFragment;
  private ImageView ivVideoRecording;
  private ImageView ivAutoScaleModel;

  private VideoRecorder videoRecorder;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_ar_view);

    arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.ar_fragment);
    arFragment.getPlaneDiscoveryController().hide();
    arFragment.getPlaneDiscoveryController().setInstructionView(null);

    ivVideoRecording = findViewById(R.id.ivVideoRecording);
    ivAutoScaleModel = findViewById(R.id. ivAutoScaleModel);

    // Create the transformable
    arFragment.transformableNode = new TransformableNode(arFragment.getTransformationSystem());
    arFragment.transformableNode.getScaleController().setMaxScale(MAX_SCALE);
    arFragment.transformableNode.getScaleController().setMinScale(MIN_SCALE);

    // Create a new video recorder instance.
    videoRecorder = new VideoRecorder();
    // Specify the AR scene view to be recorded.
    videoRecorder.setSceneView(arFragment.getArSceneView());
    // Set video quality and recording orientation to match that of the device.
    int orientation = getResources().getConfiguration().orientation;
    videoRecorder.setVideoQuality(CamcorderProfile.QUALITY_2160P, orientation);

    ivVideoRecording.setOnClickListener(v -> {
      boolean recording = videoRecorder.onToggleRecord();
      if(recording) {
        // Recording has started
        ivVideoRecording.setImageDrawable(getResources().getDrawable(R.drawable.ic_stop_recording));
      } else {
        // Recording has stopped
        ivVideoRecording.setImageDrawable(getResources().getDrawable(R.drawable.ic_start_recording));
        Toast.makeText(ArViewActivity.this, "Video recording saved!", Toast.LENGTH_SHORT).show();
      }
    });

    ivAutoScaleModel.setOnClickListener(v -> {
      arFragment.fitModelToView = !arFragment.fitModelToView;
    });

    arFragment.transformableNode.setOnTapListener((hitTestResult, motionEvent) -> unrenderModelOnTap(hitTestResult, motionEvent));

  }

  private void unrenderModelOnTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
    Log.i(TAG, "unrenderModelOnTap");

    arFragment.onPeekTouch(hitTestResult, motionEvent);

    // We are only interested in the ACTION_UP events - anything else just return
    if (motionEvent.getAction() != MotionEvent.ACTION_UP) {
      return;
    }

    // Check for touching a Sceneform node
    if (hitTestResult.getNode() != null) {
      Log.i(TAG, "unrenderModelOnTap hitTestResult.getNode() != null");
      Node hitNode = hitTestResult.getNode();

      arFragment.getArSceneView().getScene().removeChild(hitNode);
      TransformableNode hitNodeAnchor = (TransformableNode) hitNode;
      if(hitNodeAnchor != null) {
        AnchorNode parentNodeAnchor = (AnchorNode) hitNodeAnchor.getParent();
        parentNodeAnchor.getAnchor().detach();
      }
      hitNode.setParent(null);
    }
  }


  @Override
  protected void onPause() {
    super.onPause();
    arFragment.getArSceneView().getSession().pause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Request the camera permission, if necessary.
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      CameraPermissionHelper.requestCameraPermission(this);
    }
  }

  @Override
  protected void onDestroy() {
    AsyncTask.execute(() -> arFragment.getArSceneView().getSession().close());
    super.onDestroy();
  }
}