package com.example.musicplayer2.Fragments;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer2.Activity.MainActivity;
import com.example.musicplayer2.Activity.PlayerActivity;
import com.example.musicplayer2.R;
import com.example.musicplayer2.adapters.MusicAdapter;
import com.example.musicplayer2.adapters.SliderAdapter;
import com.example.musicplayer2.models.Music;
import com.example.musicplayer2.utils.FirebaseUtils;
import com.example.musicplayer2.utils.MediaPlayerManager;
import com.example.musicplayer2.utils.MiniPlayerManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class HomeFragment extends Fragment implements SliderAdapter.OnMusicClickListener {

    private ProgressBar progressBar;

    // Trending section
    private RecyclerView trendingRecyclerView;
    private SliderAdapter trendingAdapter;
    private List<Music> trendingMusicList = new ArrayList<>();

    // Pop section
    private RecyclerView popRecyclerView;
    private SliderAdapter popAdapter;
    private List<Music> popMusicList = new ArrayList<>();

    // Rock section
    private RecyclerView rockRecyclerView;
    private SliderAdapter rockAdapter;
    private List<Music> rockMusicList = new ArrayList<>();

    // Jazz section
    private RecyclerView jazzRecyclerView;
    private SliderAdapter jazzAdapter;
    private List<Music> jazzMusicList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        initializeViews(rootView);
        setupRecyclerViews();
        setupAdapters();
        loadData();

        return rootView;
    }

    private void initializeViews(View rootView) {
        progressBar = rootView.findViewById(R.id.homeProgressBar);
        trendingRecyclerView = rootView.findViewById(R.id.trendingRecyclerView);
        popRecyclerView = rootView.findViewById(R.id.popRecyclerView);
        rockRecyclerView = rootView.findViewById(R.id.rockRecyclerView);
        jazzRecyclerView = rootView.findViewById(R.id.jazzRecyclerView);
    }

    private void setupRecyclerViews() {
        setupHorizontalRecyclerView(trendingRecyclerView);
        setupHorizontalRecyclerView(popRecyclerView);
        setupHorizontalRecyclerView(rockRecyclerView);
        setupHorizontalRecyclerView(jazzRecyclerView);
    }

    private void setupHorizontalRecyclerView(RecyclerView recyclerView) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
    }

    private void setupAdapters() {
        trendingAdapter = new SliderAdapter(getContext(), trendingMusicList, this);
        popAdapter = new SliderAdapter(getContext(), popMusicList, this);
        rockAdapter = new SliderAdapter(getContext(), rockMusicList, this);
        jazzAdapter = new SliderAdapter(getContext(), jazzMusicList, this);

        trendingRecyclerView.setAdapter(trendingAdapter);
        popRecyclerView.setAdapter(popAdapter);
        rockRecyclerView.setAdapter(rockAdapter);
        jazzRecyclerView.setAdapter(jazzAdapter);
    }

    private void loadData() {
        loadTrendingSongs();
        loadGenreSongs("Pop", popMusicList, popAdapter);
        loadGenreSongs("Rock", rockMusicList, rockAdapter);
        loadGenreSongs("Jazz", jazzMusicList, jazzAdapter);
    }

    private void loadTrendingSongs() {
        FirebaseUtils.getMusicCollection().get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Music> allMusicList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Music music = document.toObject(Music.class);
                            if (music != null && music.getFileUrl() != null) {
                                allMusicList.add(music);
                            }
                        }
                        if (!allMusicList.isEmpty()) {
                            Collections.shuffle(allMusicList);
                            List<Music> randomSongs = allMusicList.stream().limit(5).collect(Collectors.toList());
                            trendingMusicList.clear();
                            trendingMusicList.addAll(randomSongs);
                            trendingAdapter.notifyDataSetChanged();
                        }
                    } else {
                        Log.e("HomeFragment", "Error getting documents: ", task.getException());
                    }
                });
    }

    private void loadGenreSongs(String genre, List<Music> genreMusicList, SliderAdapter adapter) {
        FirebaseUtils.getMusicCollection()
                .whereEqualTo("genre", genre)
                .limit(5)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        genreMusicList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Music music = document.toObject(Music.class);
                            if (music != null && music.getFileUrl() != null) {
                                genreMusicList.add(music);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.e("HomeFragment", "Error getting " + genre + " songs: ", task.getException());
                    }
                });
    }

    private void loadRecommendedSongs() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseUtils.getUserRecommendations(userId, task -> {
            if (task.isSuccessful()) {
                List<Music> recommendedSongs = task.getResult();
                if (recommendedSongs != null && !recommendedSongs.isEmpty()) {
                    trendingMusicList.clear();
                    trendingMusicList.addAll(recommendedSongs);
                    trendingAdapter.notifyDataSetChanged();
                } else {
                    // No recommendations, load default trending songs
                    loadTrendingSongs();
                }
            } else {
                // Handle error
                Log.e("HomeFragment", "Error fetching recommendations: ", task.getException());
                // Optionally load default trending songs
                loadTrendingSongs();
            }
        });
    }

    @Override
    public void onMusicClick(Music music) {
        // Handle music click
        MediaPlayerManager playerManager = MediaPlayerManager.getInstance();

        playerManager.setCurrentMusicInfo(
                music.getFileUrl(),
                music.getTitle(),
                music.getArtist(),
                music.getImageUrl()
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playerManager.playMusicFromUrl(requireContext(), music.getFileUrl());
        }

        // Show mini player
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            MiniPlayerManager.getInstance().showMiniPlayer(activity, music);
        }

        
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            // Update mini player state
            MiniPlayerManager.getInstance().updateMiniPlayer(activity);
        }
    }
}
