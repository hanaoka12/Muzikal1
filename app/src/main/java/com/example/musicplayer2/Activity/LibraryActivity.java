package com.example.musicplayer2.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer2.R;
import com.example.musicplayer2.adapters.MusicAdapter;
import com.example.musicplayer2.models.Music;
import com.example.musicplayer2.utils.FirebaseUtils;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class LibraryActivity extends AppCompatActivity implements MusicAdapter.OnMusicClickListener {

    private RecyclerView musicRecyclerView;
    private ProgressBar progressBar;
    private MusicAdapter musicAdapter;
    private List<Music> musicList;

    private static final String TAG = "LibraryActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        musicRecyclerView = findViewById(R.id.musicRecyclerView);
        progressBar = findViewById(R.id.libraryProgressBar);

        musicRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        musicList = new ArrayList<>();
        musicAdapter = new MusicAdapter(this, musicList, this);
        musicRecyclerView.setAdapter(musicAdapter);

        loadMusic();
    }

    private void loadMusic() {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseUtils.getMusicCollection().get().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);

            if (task.isSuccessful() && task.getResult() != null) {
                handleMusicLoadSuccess(task);
            } else {
                handleMusicLoadFailure(task);
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Log.e(TAG, "Error loading music: ", e);
            Toast.makeText(LibraryActivity.this, "Failed to load music library", Toast.LENGTH_SHORT).show();
        });
    }

    private void handleMusicLoadSuccess(Task<QuerySnapshot> task) {
        musicList.clear();
        for (QueryDocumentSnapshot document : task.getResult()) {
            try {
                Music music = document.toObject(Music.class);
                if (music != null && music.getFileUrl() != null) {
                    musicList.add(music);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing music data: ", e);
            }
        }
        musicAdapter.notifyDataSetChanged();

        if (musicList.isEmpty()) {
            Toast.makeText(LibraryActivity.this, "No music available in the library", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleMusicLoadFailure(Task<QuerySnapshot> task) {
        Exception e = task.getException();
        if (e != null) {
            Log.e(TAG, "Music load failed: ", e);
        }
        Toast.makeText(LibraryActivity.this, "Failed to load music library", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMusicClick(int position) {
        if (position >= 0 && position < musicList.size()) {
            Music selectedMusic = musicList.get(position);  // Get the selected music object
            
            Log.d(TAG, "Image URL being passed: " + selectedMusic.getImageUrl());
            
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra("MUSIC_URL", selectedMusic.getFileUrl());
            intent.putExtra("MUSIC_TITLE", selectedMusic.getTitle());
            intent.putExtra("MUSIC_ARTIST", selectedMusic.getArtist());
            intent.putExtra("MUSIC_IMAGE", selectedMusic.getImageUrl());  // Add image URL
            
            startActivity(intent);
        } else {
            Toast.makeText(this, "Invalid music selection", Toast.LENGTH_SHORT).show();
        }
    }
}
