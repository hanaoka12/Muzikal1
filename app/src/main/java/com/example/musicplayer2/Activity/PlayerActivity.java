package com.example.musicplayer2.Activity;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.musicplayer2.R;
import com.example.musicplayer2.utils.FirebaseUtils;
import com.example.musicplayer2.utils.MediaPlayerManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.example.musicplayer2.adapters.CommentAdapter;
import com.example.musicplayer2.models.Comment;
import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends AppCompatActivity {

    // UI Components
    private ImageButton btnBack, btnShuffle, btnPrevious, btnPlayPause, btnNext, btnRepeat;
    private ImageView albumArtImageView;
    private TextView musicTitleTextView, musicArtistTextView, currentTimeTextView, totalTimeTextView, commentsLabel;
    private SeekBar seekBar;
    private RecyclerView commentsRecyclerView;
    private EditText commentEditText;
    private Button addCommentButton;

    // Firestore
    private FirebaseFirestore db;

    // Comments
    private CommentAdapter commentAdapter;
    private List<Comment> commentList;

    // Other variables
    private String musicId, musicTitle, musicArtist, musicUrl, imageUrl;
    
    // Other MediaPlayer related variables
    private MediaPlayerManager mediaPlayerManager;
    private Handler handler;
    private Runnable updateSeekBar;
    private boolean isPlaying = false;
    private boolean isSeekbarTracking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI Components
        initializeViews();

        // Initialize RecyclerView for Comments
        setupCommentsRecyclerView();
        loadMusicData();
        // Load Comments from Firestore
        loadComments();

        // Set up Click Listeners
        setupClickListeners();

        // Initialize other components like MediaPlayer
        
        setupMediaPlayer();
        setupSeekBarUpdate();
    }

    private void initializeViews() {
        // Back Button
        btnBack = findViewById(R.id.btnBack);

        // Album Art
        albumArtImageView = findViewById(R.id.albumArtImageView);

        // Song Title and Artist
        musicTitleTextView = findViewById(R.id.musicTitleTextView);
        musicArtistTextView = findViewById(R.id.musicArtistTextView);

        // SeekBar and Time Labels
        seekBar = findViewById(R.id.seekBar);
        currentTimeTextView = findViewById(R.id.currentTimeTextView);
        totalTimeTextView = findViewById(R.id.totalTimeTextView);

        // Playback Controls
        btnShuffle = findViewById(R.id.btnShuffle);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnRepeat = findViewById(R.id.btnRepeat);

        // Comments Section
        commentsLabel = findViewById(R.id.commentsLabel);
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        commentEditText = findViewById(R.id.commentEditText);
        addCommentButton = findViewById(R.id.addCommentButton);
    }

    private void setupClickListeners() {
        // Back Button
        btnBack.setOnClickListener(v -> finish());
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        // Add Comment Button
        addCommentButton.setOnClickListener(v -> addComment());

        
    }

    private void loadMusicData() {
        musicUrl = getIntent().getStringExtra("MUSIC_URL");
        musicTitle = getIntent().getStringExtra("MUSIC_TITLE");
        musicArtist = getIntent().getStringExtra("MUSIC_ARTIST");
        imageUrl = getIntent().getStringExtra("MUSIC_IMAGE");
        musicId = getIntent().getStringExtra("MUSIC_ID");

        musicTitleTextView.setText(musicTitle);
        musicArtistTextView.setText(musicArtist);

        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.album_art_background)
                    .error(R.drawable.album_art_background)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e("PlayerActivity", "Image load failed: " + e.getMessage());
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d("PlayerActivity", "Image loaded successfully");
                            return false;
                        }
                    })
                    .into(albumArtImageView);
        } else {
            Log.d("PlayerActivity", "No image URL provided, using default");
            albumArtImageView.setImageResource(R.drawable.album_art_background);
        }
    }

    private void setupMediaPlayer() {
        mediaPlayerManager = MediaPlayerManager.getInstance();

        
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
                mediaPlayerManager.playMusicFromUrl(this, musicUrl);
            }
        } else {
            // Just update UI for current playback
            isPlaying = mediaPlayerManager.isPlaying();
            updatePlayPauseButton();
            seekBar.setMax(mediaPlayerManager.getDuration());
            updateTimeLabels();
        }
    }

    private void setupSeekBarUpdate() {
        handler = new Handler();
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
    }

    private void togglePlayPause() {
        if (isPlaying) {
            mediaPlayerManager.pauseMusic();
        } else {
            mediaPlayerManager.resumeMusic();
        }
        isPlaying = !isPlaying;
        updatePlayPauseButton();
    }

    private void updatePlayPauseButton() {
        btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
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
        commentAdapter = new CommentAdapter(this, commentList);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
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
                    Toast.makeText(this, "Failed to load comments", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void addComment() {
        String commentText = commentEditText.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Comment added", Toast.LENGTH_SHORT).show();
                commentEditText.setText("");
                loadComments();
            } else {
                Toast.makeText(this, "Failed to add comment", Toast.LENGTH_SHORT).show();
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
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(updateSeekBar);
        }
        // Don't release player here since it's managed by service
        // if (mediaPlayerManager != null) {
        //     mediaPlayerManager.releasePlayer();
        // }
    }
}