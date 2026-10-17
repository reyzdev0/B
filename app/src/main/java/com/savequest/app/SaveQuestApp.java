package com.savequest.app;

import android.app.Application;

import com.savequest.app.reminder.ReminderScheduler;

public class SaveQuestApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ReminderScheduler.ensureChannel(this);
        ReminderScheduler.schedule(this);
    }
}
