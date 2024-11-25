package com.example.musicplayer2.utils;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.musicplayer2.Service.MusicService;

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
    private Context context;
    private Intent serviceIntent;

    public interface OnPreparedListener {
        void onPrepared();
    }

    // Private constructor
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
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
            return false;
        });
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
            shouldAutoPlay = true;  // Make sure this is true

            // Start service
            if (this.context != null && serviceIntent != null) {
                this.context.startForegroundService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing music", e);
        }
    }

    public void setCurrentMusicInfo(String url, String title, String artist, String imageUrl) {
        this.currentMusicUrl = url;
        this.currentMusicTitle = title;
        this.currentMusicArtist = artist;
        this.currentImageUrl = imageUrl;
        this.shouldAutoPlay = true;  // Important: Set this to true
    }

    public String getCurrentMusicUrl() { return currentMusicUrl; }
    public String getCurrentMusicTitle() { return currentMusicTitle; }
    public String getCurrentMusicArtist() { return currentMusicArtist; }
    public String getCurrentImageUrl() { return currentImageUrl; }

    public void pauseMusic() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void resumeMusic() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
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
}