package com.savequest.app.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Menjadwalkan ulang pengingat setelah HP restart. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            ReminderScheduler.schedule(context);
        }
    }
}
