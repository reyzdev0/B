package com.savequest.app.reminder;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.savequest.app.util.AppPrefs;

import java.util.Calendar;

/** Mengatur alarm harian. Memakai alarm inexact agar tidak butuh izin exact-alarm. */
public final class ReminderScheduler {
    private ReminderScheduler() {}

    public static final String CHANNEL_ID = "daily_reminder";

    public static void ensureChannel(Context c) {
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Pengingat harian",
                NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("Pengingat untuk menabung setiap hari");
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(ch);
    }

    private static PendingIntent pending(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class);
        return PendingIntent.getBroadcast(c, 1001, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void schedule(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        AppPrefs p = new AppPrefs(c);
        if (!p.reminderEnabled()) {
            am.cancel(pending(c));
            return;
        }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, p.reminderHour());
        cal.set(Calendar.MINUTE, p.reminderMinute());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pending(c));
    }

    public static void cancel(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am != null) am.cancel(pending(c));
    }
}
