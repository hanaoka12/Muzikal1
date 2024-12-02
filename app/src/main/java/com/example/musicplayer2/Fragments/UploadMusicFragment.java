package com.example.musicplayer2.Fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.musicplayer2.Fragments.HomeFragment;
import com.example.musicplayer2.R;
import com.example.musicplayer2.utils.FirebaseUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class UploadMusicFragment extends Fragment {
    private static final int PICK_MUSIC_FILE = 1;
    private static final int PICK_IMAGE_FILE = 2;

    private ImageButton btnBack;
    private TextInputEditText titleEditText;
    private Spinner genreSpinner;
    private ImageView musicImageView;
    private MaterialButton chooseImageButton;
    private MaterialButton chooseFileButton;
    private MaterialButton uploadButton;
    private ProgressBar uploadProgressBar;

    private Uri musicFileUri;
    private Uri imageFileUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload_music, container, false);
        
        initViews(view);
        setupListeners();
        setupSpinner();
        
        return view;
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        titleEditText = view.findViewById(R.id.titleEditText);
        genreSpinner = view.findViewById(R.id.genreSpinner);
        musicImageView = view.findViewById(R.id.musicImageView);
        chooseImageButton = view.findViewById(R.id.chooseImageButton);
        chooseFileButton = view.findViewById(R.id.chooseFileButton);
        uploadButton = view.findViewById(R.id.uploadButton);
        uploadProgressBar = view.findViewById(R.id.uploadProgressBar);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        chooseFileButton.setOnClickListener(v -> openFileChooser());
        chooseImageButton.setOnClickListener(v -> openImageChooser());
        uploadButton.setOnClickListener(v -> uploadMusic());
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.genres_array,
            android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genreSpinner.setAdapter(adapter);
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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == PICK_MUSIC_FILE) {
                musicFileUri = data.getData();
                Toast.makeText(requireContext(), "Music file selected", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(requireContext(), "Title and music file are required", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadProgressBar.setVisibility(View.VISIBLE);


        if (imageFileUri != null) {
            FirebaseUtils.uploadMusicImage(imageFileUri, task -> {
                if (task.isSuccessful()) {
                    String imageUrl = task.getResult();
                    if (imageUrl != null) {
                        uploadMusicWithImage(title, genre, imageUrl);
                    } else {
                        uploadProgressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Failed to get image URL", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    uploadProgressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
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
                    uploadProgressBar.setVisibility(View.GONE);
                    if (saveTask.isSuccessful()) {
                        Toast.makeText(requireContext(), "Music uploaded successfully", Toast.LENGTH_SHORT).show();

                        if (getActivity() != null) {
                            getActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, new HomeFragment())
                                .commit();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to save music data", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                uploadProgressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to upload music file", Toast.LENGTH_SHORT).show();
            }
        });
    }
}