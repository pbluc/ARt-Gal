package com.fbu.pbluc.artgal.helpers;

import android.content.res.Configuration;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.google.ar.sceneform.SceneView;

import java.io.File;
import java.io.IOException;


/**
 * Video Recorder class handles recording the contents of a SceneView.
 */
public class MyVideoRecorder {
  private static final String TAG = "MyVideoRecorder";

  private static final int DEFAULT_BITRATE = 10000000; // Number of bits used per second to represent video after source coding
  private static final int DEFAULT_FRAMERATE = 30; // Frequency (rate) at which consecutive images (frames) are captured or displayed

  private boolean recordingVideo; // recordingVideo is true when the media recorder is capturing video

  private MediaRecorder mediaRecorder; // Used to encode the video

  private Size videoSize; // Holds the width and height of the video in pixels

  private SceneView sceneView; // A Sceneform SurfaceView that manages rendering and interaction with the scene
  private File videoDirectory; // Directory where the video will be placed
  private File videoPath; // The file path of video on the device's internal storage
  private Surface encoderSurface; // The object holding pixels (view) that are being composited to the screen
  private String videoBaseName; // Prefix used in naming video files
  private int videoCodec; // Determines the video encoding definition used by Media Recorder
  private int bitRate = DEFAULT_BITRATE;
  private int frameRate = DEFAULT_FRAMERATE;

  // Video quality settings from predefined set of parameters in CamcorderProfile
  private static final int[] FALLBACK_QUALITY_LEVELS = {
      CamcorderProfile.QUALITY_HIGH,
      CamcorderProfile.QUALITY_2160P,
      CamcorderProfile.QUALITY_1080P,
      CamcorderProfile.QUALITY_720P,
      CamcorderProfile.QUALITY_480P
  };

  // Creates instance of Video Recorder with recordingVideo set to false as MediaRecorder is not encoding
  public MyVideoRecorder() {
    recordingVideo = false;
  }

  // Getters and setters
  public void setBitRate(int bitRate) {
    this.bitRate = bitRate;
  }

  public void setFrameRate(int frameRate) {
    this.frameRate = frameRate;
  }

  public void setSceneView(SceneView sceneView) {
    this.sceneView = sceneView;
  }

  public void setVideoCodec(int videoCodec) {
    this.videoCodec = videoCodec;
  }

  public void setVideoSize(int width, int height) {
    videoSize = new Size(width, height);
  }

  public boolean onToggleRecording() {
    if (recordingVideo) { // Scene view is being recorded
      // Stops the recording
      stopRecordingVideo();
    } else { // Scene view is not being recorded
      // Starts recording
      startRecordingVideo();
    }
    // Returns new recording state
    return recordingVideo;
  }

  private void stopRecordingVideo() {
    // Sets recording state to false as video is no longer being recorded
    recordingVideo = false;

    // When capturing is complete
    // We stop mirroring the SceneView to the specified Surface.
    if (encoderSurface != null) {
      sceneView.stopMirroringToSurface(encoderSurface);
      encoderSurface = null;
    }
    // Stops recording
    mediaRecorder.stop();
    // Media Recorder placed in idle state and can reuse the object
    mediaRecorder.reset();
  }

  private void startRecordingVideo() {
    // If we do not already have a Media Recorder instance, then create one
    if (mediaRecorder == null) {
      mediaRecorder = new MediaRecorder();
    }

    try {
      buildVideoFile();
      setUpMediaRecorder();
    } catch (IOException e) {
      Log.e(TAG, "Exception setting up recorder", e);
      return;
    }

    // Sets up Surface for the MediaRecorder
    encoderSurface = mediaRecorder.getSurface();

    // In order to record the contents of this view,
    // We select a Surface onto which our SceneView should be mirrored.
    sceneView.startMirroringToSurface(
        encoderSurface, 0, 0, videoSize.getWidth(), videoSize.getHeight());

    // Sets recording state to true as video is being recorded
    recordingVideo = true;
  }

  private void buildVideoFile() {
    // Checks to see if we have a video directory to use when putting newly created videos
    if (videoDirectory == null) {
      // If not, we create a new directory on the devices in storage Gallery under a folder title "Sceneform"
      videoDirectory =
          new File(
              Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                  + "/Sceneform");
    }
    // If we haven't already chosen a prefix to place on every video, then we make one
    if (videoBaseName == null || videoBaseName.isEmpty()) {
      videoBaseName = "Sample";
    }

    // Setting the path of the video path so that it will be in the device Gallery under a folder called "Sceneform"
    // With a fileName including the prefix we previously defined and the current time stamp appended as an MP4 video
    videoPath =
        new File(
            videoDirectory, videoBaseName + Long.toHexString(System.currentTimeMillis()) + ".mp4");
    // Retrieves the directory we are trying to place the video in
    File dir = videoPath.getParentFile();

    // If the directory we want does not exist, create it directly
    if (!dir.exists()) {
      dir.mkdirs();
    }
  }

  private void setUpMediaRecorder() throws IOException {
    mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE); // Sets the video source used for recording to a Surface
    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // Sets the recorded video file type to MP4 video format

    mediaRecorder.setOutputFile(videoPath.getAbsolutePath()); // Sets the path of the recorded output video file to be the video path we built
    mediaRecorder.setVideoEncodingBitRate(bitRate); // Sets the bitrate for the encoder
    mediaRecorder.setVideoFrameRate(frameRate); // Sets the frame rate for the media
    mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight()); // Sets the video size in pixels
    mediaRecorder.setVideoEncoder(videoCodec); // Defines the video encoding format

    mediaRecorder.prepare(); // Prepares Media Recorder to begin capturing and encoding video data

    try {
      mediaRecorder.start(); // Begins to capture and encode video data to the output file specified
    } catch (IllegalStateException e) {
      Log.e(TAG, "Exception starting capture: " + e.getMessage(), e);
    }
  }

  // Define the video quality of the video including resolution and phone orientation
  public void setVideoQuality(int quality, int orientation) {
    CamcorderProfile profile = null;
    // If the Camcorder Profile has the resolution quality specified then we set it to that
    if (CamcorderProfile.hasProfile(quality)) {
      profile = CamcorderProfile.get(quality);
    }
    if (profile == null) {
      // Else, as a fallback default, select a quality that is available on this device
      for (int level : FALLBACK_QUALITY_LEVELS) {
        if (CamcorderProfile.hasProfile(level)) {
          profile = CamcorderProfile.get(level);
          break;
        }
      }
    }
    // Adjusts the video size accordingly based on the orientation throughout recording
    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
      setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
    } else {
      setVideoSize(profile.videoFrameHeight, profile.videoFrameWidth);
    }
    // Sets the video encoding definition, bitrate, and frame rate for the video.
    setVideoCodec(profile.videoCodec);
    setBitRate(profile.videoBitRate);
    setFrameRate(profile.videoFrameRate);
  }

}
