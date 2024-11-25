package com.example.musicplayer2.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import com.example.musicplayer2.R;
import com.example.musicplayer2.utils.FirebaseUtils;

public class CreatePlaylistDialog extends Dialog {
    private EditText playlistNameEditText;
    private Button createButton, cancelButton;
    private OnPlaylistCreatedListener listener;

    public interface OnPlaylistCreatedListener {
        void onPlaylistCreated();
    }

    public CreatePlaylistDialog(@NonNull Context context, OnPlaylistCreatedListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_create_playlist);

        playlistNameEditText = findViewById(R.id.playlistNameEditText);
        createButton = findViewById(R.id.createButton);
        cancelButton = findViewById(R.id.cancelButton);

        createButton.setOnClickListener(v -> {
            String name = playlistNameEditText.getText().toString().trim();
            if (!name.isEmpty()) {
                createPlaylist(name);
            }
        });

        cancelButton.setOnClickListener(v -> dismiss());
    }

    private void createPlaylist(String name) {
        FirebaseUtils.createPlaylist(name, task -> {
            if (task.isSuccessful()) {
                listener.onPlaylistCreated();
                dismiss();
            }
        });
    }
}
