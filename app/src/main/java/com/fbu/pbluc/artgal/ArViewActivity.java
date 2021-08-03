package com.fbu.pbluc.artgal;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;

import android.content.pm.PackageManager;
import android.media.CamcorderProfile;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.Toast;

import com.fbu.pbluc.artgal.fragments.CustomArFragment;
import com.fbu.pbluc.artgal.helpers.MyVideoRecorder;

import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.ux.TransformableNode;

import static com.fbu.pbluc.artgal.fragments.CustomArFragment.MAX_SCALE;
import static com.fbu.pbluc.artgal.fragments.CustomArFragment.MIN_SCALE;


public class ArViewActivity extends AppCompatActivity {

  private static final String TAG = "ArViewActivity";
  private static final int CAMERA_PERMISSION_CODE = 0;

  private CustomArFragment arFragment;
  private ImageView ivVideoRecording;
  private ImageView ivAutoScaleModel;

  private MyVideoRecorder myVideoRecorder;

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
    myVideoRecorder = new MyVideoRecorder();
    // Specify the AR scene view to be recorded.
    myVideoRecorder.setSceneView(arFragment.getArSceneView());
    // Set video quality and recording orientation to match that of the device.
    int orientation = getResources().getConfiguration().orientation;
    myVideoRecorder.setVideoQuality(CamcorderProfile.QUALITY_2160P, orientation);

    ivVideoRecording.setOnClickListener(v -> {
      boolean recording = myVideoRecorder.onToggleRecording();
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

  // TODO: Pause and resume properly with ArViewActivity lifecycle

  @Override
  protected void onPause() {
    super.onPause();
    if (arFragment.getArSceneView().getSession() != null) {
      arFragment.getArSceneView().getSession().pause();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Check if we do not have permissions to use the camera
    if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      // If not, request permissions for camera
      ActivityCompat.requestPermissions(
          this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);

      if (arFragment.getArSceneView().getSession() != null) {
        try {
          arFragment.getArSceneView().getSession().resume();
        } catch (CameraNotAvailableException e) {
          e.printStackTrace();
        }
      }
    }
  }

  @Override
  protected void onDestroy() {
    if (arFragment.getArSceneView().getSession() != null) {
      AsyncTask.execute(() -> {
        arFragment.getArSceneView().getSession().close();
      });
    }

    super.onDestroy();
  }

}