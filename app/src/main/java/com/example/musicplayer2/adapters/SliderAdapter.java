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

public class SliderAdapter extends RecyclerView.Adapter<SliderAdapter.SliderViewHolder> {

    private final List<Music> sliderMusicList;
    private final Context context;
    private final OnMusicClickListener listener;

    public interface OnMusicClickListener {
        void onMusicClick(Music music);
    }

    public SliderAdapter(Context context, List<Music> sliderMusicList, OnMusicClickListener listener) {
        this.context = context;
        this.sliderMusicList = sliderMusicList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_slider, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        Music music = sliderMusicList.get(position);
        holder.bind(music);
    }

    @Override
    public int getItemCount() {
        return sliderMusicList.size();
    }

    public class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleTextView;

        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.sliderImageView);
            titleTextView = itemView.findViewById(R.id.sliderTitleTextView);
        }

        public void bind(Music music) {
            titleTextView.setText(music.getTitle());
            String imageUrl = music.getImageUrl();

            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(android.R.drawable.ic_menu_report_image) // Use a default placeholder
                        .into(imageView);
            } else {
                imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            }

            itemView.setOnClickListener(v -> listener.onMusicClick(music));
        }
    }
}