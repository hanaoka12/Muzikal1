package com.example.musicplayer2.utils;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.musicplayer2.Service.MusicService;
import com.example.musicplayer2.models.Music;

import java.util.ArrayList;
import java.util.List;

public class MediaPlayerManager {
    private static final String TAG = "MediaPlayerManager";
    private static MediaPlayerManager instance;
    private MediaPlayer mediaPlayer;
    private OnPreparedListener onPreparedListener;
    private boolean isPrepared = false;
    private boolean shouldAutoPlay = true;  // Add this field
    private String currentMusicUrl;
    private String currentMusicTitle;
    private String currentMusicArtist;
    private String currentImageUrl;
    private String currentMusicId; 
    private Context context;
    private Intent serviceIntent;
    private boolean isRepeatEnabled = false;

    //  Playback State Change Listener Interface
    public interface OnPlaybackStateChangeListener {
        void onPlaybackStateChanged(boolean isPlaying);
    }

    // List to hold listeners
    private final List<OnPlaybackStateChangeListener> playbackStateChangeListeners = new ArrayList<>();

    public interface OnPreparedListener {
        void onPrepared();
    }

    
    private MediaPlayerManager() {
        this.mediaPlayer = new MediaPlayer();
        setupMediaPlayer();
    }

    public static synchronized MediaPlayerManager getInstance() {
        if (instance == null) {
            instance = new MediaPlayerManager();
        }
        return instance;
    }

    private void setupMediaPlayer() {
        mediaPlayer.setOnPreparedListener(mp -> {
            isPrepared = true;
            if (shouldAutoPlay) {
                mp.start();
                Log.d(TAG, "MediaPlayer started playing");
            }
            if (onPreparedListener != null) {
                onPreparedListener.onPrepared();
            }
            notifyPlaybackStateChange(true);
        });

        mediaPlayer.setOnCompletionListener(mp -> {
            if (!isRepeatEnabled) {
                notifyPlaybackStateChange(false);
            }
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
            return false;
        });
    }

    // Method to add a listener
    public void addOnPlaybackStateChangeListener(OnPlaybackStateChangeListener listener) {
        if (!playbackStateChangeListeners.contains(listener)) {
            playbackStateChangeListeners.add(listener);
        }
    }

    // Method to remove a listener
    public void removeOnPlaybackStateChangeListener(OnPlaybackStateChangeListener listener) {
        playbackStateChangeListeners.remove(listener);
    }

    // Notify all listeners about playback state change
    private void notifyPlaybackStateChange(boolean isPlaying) {
        for (OnPlaybackStateChangeListener listener : playbackStateChangeListeners) {
            listener.onPlaybackStateChanged(isPlaying);
        }
    }

    public void setOnPreparedListener(OnPreparedListener listener) {
        this.onPreparedListener = listener;
    }

    public void initService(Context context) {
        this.context = context.getApplicationContext();
        serviceIntent = new Intent(context, MusicService.class);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void playMusicFromUrl(Context context, String fileUrl) {
        try {
            Log.d(TAG, "Playing music from URL: " + fileUrl);
            isPrepared = false;
            mediaPlayer.reset();
            mediaPlayer.setDataSource(context, Uri.parse(fileUrl));
            mediaPlayer.prepareAsync();
            currentMusicUrl = fileUrl;
            shouldAutoPlay = true;  

            
            if (this.context != null && serviceIntent != null) {
                this.context.startForegroundService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing music", e);
        }
    }

    public void setCurrentMusicInfo(String currentMusicID,String url, String title, String artist, String imageUrl) {
        this.currentMusicId = currentMusicID;
        this.currentMusicUrl = url;
        this.currentMusicTitle = title;
        this.currentMusicArtist = artist;
        this.currentImageUrl = imageUrl;
        this.shouldAutoPlay = true;  
    }

    
    public void setMusic(Music music) {
        if (music != null) {
            currentMusicId = music.getMusicId(); 
            
        }
    }

    
    public String getCurrentMusicId() {
        return currentMusicId;
    }

    public String getCurrentMusicUrl() { return currentMusicUrl; }
    public String getCurrentMusicTitle() { return currentMusicTitle; }
    public String getCurrentMusicArtist() { return currentMusicArtist; }
    public String getCurrentImageUrl() { return currentImageUrl; }

    public void pauseMusic() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            notifyPlaybackStateChange(false);
        }
    }

    public void resumeMusic() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            notifyPlaybackStateChange(true);
        }
    }

    public void stopMusic() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
    }

    public void releasePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            notifyPlaybackStateChange(false);
        }
        if (context != null) {
            context.stopService(serviceIntent);
        }
    }

    public void seekTo(int position) {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(position);
        }
    }

    public int getCurrentPosition() {
        return (mediaPlayer != null && isPrepared) ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return (mediaPlayer != null && isPrepared) ? mediaPlayer.getDuration() : 0;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void togglePlayPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                pauseMusic();
            } else {
                resumeMusic();
            }
        }
    }

    public boolean isCurrentSong(String url) {
        return url != null && url.equals(currentMusicUrl);
    }

    public boolean isPrepared() {
        return isPrepared;
    }

    public void setRepeat(boolean repeat) {
        isRepeatEnabled = repeat;
        // Set MediaPlayer's looping state
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(repeat);
        }
    }

    public boolean isRepeatEnabled() {
        return isRepeatEnabled;
    }
}