package com.example.musicplayer2.Fragments;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer2.Activity.MainActivity;
import com.example.musicplayer2.Activity.PlayerActivity;
import com.example.musicplayer2.R;
import com.example.musicplayer2.adapters.GenreAdapter;
import com.example.musicplayer2.adapters.MusicAdapter;
import com.example.musicplayer2.models.Music;
import com.example.musicplayer2.utils.FirebaseUtils;
import com.example.musicplayer2.utils.MediaPlayerManager;
import com.example.musicplayer2.utils.MiniPlayerManager;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LibraryFragment extends Fragment implements 
    MusicAdapter.OnMusicClickListener, 
    GenreAdapter.OnGenreClickListener {

    private EditText searchEditText;
    private RecyclerView genreRecyclerView;
    private RecyclerView musicRecyclerView;
    private ProgressBar progressBar;
    private MusicAdapter musicAdapter;
    private GenreAdapter genreAdapter;
    private List<Music> musicList = new ArrayList<>();
    private List<String> genres = Arrays.asList("Pop", "Rock", "Jazz");

    private static final String TAG = "LibraryFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);
        
        initializeViews(view);
        setupRecyclerViews();
        setupSearch();
        loadMusic();
        
        return view;
    }

    private void initializeViews(View view) {
        searchEditText = view.findViewById(R.id.searchEditText);
        genreRecyclerView = view.findViewById(R.id.genreRecyclerView);
        musicRecyclerView = view.findViewById(R.id.musicRecyclerView);
        progressBar = view.findViewById(R.id.libraryProgressBar);
    }

    // Update setupRecyclerViews() in LibraryFragment
    private void setupRecyclerViews() {
        // Setup genre RecyclerView
        int spanCount = 2; // Change to 2 columns for better spacing
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
        genreRecyclerView.setLayoutManager(layoutManager);
        
        // Set padding on the RecyclerView instead of individual items
        int padding = getResources().getDimensionPixelSize(R.dimen.genre_grid_spacing);
        genreRecyclerView.setPadding(padding, padding, padding, padding);
        genreRecyclerView.setClipToPadding(false);
        
        genreAdapter = new GenreAdapter(getContext(), genres, this);
        genreRecyclerView.setAdapter(genreAdapter);

        // Setup music RecyclerView
        musicRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        musicAdapter = new MusicAdapter(getContext(), musicList, this);
        musicRecyclerView.setAdapter(musicAdapter);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            private Handler handler = new Handler();
            private Runnable searchRunnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> filterMusic(s.toString().trim());
                handler.postDelayed(searchRunnable, 300); // Debounce delay
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterMusic(String query) {
        if (query.isEmpty()) {
            loadMusic();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        FirebaseUtils.searchSongs(query, task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                musicList.clear();
                musicList.addAll(task.getResult());
                musicAdapter.notifyDataSetChanged();
            } else {
                Log.e("LibraryFragment", "Error searching music: ", task.getException());
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to search music", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadMusic() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseUtils.getMusicCollection()
            .get()
            .addOnCompleteListener(task -> {
                if (isAdded()) {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful() && task.getResult() != null) {
                        musicList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Music music = document.toObject(Music.class);
                            if (music != null && music.getFileUrl() != null) {
                                musicList.add(music);
                            }
                        }
                        musicAdapter.notifyDataSetChanged();
                    } else {
                        Log.e("LibraryFragment", "Error loading music: ", task.getException());
                        Toast.makeText(getContext(), "Failed to load music", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMusicClick(int position) {
        if (position < 0 || position >= musicList.size() || !isAdded()) return;

        try {
            Music selectedMusic = musicList.get(position);
            MediaPlayerManager playerManager = MediaPlayerManager.getInstance();

            // Set music info and play
            playerManager.setCurrentMusicInfo(
                    selectedMusic.getFileUrl(),
                    selectedMusic.getTitle(),
                    selectedMusic.getArtist(),
                    selectedMusic.getImageUrl()
            );

            // Play music
            playerManager.playMusicFromUrl(requireContext(), selectedMusic.getFileUrl());

            // Show mini player
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                // Use MiniPlayerManager directly instead of MainActivity's method
                MiniPlayerManager.getInstance().showMiniPlayer(activity, selectedMusic);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing music: ", e);
            if (isAdded()) {
                Toast.makeText(requireContext(), "Error playing music", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onGenreClick(String genre) {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseUtils.getMusicCollection()
            .whereEqualTo("genre", genre)
            .get()
            .addOnSuccessListener(documents -> {
                musicList.clear();
                for (QueryDocumentSnapshot document : documents) {
                    Music music = document.toObject(Music.class);
                    musicList.add(music);
                }
                musicAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            });
    }

    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            // Update mini player state
            MiniPlayerManager.getInstance().updateMiniPlayer(activity);
        }
    }
}