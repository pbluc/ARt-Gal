package com.fbu.pbluc.artgal.fragments;

import com.fbu.pbluc.artgal.ArViewActivity;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;

public class CustomArFragment extends ArFragment {

  private static final String TAG = "CustomArFragment";



  @Override
  protected Config getSessionConfiguration(Session session) {

    Config config = new Config(session);
    config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
    config.setFocusMode(Config.FocusMode.AUTO);

    session.configure(config);

    this.getArSceneView().setupSession(session);

    ((ArViewActivity) getActivity()).setUpImageDatabase(session);

    return config;
  }
}

