package com.example.musicplayer2.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.musicplayer2.R;
import com.example.musicplayer2.models.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private CircleImageView profileImageView;
    private TextInputEditText nameEditText;
    private Button changePhotoButton;
    private Button saveButton;
    private ProgressBar progressBar;
    private Uri imageUri;
    private String currentImageUrl;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initializeViews();
        setupToolbar();
        loadUserData();
        setupListeners();
    }

    private void initializeViews() {
        profileImageView = findViewById(R.id.profileImageView);
        nameEditText = findViewById(R.id.nameEditText);
        changePhotoButton = findViewById(R.id.changePhotoButton);
        saveButton = findViewById(R.id.saveButton);
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Profile");
        }
    }

    private void setupListeners() {
        changePhotoButton.setOnClickListener(v -> openImageChooser());
        saveButton.setOnClickListener(v -> saveChanges());
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        nameEditText.setText(user.getName());
                        currentImageUrl = user.getImageUrl();
                        if (currentImageUrl != null) {
                            Glide.with(this)
                                    .load(currentImageUrl)
                                    .placeholder(R.drawable.default_profile)
                                    .into(profileImageView);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImageView.setImageURI(imageUri);
        }
    }

    private void saveChanges() {
        String newName = nameEditText.getText().toString().trim();
        if (newName.isEmpty()) {
            nameEditText.setError("Name cannot be empty");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);

        if (imageUri != null) {
            uploadImageAndSaveProfile(newName);
        } else {
            updateProfile(newName, currentImageUrl);
        }
    }

    private void updateProfile(String name, String imageUrl) {
        String userId = mAuth.getCurrentUser().getUid();

        // Create a map of fields to update
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        if (imageUrl != null) {
            updates.put("imageUrl", imageUrl);
        }

        // First, check if the document exists
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Document exists, proceed with update
                        db.collection("users").document(userId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    progressBar.setVisibility(View.GONE);
                                    saveButton.setEnabled(true);
                                    Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    saveButton.setEnabled(true);
                                    Toast.makeText(EditProfileActivity.this, "Error updating profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    } else {
                        // Document doesn't exist, create it
                        User newUser = new User();
                        newUser.setUserId(userId);
                        newUser.setName(name);
                        newUser.setImageUrl(imageUrl);
                        newUser.setEmail(mAuth.getCurrentUser().getEmail());

                        db.collection("users").document(userId)
                                .set(newUser)
                                .addOnSuccessListener(aVoid -> {
                                    progressBar.setVisibility(View.GONE);
                                    saveButton.setEnabled(true);
                                    Toast.makeText(EditProfileActivity.this, "Profile created successfully", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    saveButton.setEnabled(true);
                                    Toast.makeText(EditProfileActivity.this, "Error creating profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    saveButton.setEnabled(true);
                    Toast.makeText(EditProfileActivity.this, "Error checking profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void uploadImageAndSaveProfile(String name) {
        String userId = mAuth.getCurrentUser().getUid();
        StorageReference fileRef = storage.getReference().child("profile_images/" + userId);

        // Add metadata
        StorageReference finalRef = fileRef.child(System.currentTimeMillis() + ".jpg");

        finalRef.putFile(imageUri)
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressBar.setProgress((int) progress);
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return finalRef.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    // Delete old image if it exists
                    if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                        StorageReference oldImageRef = storage.getReferenceFromUrl(currentImageUrl);
                        oldImageRef.delete().addOnFailureListener(e -> {
                            // Just log the error, don't stop the process
                            e.printStackTrace();
                        });
                    }
                    updateProfile(name, uri.toString());
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    saveButton.setEnabled(true);
                    Toast.makeText(EditProfileActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}