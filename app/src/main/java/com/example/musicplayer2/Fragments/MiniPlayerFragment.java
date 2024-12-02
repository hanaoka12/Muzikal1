package com.example.musicplayer2.Fragments;

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
import com.example.musicplayer2.R;
import com.example.musicplayer2.models.Music;
import com.example.musicplayer2.utils.MediaPlayerManager;

public class MiniPlayerFragment extends Fragment implements MediaPlayerManager.OnPlaybackStateChangeListener {

    private ImageView albumArtImageView;
    private TextView titleTextView, artistTextView;
    private ImageButton playPauseButton;
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_mini_player, container, false);
        initializeViews(rootView);

        // Initialize Glide RequestManager
        glideRequestManager = Glide.with(this);

        setupClickListeners(rootView);
        return rootView;
    }

    private void initializeViews(View rootView) {
        albumArtImageView = rootView.findViewById(R.id.mini_album_art);
        titleTextView = rootView.findViewById(R.id.mini_title);
        artistTextView = rootView.findViewById(R.id.mini_artist);
        playPauseButton = rootView.findViewById(R.id.mini_play_pause);
    }

    private void setupClickListeners(View rootView) {
        if (playPauseButton != null) {
            playPauseButton.setOnClickListener(v -> handlePlayPauseClick());
        }

        if (rootView != null) {
            rootView.setOnClickListener(v -> openPlayerFragment());
        }
    }

    private void handlePlayPauseClick() {
        if (mediaPlayerManager != null) {
            mediaPlayerManager.togglePlayPause();
            // isPlaying will be updated via the listener
        }
    }

    private void openPlayerFragment() {
        if (getActivity() != null && mediaPlayerManager != null) {
            com.example.musicplayer2.Fragment.PlayerFragment playerFragment = new com.example.musicplayer2.Fragment.PlayerFragment();

            
            Bundle args = new Bundle();
            args.putString("MUSIC_ID", mediaPlayerManager.getCurrentMusicId());
            args.putString("MUSIC_URL", mediaPlayerManager.getCurrentMusicUrl());
            args.putString("MUSIC_TITLE", mediaPlayerManager.getCurrentMusicTitle());
            args.putString("MUSIC_ARTIST", mediaPlayerManager.getCurrentMusicArtist());
            args.putString("MUSIC_IMAGE", mediaPlayerManager.getCurrentImageUrl());
            playerFragment.setArguments(args);

            // Show fragment with animation
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            R.animator.slide_in_up,
                            R.animator.slide_out_up,
                            R.animator.slide_in_down,
                            R.animator.slide_out_down
                    )
                    .add(android.R.id.content, playerFragment)
                    .addToBackStack(null)
                    .commit();
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
            if (playPauseButton != null && isAdded()) {
                playPauseButton.setImageResource(
                        isPlaying ? R.drawable.ic_pause_mini : R.drawable.ic_play_mini
                );
            }
        });
    }

    // Implement the playback state change listener method
    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        this.isPlaying = isPlaying;
        updatePlayPauseButtonState();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Register the listener
        if (mediaPlayerManager != null) {
            mediaPlayerManager.addOnPlaybackStateChangeListener(this);
            // Update initial state
            isPlaying = mediaPlayerManager.isPlaying();
            updatePlayPauseButtonState();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // Unregister the listener
        if (mediaPlayerManager != null) {
            mediaPlayerManager.removeOnPlaybackStateChangeListener(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update UI when fragment resumes
        isPlaying = mediaPlayerManager.isPlaying();
        updatePlayPauseButtonState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        glideRequestManager = null;
    }
}