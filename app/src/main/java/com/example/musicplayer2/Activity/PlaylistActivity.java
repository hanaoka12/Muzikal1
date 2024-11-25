package com.example.musicplayer2.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer2.Fragments.MiniPlayerFragment;
import com.example.musicplayer2.R;
import com.example.musicplayer2.adapters.PlaylistAdapter;
import com.example.musicplayer2.models.Music;
import com.example.musicplayer2.models.Playlist;
import com.example.musicplayer2.dialogs.CreatePlaylistDialog;
import com.example.musicplayer2.utils.MediaPlayerManager;
import com.example.musicplayer2.utils.MiniPlayerManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class PlaylistActivity extends AppCompatActivity implements PlaylistAdapter.OnPlaylistClickListener {
    private RecyclerView playlistsRecyclerView;
    private FloatingActionButton createPlaylistFab;
    private ProgressBar progressBar;
    private PlaylistAdapter playlistAdapter;
    private List<Playlist> playlists;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        initializeViews();
        setupRecyclerView();
        setupListeners();
        loadPlaylists();
        MiniPlayerManager.getInstance().initializeMiniPlayer(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPlaylists(); // Reload playlists when returning
        MiniPlayerManager.getInstance().updateMiniPlayer(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove fragment detachment code
    }

    private void initializeViews() {
        playlistsRecyclerView = findViewById(R.id.playlistsRecyclerView);
        createPlaylistFab = findViewById(R.id.createPlaylistFab);
        progressBar = findViewById(R.id.progressBar);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void setupRecyclerView() {
        playlists = new ArrayList<>();
        // Pass 'this' as the listener
        playlistAdapter = new PlaylistAdapter(this, playlists, this);
        playlistsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        playlistsRecyclerView.setAdapter(playlistAdapter);
    }

    private void setupListeners() {
        createPlaylistFab.setOnClickListener(v -> showCreatePlaylistDialog());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void loadPlaylists() {
        progressBar.setVisibility(View.VISIBLE);
        String userId = mAuth.getCurrentUser().getUid();
        
        db.collection("playlists")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                playlists.clear();
                for (com.google.firebase.firestore.DocumentSnapshot document : queryDocumentSnapshots) {
                    Playlist playlist = document.toObject(Playlist.class);
                    if (playlist != null) {
                        playlists.add(playlist);
                    }
                }
                playlistAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                // Handle error
            });
    }

    private void showCreatePlaylistDialog() {
        CreatePlaylistDialog dialog = new CreatePlaylistDialog(this, () -> {
            // Reload playlists after creation
            loadPlaylists();
        });
        dialog.show();
    }

    @Override
    public void onPlaylistClick(Playlist playlist) {
        try {
            Log.d("PlaylistActivity", "Starting PlaylistDetailActivity with ID: " 
                + playlist.getPlaylistId() + " Name: " + playlist.getName());
            
            Intent intent = new Intent(this, PlaylistDetailActivity.class);
            intent.putExtra("PLAYLIST_ID", playlist.getPlaylistId());
            intent.putExtra("PLAYLIST_NAME", playlist.getName());
            startActivity(intent);
        } catch (Exception e) {
            Log.e("PlaylistActivity", "Error starting PlaylistDetailActivity", e);
            Toast.makeText(this, "Error opening playlist", Toast.LENGTH_SHORT).show();
        }
    }
}
