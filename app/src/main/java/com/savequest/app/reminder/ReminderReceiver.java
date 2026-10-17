package com.savequest.app.reminder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.savequest.app.R;
import com.savequest.app.data.local.AppDatabase;
import com.savequest.app.data.local.entity.PlayerEntity;
import com.savequest.app.domain.StreakSystem;
import com.savequest.app.ui.lock.LockActivity;
import com.savequest.app.util.AppPrefs;
import com.savequest.app.util.DateUtil;

/**
 * Smart reminder: hanya berbunyi jika hari ini belum ada setoran,
 * dan isi pesannya menyesuaikan kondisi streak user.
 */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final Context app = context.getApplicationContext();
        final PendingResult result = goAsync();
        new Thread(() -> {
            try {
                if (!new AppPrefs(app).reminderEnabled()) return;
                AppDatabase db = AppDatabase.get(app);
                long today = DateUtil.today();
                long sumToday = db.transactionDao().depositSumSync(today, today);
                if (sumToday == 0) notifyUser(app, db.playerDao().get(), today);
            } catch (Exception ignored) {
            } finally {
                ReminderScheduler.schedule(app);
                result.finish();
            }
        }).start();
    }

    @SuppressLint("MissingPermission")
    private void notifyUser(Context app, PlayerEntity p, long today) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(app,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;

        String title = "Waktunya menabung!";
        String text = "Setor sedikit hari ini dan selesaikan quest hariannya.";
        if (p != null) {
            int streak = StreakSystem.effective(p.streak, p.lastDepositDay, today);
            if (streak > 0) {
                title = "Streak " + streak + " hari terancam putus";
                text = "Setor hari ini untuk menjaga multiplier "
                        + StreakSystem.label(StreakSystem.multiplier(streak + 1)) + " EXP.";
            }
        }
        ReminderScheduler.ensureChannel(app);
        PendingIntent open = PendingIntent.getActivity(app, 0, new Intent(app, LockActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new NotificationCompat.Builder(app, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bolt)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(open)
                .build();
        NotificationManagerCompat.from(app).notify(2001, n);
    }
}
