package com.example.musicplayer2.Fragment;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.musicplayer2.R;
import com.example.musicplayer2.utils.FirebaseUtils;
import com.example.musicplayer2.utils.MediaPlayerManager;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import com.example.musicplayer2.adapters.CommentAdapter;
import com.example.musicplayer2.models.Comment;

import java.util.ArrayList;
import java.util.List;

public class PlayerFragment extends Fragment {

    // UI Components
    private ImageButton btnBack, btnShuffle, btnPrevious, btnPlayPause, btnNext, btnRepeat;
    private ImageView albumArtImageView;
    private TextView musicTitleTextView, musicArtistTextView, currentTimeTextView, totalTimeTextView, commentsLabel;
    private SeekBar seekBar;
    private RecyclerView commentsRecyclerView;
    private EditText commentEditText;
    private Button addCommentButton;

    
    private FirebaseFirestore db;

    
    private CommentAdapter commentAdapter;
    private List<Comment> commentList;

   
    private String musicId, musicTitle, musicArtist, musicUrl, imageUrl;

   
    private MediaPlayerManager mediaPlayerManager;
    private Handler handler;
    private Runnable updateSeekBar;
    private boolean isPlaying = false;
    private boolean isSeekbarTracking = false;
    private boolean isRepeatEnabled = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize MediaPlayerManager
        mediaPlayerManager = MediaPlayerManager.getInstance();

        // Initialize Handler
        handler = new Handler();

        // Retrieve arguments passed to the fragment
        if (getArguments() != null) {
            musicUrl = getArguments().getString("MUSIC_URL");
            musicTitle = getArguments().getString("MUSIC_TITLE");
            musicArtist = getArguments().getString("MUSIC_ARTIST");
            imageUrl = getArguments().getString("MUSIC_IMAGE");
            musicId = getArguments().getString("MUSIC_ID");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_player, container, false);

        // Initialize UI Components
        initializeViews(view);

        // Initialize RecyclerView for Comments
        setupCommentsRecyclerView();

        // Load Music Data
        loadMusicData();

        // Set up Click Listeners
        setupClickListeners();

        // Setup MediaPlayer
        setupMediaPlayer();

        // Start SeekBar Update
        setupSeekBarUpdate();

        return view;
    }

    private void initializeViews(View view) {
        // Back Button
        btnBack = view.findViewById(R.id.btnBack);

        // Album Art
        albumArtImageView = view.findViewById(R.id.albumArtImageView);

        // Song Title and Artist
        musicTitleTextView = view.findViewById(R.id.musicTitleTextView);
        musicArtistTextView = view.findViewById(R.id.musicArtistTextView);

        // SeekBar and Time Labels
        seekBar = view.findViewById(R.id.seekBar);
        currentTimeTextView = view.findViewById(R.id.currentTimeTextView);
        totalTimeTextView = view.findViewById(R.id.totalTimeTextView);

        // Playback Controls
        btnShuffle = view.findViewById(R.id.btnShuffle);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        btnNext = view.findViewById(R.id.btnNext);
        btnRepeat = view.findViewById(R.id.btnRepeat);

        // Comments Section
        commentsLabel = view.findViewById(R.id.commentsLabel);
        commentsRecyclerView = view.findViewById(R.id.commentsRecyclerView);
        commentEditText = view.findViewById(R.id.commentEditText);
        addCommentButton = view.findViewById(R.id.addCommentButton);
    }

    private void setupClickListeners() {
        if (btnBack == null) {
            Log.e("PlayerFragment", "btnBack is null");
        } else {
            btnBack.setOnClickListener(v -> dismissFragment());
        }

        if (btnPlayPause == null) {
            Log.e("PlayerFragment", "btnPlayPause is null");
        } else {
            btnPlayPause.setOnClickListener(v -> togglePlayPause());
        }

        if (addCommentButton == null) {
            Log.e("PlayerFragment", "addCommentButton is null");
        } else {
            addCommentButton.setOnClickListener(v -> addComment());
        }

        if (btnRepeat != null) {
            btnRepeat.setOnClickListener(v -> toggleRepeat());
        }
    }

    private void dismissFragment() {
        if (getActivity() != null && getFragmentManager() != null) {
            getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.animator.slide_in_down, R.animator.slide_out_down)
                    .remove(this)
                    .commit();
        }
    }

    private void loadMusicData() {
        musicTitleTextView.setText(musicTitle);
        musicArtistTextView.setText(musicArtist);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.album_art_background)
                    .error(R.drawable.album_art_background)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e("PlayerFragment", "Image load failed: " + e.getMessage());
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                                       Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d("PlayerFragment", "Image loaded successfully");
                            return false;
                        }
                    })
                    .into(albumArtImageView);
        } else {
            Log.d("PlayerFragment", "No image URL provided, using default");
            albumArtImageView.setImageResource(R.drawable.album_art_background);
        }

        // Load Comments from Firestore
        loadComments();
    }

    private void setupMediaPlayer() {
        mediaPlayerManager.setOnPreparedListener(() -> {
            isPlaying = mediaPlayerManager.isPlaying();  // Get current state
            updatePlayPauseButton();
            int duration = mediaPlayerManager.getDuration();
            seekBar.setMax(duration);
            updateTimeLabels();
        });

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Don't start playback if already playing
        if (!mediaPlayerManager.isCurrentSong(musicUrl)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mediaPlayerManager.playMusicFromUrl(requireContext(), musicUrl);
                mediaPlayerManager.setCurrentMusicInfo(
                        musicId,
                        musicUrl,
                        musicTitle,
                        musicArtist,
                        imageUrl
                );
            }
        } else {
            // Just update UI for current playback
            isPlaying = mediaPlayerManager.isPlaying();
            updatePlayPauseButton();
            seekBar.setMax(mediaPlayerManager.getDuration());
            updateTimeLabels();
        }

        // Add explicit repeat state initialization
        isRepeatEnabled = mediaPlayerManager.isRepeatEnabled();
        Log.d("PlayerFragment", "Initial repeat state: " + isRepeatEnabled);
        updateRepeatButton();
    }

    private void setupSeekBarUpdate() {
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayerManager != null && !isSeekbarTracking) {
                    int currentPosition = mediaPlayerManager.getCurrentPosition();
                    int duration = mediaPlayerManager.getDuration();

                    if (duration > 0) {
                        seekBar.setMax(duration);
                        seekBar.setProgress(currentPosition);
                        updateTimeLabels();
                    }
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateSeekBar);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayerManager.isPrepared()) {
                    mediaPlayerManager.seekTo(progress);
                    updateTimeLabels();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeekbarTracking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeekbarTracking = false;
            }
        });
    }

    private void togglePlayPause() {
        if (mediaPlayerManager != null) {
            mediaPlayerManager.togglePlayPause();
            // No need to manually toggle isPlaying
            // isPlaying = !isPlaying; // Remove or comment out this line
            // Update play/pause button state based on mediaPlayerManager
            updatePlayPauseButton();
        }
    }

    private void updatePlayPauseButton() {
        if (mediaPlayerManager != null) {
            isPlaying = mediaPlayerManager.isPlaying();
            btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }

    private void updateTimeLabels() {
        int currentPosition = isSeekbarTracking ?
                seekBar.getProgress() : mediaPlayerManager.getCurrentPosition();
        int duration = mediaPlayerManager.getDuration();

        currentTimeTextView.setText(formatTime(currentPosition));
        totalTimeTextView.setText(formatTime(duration));
    }

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void setupCommentsRecyclerView() {
        commentList = new ArrayList<>();
        commentAdapter = new CommentAdapter(requireContext(), commentList);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        commentsRecyclerView.setAdapter(commentAdapter);
    }

    private void loadComments() {
        db.collection("music")
                .document(musicId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        commentList.clear();
                        for (DocumentSnapshot doc : task.getResult()) {
                            Comment comment = doc.toObject(Comment.class);
                            commentList.add(comment);
                        }
                        commentAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(requireContext(), "Failed to load comments", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addComment() {
        String commentText = commentEditText.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(requireContext(), "Comment cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String userName = currentUser.getDisplayName();

        if (userName == null || userName.isEmpty()) {
            fetchUserNameFromFirestore(userId, fetchedUserName -> {
                postComment(userId, fetchedUserName != null ? fetchedUserName : "Unknown", commentText);
            });
        } else {
            postComment(userId, userName, commentText);
        }
    }

    private void postComment(String userId, String userName, String commentText) {
        FirebaseUtils.addComment(musicId, userId, userName, commentText, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(requireContext(), "Comment added", Toast.LENGTH_SHORT).show();
                commentEditText.setText("");
                loadComments();
            } else {
                Toast.makeText(requireContext(), "Failed to add comment", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error adding comment: ", task.getException());
            }
        });
    }

    private void fetchUserNameFromFirestore(String userId, OnSuccessListener<String> onSuccessListener) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String userName = documentSnapshot.getString("name");
                    onSuccessListener.onSuccess(userName);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user name", e);
                    onSuccessListener.onSuccess(null);
                });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (handler != null) {
            handler.removeCallbacks(updateSeekBar);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (handler != null && updateSeekBar != null) {
            handler.post(updateSeekBar);
        }
        updatePlayPauseButton();
        updateRepeatButton();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null) {
            handler.removeCallbacks(updateSeekBar);
        }
        // Don't release player here since it's managed by the service
        // if (mediaPlayerManager != null) {
        //     mediaPlayerManager.releasePlayer();
        // }
    }

    // Add more descriptive logging in toggleRepeat()
    private void toggleRepeat() {
        isRepeatEnabled = !isRepeatEnabled;
        mediaPlayerManager.setRepeat(isRepeatEnabled);
        Log.d("PlayerFragment", "Repeat toggled. New state: " + isRepeatEnabled);
        updateRepeatButton();
    }

    private void updateRepeatButton() {
        if (btnRepeat != null) {
            int resourceId = isRepeatEnabled ? R.drawable.ic_repeat_on : R.drawable.ic_repeat;
            Log.d("PlayerFragment", "Updating repeat button. Using resource: " + 
                (isRepeatEnabled ? "ic_repeat_on" : "ic_repeat"));
            btnRepeat.setImageResource(resourceId);
            // Add tint for additional visual feedback
            btnRepeat.setColorFilter(
                isRepeatEnabled ? 
                getResources().getColor(R.color.spotify_green) : 
                getResources().getColor(R.color.white)
            );
        }
    }
}