package com.fbu.pbluc.artgal.models;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class Marker {

  public static final String dateTimeFormat = "EEE, MMM dd, yyyy hh:mm aa";

  private static final int SECOND_MILLIS = 1000;
  private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
  private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
  private static final int DAY_MILLIS = 24 * HOUR_MILLIS;

  public static final String KEY_UPLOADED_MARKERS = "uploadedMarkers";
  public static final String KEY_TITLE = "email";
  public static final String KEY_DESCRIPTION = "description";
  public static final String KEY_USER = "user";
  public static final String KEY_MARKER_IMG = "markerImg";
  public static final String KEY_AUGMENTED_OBJ = "augmentedObj";
  public static final String KEY_FILENAME = "fileName";
  public static final String KEY_URI = "uri";
  public static final String KEY_CREATED_AT = "createdAt";
  public static final String KEY_UPDATED_AT = "updatedAt";
  public static final String KEY_LOCATION = "location";
  public static final String KEY_LATITUDE = "latitude";
  public static final String KEY_LONGITUDE = "longitude";
  public static final String KEY_LIKE_COUNT = "likedCount";

  private String title;
  private String description;
  private DocumentReference user;
  private Map<String, Object> markerImg;
  private Map<String, Object> augmentedObj;
  private Map<String, Object> location;
  private Object createdAt;
  private Object updatedAt;
  private Integer likedCount;
  private boolean isSelected = false;

  public Marker() {
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public DocumentReference getUser() {
    return user;
  }

  public void setUser(DocumentReference user) {
    this.user = user;
  }

  public Map<String, Object> getMarkerImg() {
    return markerImg;
  }

  public void setMarkerImg(Map<String, Object> markerImg) {
    this.markerImg = markerImg;
  }

  public Map<String, Object> getAugmentedObj() {
    return augmentedObj;
  }

  public void setAugmentedObj(Map<String, Object> augmentedObj) {
    this.augmentedObj = augmentedObj;
  }

  public Map<String, Object> getLocation() {
    return location;
  }

  public void setLocation(Map<String, Object> location) {
    this.location = location;
  }

  public Object getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Object createdAt) {
    this.createdAt = createdAt;
  }

  public Object getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Object updatedAt) {
    this.updatedAt = updatedAt;
  }

  public boolean isSelected() {
    return isSelected;
  }

  public void setSelected(boolean selected) {
    isSelected = selected;
  }

  public Integer getLikedCount() {
    return likedCount;
  }

  public void setLikedCount(Integer likedCount) {
    this.likedCount = likedCount;
  }

  public void likeMarker() {
    likedCount++;
  }

  public void unlikeMarker() { likedCount--; }

  @NonNull
  @Override
  public String toString() {
    return "{ " + title + ", " + description + ", " + user.toString() + ", " + likedCount + ", " + isSelected + "} ";
  }

  public String formattedCreatedAt() {
    Date createdDate = ((Timestamp) createdAt).toDate();
    DateFormat dateFormat = new SimpleDateFormat(dateTimeFormat, Locale.ENGLISH);
    dateFormat.setLenient(true);
    return dateFormat.format(createdDate);
  }

  public String calculateTimeAgo() {
    Date createdDate = ((Timestamp) createdAt).toDate();

    try {
      createdDate.getTime();
      long time = createdDate.getTime();
      long now = System.currentTimeMillis();

      final long diff = now - time;
      if (diff < MINUTE_MILLIS) {
        return "just now";
      } else if (diff < 2 * MINUTE_MILLIS) {
        return "a minute ago";
      } else if (diff < 50 * MINUTE_MILLIS) {
        return diff / MINUTE_MILLIS + " m";
      } else if (diff < 90 * MINUTE_MILLIS) {
        return "an hour ago";
      } else if (diff < 24 * HOUR_MILLIS) {
        return diff / HOUR_MILLIS + " h";
      } else if (diff < 48 * HOUR_MILLIS) {
        return "yesterday";
      } else {
        return diff / DAY_MILLIS + " d";
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }
}
