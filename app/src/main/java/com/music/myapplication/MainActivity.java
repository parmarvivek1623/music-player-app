package com.music.myapplication;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView musicRecyclerView;
    private TextView currentSongTitle, startTime, endTime;
    private ImageButton btnPlayPause;
    private SeekBar musicSeekBar;

    private final ArrayList<String> musicList = new ArrayList<>();
    private MediaPlayer mediaPlayer;
    private int currentIndex = 0;

    private final Handler handler = new Handler();
    private Runnable updateSeekBar;

    private static final String TAG = "MusicPlayerApp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        musicRecyclerView = findViewById(R.id.musicRecyclerView);
        currentSongTitle = findViewById(R.id.currentSongTitle);
        startTime = findViewById(R.id.startTime);  // Ensure present in layout
        endTime = findViewById(R.id.endTime);      // Ensure present in layout
        btnPlayPause = findViewById(R.id.btnPlayPause);
        ImageButton btnNext = findViewById(R.id.btnNext);
        ImageButton btnPrevious = findViewById(R.id.btnPrevious);
        musicSeekBar = findViewById(R.id.musicSeekBar);

        if (checkPermissions()) {
            loadMusicFiles();
        } else {
            requestPermissions();
        }

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnNext.setOnClickListener(v -> playNext());
        btnPrevious.setOnClickListener(v -> playPrevious());

        musicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void loadMusicFiles() {
        musicList.clear();
        ContentResolver contentResolver = getContentResolver();
        Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        Cursor cursor = contentResolver.query(collection, projection, selection, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            do {
                String path = cursor.getString(dataColumn);
                if (path.endsWith(".mp3") || path.endsWith(".wav") || path.endsWith(".m4a")) {
                    musicList.add(path);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        musicRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        MusicAdapter adapter = new MusicAdapter(musicList, this::playMusic);
        musicRecyclerView.setAdapter(adapter);
    }

    private void playMusic(int index) {
        if (musicList.size() == 0) return;

        currentIndex = index;
        String path = musicList.get(index);
        String name = path.substring(path.lastIndexOf("/") + 1);
        currentSongTitle.setText(name);

        if (mediaPlayer != null) {
            handler.removeCallbacks(updateSeekBar);
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();
            btnPlayPause.setImageResource(R.drawable.ic_pause);

            musicSeekBar.setMax(mediaPlayer.getDuration());
            endTime.setText(formatTime(mediaPlayer.getDuration()));

            updateSeekBar = new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer != null) {
                        int currentPos = mediaPlayer.getCurrentPosition();
                        musicSeekBar.setProgress(currentPos);
                        startTime.setText(formatTime(currentPos));
                        handler.removeCallbacks(this);
                        handler.postDelayed(this, 500);
                    }
                }
            };
            handler.post(updateSeekBar);

            mediaPlayer.setOnCompletionListener(mp -> {
                handler.removeCallbacks(updateSeekBar);
                playNext();
            });

        } catch (Exception e) {
            Log.e(TAG, "Error playing music: " + e.getMessage(), e);  // Robust logging
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            handler.removeCallbacks(updateSeekBar);
            btnPlayPause.setImageResource(R.drawable.ic_play);
        } else if (mediaPlayer != null) {
            mediaPlayer.start();
            handler.removeCallbacks(updateSeekBar);
            handler.post(updateSeekBar);
            btnPlayPause.setImageResource(R.drawable.ic_pause);
        }
    }

    private void playNext() {
        if (mediaPlayer == null || musicList.size() == 0) return;
        if (currentIndex < musicList.size() - 1) {
            playMusic(++currentIndex);
        }
    }

    private void playPrevious() {
        if (mediaPlayer == null || musicList.size() == 0) return;
        if (currentIndex > 0) {
            playMusic(--currentIndex);
        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        int REQUEST_PERMISSION_CODE = 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_AUDIO}, REQUEST_PERMISSION_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            handler.removeCallbacks(updateSeekBar);
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private String formatTime(int millis) {
        int minutes = (millis / 1000) / 60;
        int seconds = (millis / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}


