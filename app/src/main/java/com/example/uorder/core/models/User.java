package com.example.uorder.core.models;

import com.google.firebase.Timestamp;

public class User {
    private String userName;
    private String userEmail;
    private String userRole; // customer or business
    private Timestamp createdAt;

    public User() {
        // Default constructor for Firebase
    }

    public User(String userName, String userEmail, String userRole) {
        this.userName = userName;
        this.userEmail = userEmail;
        this.userRole = userRole;
        this.createdAt = Timestamp.now();
    }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
