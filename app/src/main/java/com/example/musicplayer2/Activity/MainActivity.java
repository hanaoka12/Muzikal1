package com.example.musicplayer2.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import com.example.musicplayer2.Fragments.HomeFragment;
import com.example.musicplayer2.Fragments.LibraryFragment;
import com.example.musicplayer2.Fragments.ProfileFragment;
import com.example.musicplayer2.R;
import com.example.musicplayer2.models.Music;
import com.example.musicplayer2.utils.MiniPlayerManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private MiniPlayerManager miniPlayerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User is not logged in, redirect to LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Initialize MiniPlayerManager
        miniPlayerManager = MiniPlayerManager.getInstance();
        miniPlayerManager.initializeMiniPlayer(this);

        // Setup bottom navigation
        setupBottomNavigation();

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (item.getItemId() == R.id.nav_library) {
                selectedFragment = new LibraryFragment();
            } else if (item.getItemId() == R.id.nav_upload) {
                startActivity(new Intent(MainActivity.this, UploadMusicActivity.class));
                return true;
            } else if (item.getItemId() == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save any necessary state
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore any necessary state
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (miniPlayerManager != null && !isFinishing()) {
                // Add a slight delay to ensure activity is fully resumed
                new Handler().postDelayed(() -> {
                    if (!isFinishing()) {
                        miniPlayerManager.updateMiniPlayer(this);
                    }
                }, 100);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating mini player: ", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any resources if needed
    }

    public void showMiniPlayer(Music music) {
        try {
            MiniPlayerManager.getInstance().showMiniPlayer(this, music);
        } catch (Exception e) {
            Log.e(TAG, "Error showing mini player: ", e);
        }
    }
}