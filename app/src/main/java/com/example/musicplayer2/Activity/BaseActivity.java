package com.example.musicplayer2.Activity;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import com.example.musicplayer2.Fragments.MiniPlayerFragment;
import com.example.musicplayer2.R;
import com.example.musicplayer2.models.Music;
import com.example.musicplayer2.utils.MediaPlayerManager;

public abstract class BaseActivity extends AppCompatActivity {
    protected MiniPlayerFragment miniPlayerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Remove these lines as they're causing layout issues
        // EdgeToEdge.enable(this);
        // setContentView(R.layout.activity_base);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupMiniPlayer();
    }

    protected void setupMiniPlayer() {
        View miniPlayerContainer = findViewById(R.id.mini_player_container);
        if (miniPlayerContainer != null) {
            miniPlayerFragment = (MiniPlayerFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mini_player_container);
                
            if (miniPlayerFragment == null) {
                miniPlayerFragment = new MiniPlayerFragment();
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.mini_player_container, miniPlayerFragment)
                    .commit();
            }

            // Update visibility based on playback state
            MediaPlayerManager playerManager = MediaPlayerManager.getInstance();
            if (playerManager.getCurrentMusicUrl() != null) {
                miniPlayerContainer.setVisibility(View.VISIBLE);
                showMiniPlayer(new Music(
                    null,
                    playerManager.getCurrentMusicTitle(),
                    playerManager.getCurrentMusicArtist(),
                    playerManager.getCurrentMusicUrl(),
                    null,
                    playerManager.getCurrentImageUrl(),
                    null
                ));
            }
        }
    }

    public void showMiniPlayer(Music music) {
        View miniPlayerContainer = findViewById(R.id.mini_player_container);
        if (miniPlayerContainer != null) {
            miniPlayerContainer.setVisibility(View.VISIBLE);
            if (miniPlayerFragment != null) {

            }
        }
    }
}