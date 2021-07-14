package com.fbu.pbluc.artgal.models;

import android.net.Uri;

import com.google.firebase.firestore.DocumentReference;

import java.util.Map;

public class Marker {

    private String title;
    private String description;
    private DocumentReference user;
    private Map<String, Object> markerImg;
    private Map<String, Object> augmentedObj;
    private Object createdAt;
    private Object updatedAt;

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
}
