package com.example.musicplayer2.adapters;



import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.musicplayer2.R;
import com.example.musicplayer2.models.Music;

import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {

    private Context context;
    private List<Music> musicList;
    private OnMusicClickListener onMusicClickListener;

    // Constructor
    public MusicAdapter(Context context, List<Music> musicList, OnMusicClickListener listener) {
        this.context = context;
        this.musicList = musicList;
        this.onMusicClickListener = listener;
    }

    // ViewHolder class
    public static class MusicViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTextView;
        public TextView artistTextView;
        public ImageView musicImageView;
        public ImageButton moreButton;

        public MusicViewHolder(View itemView, final OnMusicClickListener listener) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.music_title);
            artistTextView = itemView.findViewById(R.id.music_artist);
            musicImageView = itemView.findViewById(R.id.musicImageView);
            moreButton = itemView.findViewById(R.id.moreButton);

            // Handle music item click
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onMusicClick(position);
                    }
                }
            });
        }
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.music_item, parent, false);
        return new MusicViewHolder(view, onMusicClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        Music music = musicList.get(position);
        holder.titleTextView.setText(music.getTitle());
        holder.artistTextView.setText(music.getArtist());

        // Load image using Glide
        if (music.getImageUrl() != null && !music.getImageUrl().isEmpty()) {
            Glide.with(context)
                .load(music.getImageUrl())
                .placeholder(R.drawable.album_art_background)
                .transform(new RoundedCorners(8))
                .into(holder.musicImageView);
        } else {
            holder.musicImageView.setImageResource(R.drawable.album_art_background);
        }
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    // Interface for handling music item clicks
    public interface OnMusicClickListener {
        void onMusicClick(int position);
    }
}
