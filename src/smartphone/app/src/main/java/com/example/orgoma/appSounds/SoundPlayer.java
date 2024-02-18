package com.example.orgoma.appSounds;

import android.content.Context;
import android.media.MediaPlayer;

public class SoundPlayer {
    private MediaPlayer mediaPlayer;
    private int soundResource;

    // Constructor to initialize the sound resource
    public SoundPlayer(Context context, int soundResource) {
        this.soundResource = soundResource;
        mediaPlayer = MediaPlayer.create(context, soundResource);
    }

    public void playSound() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void stopSound() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            mediaPlayer.seekTo(0); // Rewind to the beginning
        }
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public void releaseResources() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }
}