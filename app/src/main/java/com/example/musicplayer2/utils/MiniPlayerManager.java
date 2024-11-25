// MiniPlayerManager.java
package com.example.musicplayer2.utils;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.musicplayer2.Fragments.MiniPlayerFragment;
import com.example.musicplayer2.R;
import com.example.musicplayer2.models.Music;

public class MiniPlayerManager {
    private static MiniPlayerManager instance;
    private Music currentMusic;
    private boolean isVisible = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String currentFragmentTag = null;
    private static final String BASE_TAG = "mini_player_";

    private MiniPlayerManager() {}

    public static synchronized MiniPlayerManager getInstance() {
        if (instance == null) {
            instance = new MiniPlayerManager();
        }
        return instance;
    }

    private String getFragmentTag(AppCompatActivity activity) {
        return BASE_TAG + activity.getClass().getSimpleName();
    }

    private void safelyUpdateFragment(AppCompatActivity activity, Music music, boolean forceCreate) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        try {
            FragmentManager fragmentManager = activity.getSupportFragmentManager();
            String tag = getFragmentTag(activity);

            // Wait for any pending transactions
            fragmentManager.executePendingTransactions();

            Fragment existingFragment = fragmentManager.findFragmentByTag(tag);

            if (existingFragment instanceof MiniPlayerFragment && !forceCreate) {
                if (existingFragment.isAdded()) {
                    ((MiniPlayerFragment) existingFragment).safeUpdateMusicInfo(music);
                }
            } else {
                MiniPlayerFragment newFragment = MiniPlayerFragment.newInstance();
                FragmentTransaction ft = fragmentManager.beginTransaction();
                ft.replace(R.id.mini_player_container, newFragment, tag);
                ft.commitNowAllowingStateLoss();

                // Update after commit
                if (music != null) {
                    newFragment.safeUpdateMusicInfo(music);
                }
            }

            View container = activity.findViewById(R.id.mini_player_container);
            if (container != null) {
                container.setVisibility(View.VISIBLE);
            }

            currentFragmentTag = tag;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showMiniPlayer(AppCompatActivity activity, Music music) {
        if (activity == null || music == null) return;

        mainHandler.post(() -> {
            currentMusic = music;
            isVisible = true;
            safelyUpdateFragment(activity, music, false);
        });
    }

    public void updateMiniPlayer(AppCompatActivity activity) {
        if (activity == null) return;

        mainHandler.post(() -> {
            try {
                MediaPlayerManager playerManager = MediaPlayerManager.getInstance();
                if (playerManager != null && playerManager.getCurrentMusicUrl() != null) {
                    Music music = new Music(
                            null,
                            playerManager.getCurrentMusicTitle(),
                            playerManager.getCurrentMusicArtist(),
                            playerManager.getCurrentMusicUrl(),
                            null,
                            playerManager.getCurrentImageUrl(),
                            null
                    );
                    currentMusic = music;
                    isVisible = true;
                    safelyUpdateFragment(activity, music, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void initializeMiniPlayer(AppCompatActivity activity) {
        if (activity == null) return;

        mainHandler.post(() -> {
            if (currentMusic != null && isVisible) {
                safelyUpdateFragment(activity, currentMusic, true);
            }
        });
    }

    public Music getCurrentMusic() {
        return currentMusic;
    }

    public boolean isVisible() {
        return isVisible;
    }
}