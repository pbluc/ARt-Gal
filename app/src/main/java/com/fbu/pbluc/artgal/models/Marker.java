package com.fbu.pbluc.artgal.models;

import com.google.firebase.firestore.DocumentReference;

public class Marker {

    private String title;
    private String description;
    private DocumentReference user;
    private String markerImg;
    private String augmentedObj;
    private Object createdAt;
    private Object updatedAt;

    public Marker() {
    }

    public Marker(String title, String description, DocumentReference user, String markerImg, String augmentedObj, Object createdAt, Object updatedAt) {
        this.title = title;
        this.description = description;
        this.user = user;
        this.markerImg = markerImg;
        this.augmentedObj = augmentedObj;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getMarkerImg() {
        return markerImg;
    }

    public void setMarkerImg(String markerImg) {
        this.markerImg = markerImg;
    }

    public String getAugmentedObj() {
        return augmentedObj;
    }

    public void setAugmentedObj(String augmentedObj) {
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
