package com.example.musicplayer2.Activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer2.Fragments.MiniPlayerFragment;
import com.example.musicplayer2.R;
import com.example.musicplayer2.adapters.PlaylistMusicAdapter;
import com.example.musicplayer2.models.Music;
import com.example.musicplayer2.utils.FirebaseUtils;
import com.example.musicplayer2.utils.MediaPlayerManager;

import com.example.musicplayer2.utils.MiniPlayerManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailActivity extends AppCompatActivity implements 
    PlaylistMusicAdapter.OnMusicClickListener, 
    PlaylistMusicAdapter.OnMusicDeleteListener {
    private TextView playlistNameTextView;
    private RecyclerView musicRecyclerView;
    private ProgressBar progressBar;
    private FloatingActionButton addSongsFab;
    private PlaylistMusicAdapter musicAdapter;
    private List<Music> playlistSongs;
    private String playlistId;
    private String playlistName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        playlistId = getIntent().getStringExtra("PLAYLIST_ID");
        playlistName = getIntent().getStringExtra("PLAYLIST_NAME");
        
        if (playlistId == null) {
            finish();
            return;
        }

        initializeViews();
        setupListeners();
        loadPlaylistSongs();
        MiniPlayerManager.getInstance().initializeMiniPlayer(this);
    }

    private void initializeViews() {
        playlistNameTextView = findViewById(R.id.playlistNameTextView);
        musicRecyclerView = findViewById(R.id.musicRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        addSongsFab = findViewById(R.id.addSongsFab);

        playlistNameTextView.setText(playlistName);

        playlistSongs = new ArrayList<>();
        musicAdapter = new PlaylistMusicAdapter(this, playlistSongs, playlistId, this, this);
        musicRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        musicRecyclerView.setAdapter(musicAdapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        addSongsFab.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddSongsToPlaylistActivity.class);
            intent.putExtra("PLAYLIST_ID", playlistId);
            startActivity(intent);
        });
    }

    private void loadPlaylistSongs() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseUtils.getPlaylistSongs(playlistId, task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                playlistSongs.clear();
                playlistSongs.addAll(task.getResult());
                musicAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onMusicClick(Music music) {
        MediaPlayerManager playerManager = MediaPlayerManager.getInstance();
        
        playerManager.setCurrentMusicInfo(
            music.getFileUrl(),
            music.getTitle(),
            music.getArtist(),
            music.getImageUrl()
        );
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playerManager.playMusicFromUrl(this, music.getFileUrl());
        }

        // Use MainActivity's static method to show mini player
        MiniPlayerManager.getInstance().showMiniPlayer(this, music);
    }

    @Override
    public void onDeleteClick(Music music) {
        FirebaseUtils.removeSongFromPlaylist(playlistId, music.getMusicId(), task -> {
            if (task.isSuccessful()) {
                loadPlaylistSongs(); // Reload the playlist
            }
        });
    }

    private Activity getActivity() {
        return this;
    }

    @Override
    protected void onResume() {
        super.onResume();
        MiniPlayerManager.getInstance().updateMiniPlayer(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}