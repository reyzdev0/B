package com.savequest.app.feedback;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.savequest.app.util.AppPrefs;

/** Haptic + efek suara (tanpa file audio; nada dibuat ToneGenerator). Mengikuti pengaturan user. */
public final class FeedbackManager {
    private static volatile FeedbackManager instance;

    public static FeedbackManager get(Context c) {
        if (instance == null) {
            synchronized (FeedbackManager.class) {
                if (instance == null) instance = new FeedbackManager(c.getApplicationContext());
            }
        }
        return instance;
    }

    private final AppPrefs prefs;
    private final Vibrator vibrator;
    private ToneGenerator tone;

    private FeedbackManager(Context c) {
        prefs = new AppPrefs(c);
        vibrator = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
    }

    /** Ketukan ringan: navigasi, keypad. */
    public void tick() { vibrate(0, 12); }

    /** Berhasil setor. */
    public void success() {
        vibrate(0, 30, 60, 40);
        beep(ToneGenerator.TONE_PROP_ACK, 120);
    }

    /** Klaim hadiah quest. */
    public void claim() {
        vibrate(0, 25, 50, 25, 50, 60);
        beep(ToneGenerator.TONE_PROP_PROMPT, 160);
    }

    /** Naik level. */
    public void levelUp() {
        vibrate(0, 40, 60, 40, 60, 40, 60, 120);
        beep(ToneGenerator.TONE_PROP_BEEP2, 250);
    }

    public void error() {
        vibrate(0, 80, 60, 80);
        beep(ToneGenerator.TONE_PROP_NACK, 150);
    }

    private int amplitude() {
        switch (prefs.hapticLevel()) {
            case 1: return 70;
            case 3: return 255;
            default: return 150;
        }
    }

    /** pattern: delay, getar, jeda, getar, ... (ms). */
    private void vibrate(long... pattern) {
        if (!prefs.hapticEnabled() || vibrator == null || !vibrator.hasVibrator()) return;
        try {
            VibrationEffect fx;
            if (vibrator.hasAmplitudeControl()) {
                int[] amps = new int[pattern.length];
                for (int i = 0; i < pattern.length; i++) amps[i] = (i % 2 == 1) ? amplitude() : 0;
                fx = VibrationEffect.createWaveform(pattern, amps, -1);
            } else {
                fx = VibrationEffect.createWaveform(pattern, -1);
            }
            vibrator.vibrate(fx);
        } catch (Exception ignored) {
        }
    }

    private void beep(int toneType, int ms) {
        if (!prefs.soundEnabled()) return;
        try {
            if (tone == null) tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 55);
            tone.startTone(toneType, ms);
        } catch (Exception e) {
            tone = null;
        }
    }
}
