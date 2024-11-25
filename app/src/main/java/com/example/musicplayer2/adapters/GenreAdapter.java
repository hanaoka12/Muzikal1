package com.example.musicplayer2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer2.R;
import com.example.musicplayer2.utils.FirebaseUtils;

import java.util.List;

public class GenreAdapter extends RecyclerView.Adapter<GenreAdapter.GenreViewHolder> {
    private final List<String> genres;
    private final Context context;
    private final OnGenreClickListener listener;

    public interface OnGenreClickListener {
        void onGenreClick(String genre);
    }

    public GenreAdapter(Context context, List<String> genres, OnGenreClickListener listener) {
        this.context = context;
        this.genres = genres;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GenreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_genre, parent, false);
        return new GenreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GenreViewHolder holder, int position) {
        String genre = genres.get(position);
        holder.genreTextView.setText(genre);
        
        // Set background gradient based on genre
        int backgroundRes;
        int iconRes;
        switch (genre.toLowerCase()) {
            case "pop":
                backgroundRes = R.drawable.gradient_pop;
                iconRes = R.drawable.ic_pop;
                break;
            case "rock":
                backgroundRes = R.drawable.gradient_rock;
                iconRes = R.drawable.ic_rock;
                break;
            case "jazz":
                backgroundRes = R.drawable.gradient_jazz;
                iconRes = R.drawable.ic_jazz;
                break;
            default:
                backgroundRes = R.drawable.gradient_pop;
                iconRes = R.drawable.ic_music_note;
                break;
        }
        
        holder.itemView.setBackgroundResource(backgroundRes);
        holder.genreIcon.setImageResource(iconRes);
        
        // Get song count for this genre
        FirebaseUtils.getMusicCollection()
            .whereEqualTo("genre", genre)
            .get()
            .addOnSuccessListener(documents -> {
                int count = documents.size();
                holder.songCountText.setText(count + " songs");
            });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGenreClick(genre);
                
                // Add subtle scale animation
                v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> 
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start())
                    .start();
            }
        });
    }

    @Override
    public int getItemCount() {
        return genres.size();
    }

    static class GenreViewHolder extends RecyclerView.ViewHolder {
        TextView genreTextView;
        TextView songCountText;
        ImageView genreIcon;
        View itemView;

        GenreViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            genreTextView = itemView.findViewById(R.id.genreTextView);
            songCountText = itemView.findViewById(R.id.songCountText);
            genreIcon = itemView.findViewById(R.id.genreIcon);
        }
    }
}