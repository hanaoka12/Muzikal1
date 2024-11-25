package com.example.musicplayer2.models;


import java.util.List;

public class User {
    private String userId;
    private String email;
    private List<String> playlistIds; // List of playlist IDs the user has created
    private List<String> uploadedSongs; // List of song IDs the user has uploaded
    private String name;
    private String imageUrl;
    public User() {
        // Empty constructor for Firebase
    }

    public User(String userId, String email, List<String> playlistIds, List<String> uploadedSongs) {
        this.userId = userId;
        this.email = email;
        this.playlistIds = playlistIds;
        this.uploadedSongs = uploadedSongs;
        this.name = name;
        this.imageUrl = imageUrl;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getPlaylistIds() {
        return playlistIds;
    }

    public void setPlaylistIds(List<String> playlistIds) {
        this.playlistIds = playlistIds;
    }

    public List<String> getUploadedSongs() {
        return uploadedSongs;
    }

    public void setUploadedSongs(List<String> uploadedSongs) {
        this.uploadedSongs = uploadedSongs;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}

