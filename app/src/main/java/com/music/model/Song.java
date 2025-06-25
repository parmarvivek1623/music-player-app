package com.music.model;

public class Song {
    private final long id;
    private final String title;
    private final long duration;

    public Song(long id, String title, long duration) {
        this.id = id;
        this.title = title;
        this.duration = duration;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public long getDuration() {
        return duration;
    }
}