package com.example.PlayTunes;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    // UI Components
    private MediaPlayer mp;
    private ImageButton playButton;
    private ImageButton replayButton;
    private ImageButton skipButton;
    private ImageButton backButton;
    private SeekBar seekSong, seekVolume;
    private TextView timeCurrent, timeTotal, volumeCurrent, volumeMax, songNameTextView;
    private ImageView vinylRecord;

    // Timers and Flags
    private Timer timer;
    private boolean isSeekBarBeingTouched = false;

    // Audio Manager
    private AudioManager audioManager;

    // ActivityResultLauncher for picking audio
    private final ActivityResultLauncher<Intent> pickAudioLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> onActivityResult(result.getResultCode(), result.getData())
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        initializeUI();

        // Initialize volume controls
        initializeVolumeControls();

        // Set initial state
        setInitialState();

        // Set listeners for SeekBars
        setSeekBarListeners();

        // Initialize and start the timer for seek bar updates
        initializeSeekBarTimer();

    }

    /**
     * Initializes UI components.
     */
    private void initializeUI() {
        // Buttons
        playButton = findViewById(R.id.playButton);
        replayButton = findViewById(R.id.replayButton);
        skipButton = findViewById(R.id.skipButton);
        backButton = findViewById(R.id.backButton);

        // SeekSong field
        seekSong = findViewById(R.id.seekSong);
        timeCurrent = findViewById(R.id.timeCurrent);
        timeTotal = findViewById(R.id.timeTotal);

        // SeekVolume field
        seekVolume = findViewById(R.id.seekVolume);
        volumeCurrent = findViewById(R.id.volumeCurrent);
        volumeMax = findViewById(R.id.volumeMax);

        // Text and Image Views
        songNameTextView = findViewById(R.id.songNameTextView);
        vinylRecord = findViewById(R.id.vinylRecord);
    }

    /**
     * Initializes volume controls using AudioManager.
     */
    private void initializeVolumeControls() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        // Initialize volume seek bar and buttons
        seekVolume.setMax(maxVolume);
        seekVolume.setProgress(currentVolume);
        volumeCurrent.setText(String.valueOf(currentVolume));
        volumeMax.setText(String.valueOf(maxVolume));
    }

    /**
     * Sets the initial disabled state of buttons and seek bars.
     */
    private void setInitialState() {
        playButton.setEnabled(false);
        replayButton.setEnabled(false);
        skipButton.setEnabled(false);
        backButton.setEnabled(false);
        seekSong.setEnabled(false);
    }

    /**
     * Sets listeners for SeekBars.
     */
    private void setSeekBarListeners() {
        // SeekSong listener
        seekSong.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (mp != null) {
                        mp.seekTo(progress);
                        timeCurrent.setText(createTimerLabel(progress));
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeekBarBeingTouched = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeekBarBeingTouched = false;
            }
        });

        // SeekVolume listener
        seekVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                    volumeCurrent.setText(String.valueOf(progress));
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

    /**
     * Initializes and starts the timer for seek bar updates.
     */
    private void initializeSeekBarTimer() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (mp != null && !isSeekBarBeingTouched) {
                        seekSong.setProgress(mp.getCurrentPosition());
                        timeCurrent.setText(createTimerLabel(mp.getCurrentPosition()));
                    }
                });
            }
        }, 0, 100);
    }

    /**
     * Converts the duration from milliseconds into a formatted timer label (mm:ss).
     *
     * @param duration Duration in milliseconds.
     * @return Formatted timer label.
     */
    public String createTimerLabel(int duration) {
        String timerLabel = "";
        int min = duration / 1000 / 60;
        int sec = duration / 1000 % 60;

        timerLabel += min + ":";

        if (sec < 10) timerLabel += "0";
        timerLabel += sec;

        return timerLabel;
    }

    /**
     * Updates the play/pause button based on the playback state.
     */
    private void updatePlayButton() {
        if (mp != null && !mp.isPlaying()) {
            playButton.setImageResource(R.drawable.play);
        } else {
            playButton.setImageResource(R.drawable.pause);
        }
    }

    /**
     * Handles the play/pause functionality.
     *
     * @param v The View that triggers the method.
     */
    public void playAudio(View v) {
        if (mp != null) {
            if (mp.isPlaying()) {
                mp.pause();
                updatePlayButton();
                stopVinylRecordAnimation();
            } else {
                // Check if the user seeks to the end of the song
                if (seekSong.getProgress() >= seekSong.getMax()) {
                    // If yes, reset MediaPlayer to the beginning
                    mp.seekTo(0);
                }

                mp.start();
                updatePlayButton();
                startVinylRecordAnimation();
            }
        }
    }

    /**
     * Seeks the MediaPlayer to the beginning of the song.
     *
     * @param v The View that triggers the method.
     */
    public void replayAudio(View v) {
        if (mp != null) {
            mp.seekTo(0);
        }
    }

    /**
     * Skips the playback forward by 30 seconds.
     *
     * @param v The View that triggers the method.
     */
    public void skipForward(View v) {
        if (mp != null) {
            int currentPosition = mp.getCurrentPosition();
            int duration = mp.getDuration();
            int newPosition = currentPosition + 30000;
            if (newPosition > duration) {
                newPosition = duration;
            }
            mp.seekTo(newPosition);
        }
    }

    /**
     * Skips the playback backward by 30 seconds.
     *
     * @param v The View that triggers the method.
     */
    public void skipBackward(View v) {
        if (mp != null) {
            int currentPosition = mp.getCurrentPosition();
            int newPosition = currentPosition - 30000;
            if (newPosition < 0) {
                newPosition = 0;
            }
            mp.seekTo(newPosition);
        }
    }

    /**
     * Initiates the process of picking an audio file from external storage.
     *
     * @param v The View that triggers the method.
     */
    public void pickAudio(View v) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        pickAudioLauncher.launch(intent);
    }

    /**
     * Handles the result of the audio file selection intent.
     *
     * @param resultCode The result code.
     * @param data       The intent data containing the selected audio file URI.
     */
    private void onActivityResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            Uri selectedAudioUri = data.getData();
            if (selectedAudioUri != null) {
                // Get the file name
                String fileName = getFileName(selectedAudioUri);
                songNameTextView.setText(fileName);

                releaseMediaPlayer();
                try {
                    mp = new MediaPlayer();
                    mp.setDataSource(getApplicationContext(), selectedAudioUri);
                    mp.prepare();
                    if (mp != null) {
                        mp.setOnCompletionListener(mediaPlayer -> updatePlayButton());
                    }
                    playButton.setEnabled(true);
                    replayButton.setEnabled(true);
                    skipButton.setEnabled(true);
                    backButton.setEnabled(true);
                    seekSong.setEnabled(true);

                    seekSong.setMax(mp.getDuration());
                    timeTotal.setText(createTimerLabel(mp.getDuration()));

                    seekSong.setProgress(0);
                    timeCurrent.setText(createTimerLabel(0));

                    mp.start();
                    updatePlayButton();
                    startVinylRecordAnimation();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Starts the rotation animation for the vinyl record.
     */
    private void startVinylRecordAnimation() {
        vinylRecord.animate().rotationBy(3f).setDuration(100).setInterpolator(new LinearInterpolator()).withEndAction(() -> {
            if (mp != null && mp.isPlaying()) {
                startVinylRecordAnimation();
            }
        }).start();
    }

    /**
     * Stops the rotation animation for the vinyl record.
     */
    private void stopVinylRecordAnimation() {
        vinylRecord.clearAnimation();
    }

    /**
     * Retrieves the display name of the selected audio file.
     *
     * @param uri The URI of the selected audio file.
     * @return The display name of the audio file.
     */
    private String getFileName(Uri uri) {
        String result = null;
        String[] projection = {MediaStore.Images.Media.DISPLAY_NAME};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            result = cursor.getString(index);
            cursor.close();
        }

        // Trim, remove ".mp3", and limit to 30 characters
        if (result != null) {
            result = result.replaceAll("\\.mp3$", ""); // Remove ".mp3"
            result = result.length() > 30 ? result.substring(0, 30) : result; // Trim to 30 characters
        }

        return result;
    }

    /**
     * Releases the resources of the MediaPlayer.
     */
    private void releaseMediaPlayer() {
        if (mp != null) {
            mp.release();
            mp = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
        releaseMediaPlayer();
    }
}
