package com.example.musicplayer2.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.musicplayer2.R;
import com.example.musicplayer2.models.Playlist;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {
    private Context context;
    private List<Playlist> playlists;
    private OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public PlaylistAdapter(Context context, List<Playlist> playlists, OnPlaylistClickListener listener) {
        this.context = context;
        this.playlists = playlists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.playlistNameTextView.setText(playlist.getName());
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                Log.d("PlaylistAdapter", "Playlist clicked: " + playlist.getName() 
                    + " ID: " + playlist.getPlaylistId());
                listener.onPlaylistClick(playlist);
            }
        });

        if (playlist.getImageUrl() != null && !playlist.getImageUrl().isEmpty()) {
            Glide.with(context)
                .load(playlist.getImageUrl())
                .placeholder(R.drawable.ic_playlist)
                .into(holder.playlistImageView);
        }
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView playlistImageView;
        TextView playlistNameTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            playlistImageView = itemView.findViewById(R.id.playlistImageView);
            playlistNameTextView = itemView.findViewById(R.id.playlistNameTextView);
        }
    }
}
