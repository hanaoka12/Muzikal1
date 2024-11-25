package com.example.musicplayer2.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.musicplayer2.R;
import com.example.musicplayer2.utils.FirebaseUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class UploadMusicActivity extends AppCompatActivity {
    private static final int PICK_MUSIC_FILE = 1;
    private static final int PICK_IMAGE_FILE = 2;
    private EditText titleEditText;
    private ImageView musicImageView;
    private Spinner genreSpinner;
    private Button chooseFileButton, chooseImageButton, uploadButton;
    private ProgressBar progressBar;
    private Uri musicFileUri;
    private Uri imageFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_music);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        titleEditText = findViewById(R.id.titleEditText);
        musicImageView = findViewById(R.id.musicImageView);
        genreSpinner = findViewById(R.id.genreSpinner);
        chooseFileButton = findViewById(R.id.chooseFileButton);
        chooseImageButton = findViewById(R.id.chooseImageButton);
        uploadButton = findViewById(R.id.uploadButton);
        progressBar = findViewById(R.id.uploadProgressBar);

        chooseFileButton.setOnClickListener(v -> openFileChooser());
        chooseImageButton.setOnClickListener(v -> openImageChooser());

        // Initialize genre spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.genres_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genreSpinner.setAdapter(adapter);

        uploadButton.setOnClickListener(v -> uploadMusic());
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(intent, PICK_MUSIC_FILE);
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == PICK_MUSIC_FILE) {
                musicFileUri = data.getData();
                Toast.makeText(this, "Music file selected", Toast.LENGTH_SHORT).show();
            } else if (requestCode == PICK_IMAGE_FILE) {
                imageFileUri = data.getData();
                musicImageView.setImageURI(imageFileUri);
            }
        }
    }

    private void uploadMusic() {
        String title = titleEditText.getText().toString().trim();
        String genre = genreSpinner.getSelectedItem().toString();

        if (title.isEmpty() || musicFileUri == null) {
            Toast.makeText(this, "Title and music file are required", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // First upload the image if selected
        if (imageFileUri != null) {
            FirebaseUtils.uploadMusicImage(imageFileUri, task -> {
                if (task.isSuccessful()) {
                    // Extract the String from the Task result
                    String imageUrl = task.getResult();
                    if (imageUrl != null) {
                        uploadMusicWithImage(title, genre, imageUrl);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Failed to get image URL", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            uploadMusicWithImage(title, genre, "");
        }
    }

    private void uploadMusicWithImage(String title, String genre, String imageUrl) {
        FirebaseUtils.uploadMusic(musicFileUri, title, genre, imageUrl, task -> {
            if (task.isSuccessful()) {
                String fileUrl = task.getResult().toString();
                FirebaseUtils.saveMusicData(title, genre, fileUrl, imageUrl, saveTask -> {
                    progressBar.setVisibility(View.GONE);
                    if (saveTask.isSuccessful()) {
                        Toast.makeText(UploadMusicActivity.this, "Music uploaded successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(UploadMusicActivity.this, "Failed to save music data", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(UploadMusicActivity.this, "Failed to upload music file", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
