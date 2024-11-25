package com.example.musicplayer2.Activity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.musicplayer2.R;
import com.example.musicplayer2.utils.FirebaseUtils;
import com.example.musicplayer2.utils.MediaPlayerManager;
import com.google.firebase.auth.FirebaseAuth;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
public class PlayerActivity extends AppCompatActivity {

    private TextView titleTextView, artistTextView, currentTimeTextView, totalTimeTextView;
    private ImageButton btnPlayPause, btnPrevious, btnNext, btnShuffle, btnRepeat, btnBack;
    private ImageView albumArtImageView;
    private SeekBar seekBar;
    private String musicUrl, musicTitle, musicArtist, imageUrl;
    private MediaPlayerManager mediaPlayerManager;
    private Handler handler;
    private Runnable updateSeekBar;
    private boolean isPlaying = false;
    private boolean isSeekbarTracking = false;
    private String musicId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initializeViews();
        setupClickListeners();
        loadMusicData();
        setupMediaPlayer();
        setupSeekBarUpdate();
    }

    private void initializeViews() {
        titleTextView = findViewById(R.id.musicTitleTextView);
        artistTextView = findViewById(R.id.musicArtistTextView);
        currentTimeTextView = findViewById(R.id.currentTimeTextView);
        totalTimeTextView = findViewById(R.id.totalTimeTextView);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnRepeat = findViewById(R.id.btnRepeat);
        btnBack = findViewById(R.id.btnBack);
        seekBar = findViewById(R.id.seekBar);
        albumArtImageView = findViewById(R.id.albumArtImageView);
    }

    private void setupClickListeners() {
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnBack.setOnClickListener(v -> finish());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updateTimeLabels();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeekbarTracking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeekbarTracking = false;
                mediaPlayerManager.seekTo(seekBar.getProgress());
            }
        });
    }

    private void loadMusicData() {
        musicUrl = getIntent().getStringExtra("MUSIC_URL");
        musicTitle = getIntent().getStringExtra("MUSIC_TITLE");
        musicArtist = getIntent().getStringExtra("MUSIC_ARTIST");
        imageUrl = getIntent().getStringExtra("MUSIC_IMAGE");
        musicId = getIntent().getStringExtra("MUSIC_ID");



        titleTextView.setText(musicTitle);
        artistTextView.setText(musicArtist);

        // Load image using Glide with error handling
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.album_art_background)
                .error(R.drawable.album_art_background)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Log.e("PlayerActivity", "Image load failed: " + e.getMessage());
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d("PlayerActivity", "Image loaded successfully");
                        return false;
                    }
                })
                .into(albumArtImageView);
        } else {
            Log.d("PlayerActivity", "No image URL provided, using default");
            albumArtImageView.setImageResource(R.drawable.album_art_background);
        }
    }

    private void setupMediaPlayer() {
        mediaPlayerManager = MediaPlayerManager.getInstance();
        
        // Only set the listener, don't start playback
        mediaPlayerManager.setOnPreparedListener(() -> {
            isPlaying = mediaPlayerManager.isPlaying();  // Get current state
            updatePlayPauseButton();
            int duration = mediaPlayerManager.getDuration();
            seekBar.setMax(duration);
            updateTimeLabels();
        });

        // Log user interaction
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseUtils.logUserInteraction(userId, musicId);

        // Don't start playback if already playing
        if (!mediaPlayerManager.isCurrentSong(musicUrl)) {
            mediaPlayerManager.playMusicFromUrl(this, musicUrl);
        } else {
            // Just update UI for current playback
            isPlaying = mediaPlayerManager.isPlaying();
            updatePlayPauseButton();
            seekBar.setMax(mediaPlayerManager.getDuration());
            updateTimeLabels();
        }
    }

    private void setupSeekBarUpdate() {
        handler = new Handler();
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayerManager != null && !isSeekbarTracking) {
                    int currentPosition = mediaPlayerManager.getCurrentPosition();
                    int duration = mediaPlayerManager.getDuration();
                    
                    if (duration > 0) {
                        seekBar.setMax(duration);
                        seekBar.setProgress(currentPosition);
                        updateTimeLabels();
                    }
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateSeekBar);
    }

    private void togglePlayPause() {
        if (isPlaying) {
            mediaPlayerManager.pauseMusic();
        } else {
            mediaPlayerManager.resumeMusic();
        }
        isPlaying = !isPlaying;
        updatePlayPauseButton();
    }

    private void updatePlayPauseButton() {
        btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private void updateTimeLabels() {
        int currentPosition = isSeekbarTracking ? 
            seekBar.getProgress() : mediaPlayerManager.getCurrentPosition();
        int duration = mediaPlayerManager.getDuration();

        currentTimeTextView.setText(formatTime(currentPosition));
        totalTimeTextView.setText(formatTime(duration));
    }

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(updateSeekBar);
        }
        // Don't release player here since it's managed by service
        // if (mediaPlayerManager != null) {
        //     mediaPlayerManager.releasePlayer();
        // }
    }
}

