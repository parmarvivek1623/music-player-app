package com.music.myapplication;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.music.model.Song;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView musicRecyclerView;
    private TextView currentSongTitle, startTime, endTime;
    private ImageButton btnPlayPause;
    private SeekBar musicSeekBar;

    private final ArrayList<Song> musicList = new ArrayList<>();
    private MediaPlayer mediaPlayer;
    private int currentIndex = 0;

    private final Handler handler = new Handler();
    private Runnable updateSeekBar;

    private static final String TAG = "MusicPlayerApp";
    private static final int REQUEST_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        musicRecyclerView = findViewById(R.id.musicRecyclerView);
        currentSongTitle = findViewById(R.id.currentSongTitle);
        startTime = findViewById(R.id.startTime);
        endTime = findViewById(R.id.endTime);
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
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void loadMusicFiles() {
        musicList.clear();
        ContentResolver contentResolver = getContentResolver();
        Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        Cursor cursor = contentResolver.query(collection, projection, selection, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);

            do {
                long id = cursor.getLong(idColumn);
                String title = cursor.getString(titleColumn);
                long duration = cursor.getLong(durationColumn);
                musicList.add(new Song(id, title, duration));
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (musicList.isEmpty()) {
            Toast.makeText(this, "No music files found!", Toast.LENGTH_SHORT).show();
            currentSongTitle.setText(getString(R.string.no_song_playing));
            btnPlayPause.setEnabled(false);
        } else {
            btnPlayPause.setEnabled(true);
        }

        musicRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        MusicAdapter adapter = new MusicAdapter(this, musicList, this::playMusic);
        musicRecyclerView.setAdapter(adapter);
    }

    private void playMusic(int index) {
        if (musicList.isEmpty()) return;

        currentIndex = index;
        Song song = musicList.get(index);
        currentSongTitle.setText(song.getTitle());

        releaseMediaPlayer();

        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.getId());

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(this, contentUri);
            mediaPlayer.prepare();
            mediaPlayer.start();
            setPlayPauseIcon();  // <- Reliable icon update

            musicSeekBar.setMax(mediaPlayer.getDuration());
            endTime.setText(formatTime(mediaPlayer.getDuration()));

            updateSeekBar = new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer != null) {
                        int currentPos = mediaPlayer.getCurrentPosition();
                        musicSeekBar.setProgress(currentPos);
                        startTime.setText(formatTime(currentPos));
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
            Log.e(TAG, "Error playing music: " + e.getMessage(), e);
        }
    }


    private void setPlayPauseIcon() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            btnPlayPause.setImageResource(R.drawable.ic_pause);   // Show pause icon when playing
        } else {
            btnPlayPause.setImageResource(R.drawable.ic_play);    // Show play icon when paused
        }
    }


    private void togglePlayPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                handler.removeCallbacks(updateSeekBar);

            } else {
                mediaPlayer.start();
                handler.post(updateSeekBar);
            }
            setPlayPauseIcon();  // <- Consistent button update
        }
    }


    private void playNext() {
        if (musicList.isEmpty()) return;
        if (currentIndex < musicList.size() - 1) {
            playMusic(++currentIndex);
        }
    }

    private void playPrevious() {
        if (musicList.isEmpty()) return;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_AUDIO}, REQUEST_PERMISSION_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMusicFiles();
            } else {
                Toast.makeText(this, "Permission denied. Cannot load music.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
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
