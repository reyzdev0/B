package com.savequest.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.savequest.app.audio.BgmPlayer;
import com.savequest.app.ui.lock.LockActivity;
import com.savequest.app.util.Session;

/**
 * Dasar semua layar yang dilindungi: blokir screenshot, wajib lolos lock screen,
 * kunci ulang saat app ke background, dan kendali musik latar.
 */
public abstract class SecureActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!Session.unlocked) {
            startActivity(new Intent(this, LockActivity.class));
            finish();
            return;
        }
        Session.clearExternal();
        BgmPlayer.get(this).resume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isChangingConfigurations() || Session.isSuppressed()) return;
        Session.unlocked = false;
        BgmPlayer.get(this).pause();
    }

    /** Buka Activity lain di dalam app tanpa memicu kunci ulang. */
    public void startInternal(Intent intent) {
        Session.markInternal();
        startActivity(intent);
    }

    @Override
    public void finish() {
        Session.markInternal();
        super.finish();
    }
}
