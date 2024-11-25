// MiniPlayerFragment.java
package com.example.musicplayer2.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.example.musicplayer2.Activity.PlayerActivity;
import com.example.musicplayer2.R;
import com.example.musicplayer2.models.Music;
import com.example.musicplayer2.utils.MediaPlayerManager;

public class MiniPlayerFragment extends Fragment {
    private ImageView albumArtImageView;
    private TextView titleTextView, artistTextView;
    private ImageButton playPauseButton;
    private View rootView;
    private MediaPlayerManager mediaPlayerManager;
    private boolean isPlaying = false;
    private RequestManager glideRequestManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Music pendingMusic;

    public static MiniPlayerFragment newInstance() {
        return new MiniPlayerFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mediaPlayerManager = MediaPlayerManager.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_mini_player, container, false);
        initializeViews();
        setupClickListeners();
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getContext() != null) {
            glideRequestManager = Glide.with(getContext().getApplicationContext());
        }

        if (pendingMusic != null) {
            safeUpdateMusicInfo(pendingMusic);
            pendingMusic = null;
        }

        setupMediaPlayerListener();
    }

    private void setupMediaPlayerListener() {
        if (mediaPlayerManager != null) {
            mediaPlayerManager.setOnPreparedListener(() -> {
                isPlaying = true;
                updatePlayPauseButtonState();
            });
        }
    }

    private void initializeViews() {
        albumArtImageView = rootView.findViewById(R.id.mini_album_art);
        titleTextView = rootView.findViewById(R.id.mini_title);
        artistTextView = rootView.findViewById(R.id.mini_artist);
        playPauseButton = rootView.findViewById(R.id.mini_play_pause);
    }

    private void setupClickListeners() {
        if (playPauseButton != null) {
            playPauseButton.setOnClickListener(v -> handlePlayPauseClick());
        }

        if (rootView != null) {
            rootView.setOnClickListener(v -> openPlayerActivity());
        }
    }

    private void handlePlayPauseClick() {
        if (mediaPlayerManager != null) {
            mediaPlayerManager.togglePlayPause();
            isPlaying = mediaPlayerManager.isPlaying();
            updatePlayPauseButtonState();
        }
    }

    private void openPlayerActivity() {
        if (getContext() != null && mediaPlayerManager != null) {
            Intent intent = new Intent(getContext(), PlayerActivity.class);
            intent.putExtra("MUSIC_URL", mediaPlayerManager.getCurrentMusicUrl());
            intent.putExtra("MUSIC_TITLE", mediaPlayerManager.getCurrentMusicTitle());
            intent.putExtra("MUSIC_ARTIST", mediaPlayerManager.getCurrentMusicArtist());
            intent.putExtra("MUSIC_IMAGE", mediaPlayerManager.getCurrentImageUrl());
            startActivity(intent);
        }
    }

    public void safeUpdateMusicInfo(Music music) {
        if (music == null) return;

        if (!isAdded() || getContext() == null) {
            pendingMusic = music;
            return;
        }

        mainHandler.post(() -> {
            try {
                if (isAdded() && getContext() != null) {
                    updateUIElements(music);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void updateUIElements(Music music) {
        if (titleTextView != null) {
            titleTextView.setText(music.getTitle());
        }
        if (artistTextView != null) {
            artistTextView.setText(music.getArtist());
        }

        updateAlbumArt(music);
        updatePlayPauseButtonState();
    }

    private void updateAlbumArt(Music music) {
        if (albumArtImageView != null && glideRequestManager != null && music.getImageUrl() != null) {
            glideRequestManager
                    .load(music.getImageUrl())
                    .placeholder(R.drawable.album_art_background)
                    .into(albumArtImageView);
        }
    }

    private void updatePlayPauseButtonState() {
        mainHandler.post(() -> {
            if (playPauseButton != null && mediaPlayerManager != null && isAdded()) {
                isPlaying = mediaPlayerManager.isPlaying();
                playPauseButton.setImageResource(
                        isPlaying ? R.drawable.ic_pause_mini : R.drawable.ic_play_mini
                );
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePlayPauseButtonState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        glideRequestManager = null;
    }
}