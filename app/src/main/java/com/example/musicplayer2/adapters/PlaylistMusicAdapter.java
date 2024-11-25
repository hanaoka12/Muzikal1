package com.example.musicplayer2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.musicplayer2.R;
import com.example.musicplayer2.models.Music;
import java.util.List;

public class PlaylistMusicAdapter extends RecyclerView.Adapter<PlaylistMusicAdapter.ViewHolder> {
    private Context context;
    private List<Music> musicList;
    private String playlistId;
    private OnMusicClickListener listener;
    private OnMusicDeleteListener deleteListener;

    public interface OnMusicClickListener {
        void onMusicClick(Music music);
    }

    public interface OnMusicDeleteListener {
        void onDeleteClick(Music music);
    }

    public PlaylistMusicAdapter(Context context, List<Music> musicList, String playlistId,
                              OnMusicClickListener listener, OnMusicDeleteListener deleteListener) {
        this.context = context;
        this.musicList = musicList;
        this.playlistId = playlistId;
        this.listener = listener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist_music, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Music music = musicList.get(position);
        holder.titleTextView.setText(music.getTitle());
        holder.artistTextView.setText(music.getArtist());

        if (music.getImageUrl() != null && !music.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(music.getImageUrl())
                    .placeholder(R.drawable.album_art_background)
                    .into(holder.musicImageView);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMusicClick(music);
            }
        });

        holder.menuButton.setOnClickListener(v -> showPopupMenu(v, music));
    }

    private void showPopupMenu(View view, Music music) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.inflate(R.menu.playlist_music_menu);
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_remove) {
                if (deleteListener != null) {
                    deleteListener.onDeleteClick(music);
                }
                return true;
            }
            return false;
        });
        popup.show();
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView musicImageView;
        TextView titleTextView, artistTextView;
        ImageButton menuButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            musicImageView = itemView.findViewById(R.id.musicImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            artistTextView = itemView.findViewById(R.id.artistTextView);
            menuButton = itemView.findViewById(R.id.menuButton);
        }
    }
}
