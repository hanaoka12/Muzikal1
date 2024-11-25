package com.example.musicplayer2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.musicplayer2.R;
import com.example.musicplayer2.models.Music;
import java.util.List;

public class SearchMusicAdapter extends RecyclerView.Adapter<SearchMusicAdapter.ViewHolder> {
    private Context context;
    private List<Music> musicList;
    private OnMusicSelectListener listener;

    public interface OnMusicSelectListener {
        void onMusicSelected(Music music);
    }

    public SearchMusicAdapter(Context context, List<Music> musicList, OnMusicSelectListener listener) {
        this.context = context;
        this.musicList = musicList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_music, parent, false);
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
                listener.onMusicSelected(music);
            }
        });
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView musicImageView;
        TextView titleTextView, artistTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            musicImageView = itemView.findViewById(R.id.musicImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            artistTextView = itemView.findViewById(R.id.artistTextView);
        }
    }
}
