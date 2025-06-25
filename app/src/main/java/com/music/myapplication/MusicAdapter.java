package com.music.myapplication;

import android.content.ContentUris;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.music.model.Song;

import java.util.ArrayList;
import java.util.Locale;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {

    private final ArrayList<Song> musicList;
    private final OnItemClickListener listener;
    private final Context context;

    public interface OnItemClickListener {
        void onMusicClick(int position);
    }

    public MusicAdapter(Context context, ArrayList<Song> musicList, OnItemClickListener listener) {
        this.context = context;
        this.musicList = musicList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MusicViewHolder holder, int position) {
        Song song = musicList.get(position);
        holder.songName.setText(song.getTitle());
        holder.songDuration.setText(formatTime((int) song.getDuration()));

        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.getId());

        try (MediaMetadataRetriever mmr = new MediaMetadataRetriever()) {
            mmr.setDataSource(context, contentUri);
            byte[] artBytes = mmr.getEmbeddedPicture();
            if (artBytes != null) {
                Glide.with(context).asBitmap().load(artBytes).into(holder.albumArt);
            } else {
                holder.albumArt.setImageResource(R.drawable.ic_music_note);
            }
        } catch (Exception e) {
            holder.albumArt.setImageResource(R.drawable.ic_music_note);
        }

        holder.itemView.setOnClickListener(v -> listener.onMusicClick(position));
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    private String formatTime(int millis) {
        int minutes = (millis / 1000) / 60;
        int seconds = (millis / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    public static class MusicViewHolder extends RecyclerView.ViewHolder {
        TextView songName, songDuration;
        ImageView albumArt;

        public MusicViewHolder(View itemView) {
            super(itemView);
            songName = itemView.findViewById(R.id.songName);
            songDuration = itemView.findViewById(R.id.songDuration);
            albumArt = itemView.findViewById(R.id.albumArt);
        }
    }
}