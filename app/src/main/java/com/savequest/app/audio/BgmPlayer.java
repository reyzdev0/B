package com.savequest.app.audio;

import android.content.Context;
import android.media.MediaPlayer;

import com.savequest.app.R;
import com.savequest.app.util.AppPrefs;

/** Musik latar RPG (3 lagu loop). Mengikuti pengaturan; ada toggle mute cepat. */
public final class BgmPlayer {
    public static final String[] TRACK_NAMES = {"Desa Pagi", "Hutan Tenang", "Malam Berbintang"};
    private static final int[] TRACKS = {R.raw.bgm_village, R.raw.bgm_forest, R.raw.bgm_night};
    private static volatile BgmPlayer instance;

    public static BgmPlayer get(Context c) {
        if (instance == null) {
            synchronized (BgmPlayer.class) {
                if (instance == null) instance = new BgmPlayer(c.getApplicationContext());
            }
        }
        return instance;
    }

    private final Context app;
    private final AppPrefs prefs;
    private MediaPlayer mp;
    private int loadedTrack = -1;

    private BgmPlayer(Context app) {
        this.app = app;
        this.prefs = new AppPrefs(app);
    }

    private void ensure() {
        int t = Math.max(0, Math.min(TRACKS.length - 1, prefs.bgmTrack()));
        if (mp != null && loadedTrack == t) return;
        release();
        try {
            mp = MediaPlayer.create(app, TRACKS[t]);
            if (mp != null) {
                mp.setLooping(true);
                loadedTrack = t;
                applyVolume();
            }
        } catch (Exception e) {
            mp = null;
        }
    }

    private void applyVolume() {
        if (mp != null) {
            float v = prefs.bgmVolume() / 100f;
            mp.setVolume(v, v);
        }
    }

    public void resume() {
        if (!prefs.bgmEnabled()) return;
        try {
            ensure();
            if (mp != null && !mp.isPlaying()) mp.start();
        } catch (Exception ignored) {
        }
    }

    public void pause() {
        try {
            if (mp != null && mp.isPlaying()) mp.pause();
        } catch (Exception ignored) {
        }
    }

    public boolean isEnabled() { return prefs.bgmEnabled(); }

    public void setEnabled(boolean on) {
        prefs.setBgm(on);
        if (on) resume(); else pause();
    }

    /** Toggle mute cepat. Mengembalikan status terbaru (true = musik menyala). */
    public boolean toggle() {
        setEnabled(!prefs.bgmEnabled());
        return prefs.bgmEnabled();
    }

    public void setTrack(int track) {
        prefs.setBgmTrack(track);
        if (prefs.bgmEnabled()) resume();
    }

    public void setVolume(int percent) {
        prefs.setBgmVolume(percent);
        applyVolume();
    }

    private void release() {
        if (mp != null) {
            try { mp.release(); } catch (Exception ignored) { }
            mp = null;
            loadedTrack = -1;
        }
    }
}
