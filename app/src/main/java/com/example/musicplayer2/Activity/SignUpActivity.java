package com.example.musicplayer2.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.musicplayer2.R;
import com.example.musicplayer2.utils.FirebaseUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignUpActivity extends AppCompatActivity {

    private EditText nameEditText, emailEditText, passwordEditText;
    private Button signUpButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private static final String TAG = "SignUpActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signUpButton = findViewById(R.id.signUpButton);
        progressBar = findViewById(R.id.progressBar);

        signUpButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(name)) {
            nameEditText.setError("Name is required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return;
        }

        progressBar.setVisibility(android.view.View.VISIBLE);
        signUpButton.setEnabled(false);

        // Create Firebase Auth account
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Create user profile in Firestore
                            FirebaseUtils.createUserProfile(
                                    user.getUid(),
                                    email,
                                    name,
                                    profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            Toast.makeText(SignUpActivity.this,
                                                    "Registration Successful",
                                                    Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                                            finish();
                                        } else {
                                            Log.e(TAG, "Failed to create user profile", profileTask.getException());
                                            Toast.makeText(SignUpActivity.this,
                                                    "Failed to create user profile",
                                                    Toast.LENGTH_LONG).show();
                                        }
                                        progressBar.setVisibility(android.view.View.GONE);
                                        signUpButton.setEnabled(true);
                                    });
                        }
                    } else {
                        Exception exception = task.getException();
                        Log.e(TAG, "Registration failed: " + exception.getMessage());
                        Toast.makeText(SignUpActivity.this,
                                "Registration Failed: " + exception.getMessage(),
                                Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(android.view.View.GONE);
                        signUpButton.setEnabled(true);
                    }
                });
    }
}