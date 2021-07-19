package com.fbu.pbluc.artgal.models;

import java.util.Map;

public class User {
  public static final String KEY_USERS = "users";
  public static final String KEY_EMAIL = "email";
  public static final String KEY_FULL_NAME = "name";
  public static final String KEY_FIRST_NAME = "fName";
  public static final String KEY_LAST_NAME = "lName";
  public static final String KEY_USERNAME = "username";
  public static final String KEY_PASSWORD = "password";
  public static final String KEY_CREATED_AT = "createdAt";
  public static final String KEY_UPDATED_AT = "updatedAt";

  private String email;
  private Map<String, Object> name;
  private String username;
  private String password;
  private Object createdAt;
  private Object updatedAt;

  public User() {
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Map<String, Object> getName() {
    return name;
  }

  public void setName(Map<String, Object> name) {
    this.name = name;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
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
