package com.example.musicplayer2.models;

import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private String playlistId;
    private String name;
    private String userId;
    private List<String> musicIds;
    private String imageUrl;
    private long createdAt;

    public Playlist() {
        // Required empty constructor for Firestore
        musicIds = new ArrayList<>();
    }

    public Playlist(String playlistId, String name, String userId) {
        this.playlistId = playlistId;
        this.name = name;
        this.userId = userId;
        this.musicIds = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getPlaylistId() { return playlistId; }
    public void setPlaylistId(String playlistId) { this.playlistId = playlistId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public List<String> getMusicIds() { return musicIds; }
    public void setMusicIds(List<String> musicIds) { this.musicIds = musicIds; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
