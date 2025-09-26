package com.example.quicknotes.model;

import android.content.Context;
import java.util.Date;

public class NotificationRepository {
    private final Notifier notifier;

    public NotificationRepository(Context context) {
        Context applicationContext = context.getApplicationContext();
        this.notifier = new Notifier(applicationContext);
    }

    public void scheduleNotification(Note note) {
        if (note != null && note.isNotificationsEnabled() && note.getNotificationDate() != null) {
            notifier.scheduleNotification(note);
        }
    }

    public void cancelNotification(Note note) {
        if (note != null) {
            notifier.cancelNotification(note);
        }
    }

    public boolean isValidNotificationDate(Date date) {
        return notifier.isValidNotificationDate(date);
    }

}
