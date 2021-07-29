package com.fbu.pbluc.artgal.adapters;

import android.view.LayoutInflater;
import android.view.View;

import com.fbu.pbluc.artgal.R;
import com.google.android.gms.maps.GoogleMap;
import com.fbu.pbluc.artgal.models.Marker;

public class CustomWindowAdapter implements GoogleMap.InfoWindowAdapter {

  private LayoutInflater mInflater;

  public CustomWindowAdapter(LayoutInflater mInflater) {
    this.mInflater = mInflater;
  }

  // This changes the frame of the info window; returning null uses the default frame.
  // This is just the border and arrow surrounding the contents specified below
  @Override
  public View getInfoWindow(com.google.android.gms.maps.model.Marker marker) {
    return null;
  }

  // This defines the contents within the info window based on the marker
  @Override
  public View getInfoContents(com.google.android.gms.maps.model.Marker marker) {
    // Retrieve Marker model attached
    Marker attachedMarker = (Marker) marker.getTag();
    // Getting view from the layout file
    View v = mInflater.inflate(R.layout.custom_info_window, null);
    // TODO: Populate fields
    // Return info window contents
    return v;
  }
}
