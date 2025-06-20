package com.music.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {

    private final ArrayList<String> musicList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onMusicClick(int position);
    }

    public MusicAdapter(ArrayList<String> musicList, OnItemClickListener listener) {
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
        String path = musicList.get(position);
        String name = path.substring(path.lastIndexOf("/") + 1);
        holder.songName.setText(name);

        holder.itemView.setOnClickListener(v -> listener.onMusicClick(position));
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    public static class MusicViewHolder extends RecyclerView.ViewHolder {
        TextView songName;

        public MusicViewHolder(View itemView) {
            super(itemView);
            songName = itemView.findViewById(R.id.songName);
        }
    }
}