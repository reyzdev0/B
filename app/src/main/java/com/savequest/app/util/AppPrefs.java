package com.savequest.app.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Semua pengaturan user (Settings). */
public class AppPrefs {
    private final SharedPreferences sp;

    public AppPrefs(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    public boolean soundEnabled() { return sp.getBoolean("sound", true); }
    public void setSound(boolean v) { sp.edit().putBoolean("sound", v).apply(); }

    public boolean hapticEnabled() { return sp.getBoolean("haptic", true); }
    public void setHaptic(boolean v) { sp.edit().putBoolean("haptic", v).apply(); }

    /** 1 = lembut, 2 = sedang, 3 = kuat. */
    public int hapticLevel() { return sp.getInt("haptic_level", 2); }
    public void setHapticLevel(int v) { sp.edit().putInt("haptic_level", v).apply(); }

    public boolean reminderEnabled() { return sp.getBoolean("reminder", false); }
    public void setReminder(boolean v) { sp.edit().putBoolean("reminder", v).apply(); }
    public int reminderHour() { return sp.getInt("reminder_h", 19); }
    public int reminderMinute() { return sp.getInt("reminder_m", 0); }
    public void setReminderTime(int h, int m) { sp.edit().putInt("reminder_h", h).putInt("reminder_m", m).apply(); }

    public boolean biometricEnabled() { return sp.getBoolean("biometric", true); }
    public void setBiometric(boolean v) { sp.edit().putBoolean("biometric", v).apply(); }

    public boolean bgmEnabled() { return sp.getBoolean("bgm", true); }
    public void setBgm(boolean v) { sp.edit().putBoolean("bgm", v).apply(); }
    public int bgmTrack() { return sp.getInt("bgm_track", 0); }
    public void setBgmTrack(int v) { sp.edit().putInt("bgm_track", v).apply(); }
    /** 0..100 */
    public int bgmVolume() { return sp.getInt("bgm_volume", 40); }
    public void setBgmVolume(int v) { sp.edit().putInt("bgm_volume", v).apply(); }

    /** Sumber update: "pemilik/repo" GitHub, atau URL JSON lengkap. Kosong = belum diatur. */
    public String updateSource() { return sp.getString("update_source", ""); }
    public void setUpdateSource(String v) { sp.edit().putString("update_source", v.trim()).apply(); }
    public boolean updateAuto() { return sp.getBoolean("update_auto", true); }
    public void setUpdateAuto(boolean v) { sp.edit().putBoolean("update_auto", v).apply(); }
    public long lastUpdateCheck() { return sp.getLong("update_last", 0); }
    public void setLastUpdateCheck(long v) { sp.edit().putLong("update_last", v).apply(); }
}
