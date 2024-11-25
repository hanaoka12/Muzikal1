package com.example.musicplayer2.Activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicplayer2.R;
import com.example.musicplayer2.adapters.SearchMusicAdapter;
import com.example.musicplayer2.models.Music;
import com.example.musicplayer2.utils.FirebaseUtils;
import java.util.ArrayList;
import java.util.List;

public class AddSongsToPlaylistActivity extends AppCompatActivity {
    private EditText searchEditText;
    private RecyclerView searchResultsRecyclerView;
    private ProgressBar progressBar;
    private SearchMusicAdapter adapter;
    private List<Music> searchResults;
    private String playlistId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_songs_to_playlist);

        playlistId = getIntent().getStringExtra("PLAYLIST_ID");
        if (playlistId == null) {
            finish();
            return;
        }

        initializeViews();
        setupSearchListener();
    }

    private void initializeViews() {
        searchEditText = findViewById(R.id.searchEditText);
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        searchResults = new ArrayList<>();
        adapter = new SearchMusicAdapter(this, searchResults, this::addSongToPlaylist);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecyclerView.setAdapter(adapter);
    }

    private void setupSearchListener() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (!query.isEmpty()) {
                    searchSongs(query);
                } else {
                    searchResults.clear();
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchSongs(String query) {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseUtils.searchSongs(query, task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful() && task.getResult() != null) {
                searchResults.clear();
                searchResults.addAll(task.getResult());
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void addSongToPlaylist(Music song) {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseUtils.addSongToPlaylist(playlistId, song.getMusicId(), task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                finish(); // Return to playlist detail
            }
        });
    }
}