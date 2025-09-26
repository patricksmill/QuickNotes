package com.example.quicknotes.model;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.RequiresPermission;
import androidx.preference.PreferenceManager;

import com.example.quicknotes.R;
import com.example.quicknotes.controller.NotificationReceiver;
import com.google.android.material.snackbar.Snackbar;

import java.util.Date;

public class Notifier {
    private final Context ctx;
    private View rootView; // For Snackbar display

    public static final String channelID = "channel1";
    public static final String titleExtra = "titleExtra";
    public static final String messageExtra = "messageExtra";

    public Notifier(Context ctx) {
        this.ctx = ctx;
    }

    /**
     * Sets the root view for displaying Snackbar messages.
     * @param rootView The root view to anchor Snackbar messages to
     */
    public void setRootView(View rootView) {
        this.rootView = rootView;
    }

    /**
     * Shows a message using Snackbar if rootView is available, otherwise falls back to basic logging.
     */
    private void showMessage(String message, int duration) {
        if (rootView != null) {
            Snackbar.make(rootView, message, duration).show();
        }
        // If no root view available, the message won't be shown
        // This is better than using Toast which doesn't fit the app's design
    }

    private SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public boolean globalNotificationsAllowed() {
        return getPrefs().getBoolean("pref_noti", false);
    }

    /**
     * Checks if the app has permission to schedule exact alarms.
     * @return true if permission is granted, false otherwise
     */
    public boolean canScheduleExactAlarms() {
        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        return alarmManager != null && alarmManager.canScheduleExactAlarms();
    }

    /**
     * Opens the system settings to request exact alarm permission.
     */
    public void requestExactAlarmPermission() {
        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            ctx.startActivity(intent);
            showMessage(ctx.getString(R.string.alarm_permission_request), Snackbar.LENGTH_LONG);
        } catch (Exception e) {
            showMessage(ctx.getString(R.string.alarm_permission_error), Snackbar.LENGTH_LONG);
        }
    }

    /**
     * Validates that the notification date is not in the past.
     * @param notifyDate The date to validate
     * @return true if the date is valid (in the future), false otherwise
     */
    public boolean isValidNotificationDate(Date notifyDate) {
        if (notifyDate == null) return false;
        return notifyDate.after(new Date());
    }

    /**
     * Cancels any existing notification for the given note.
     * @param note The note whose notification should be canceled
     */
    public void cancelNotification(Note note) {
        if (note == null) return;

        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        try {
            Intent intent = new Intent(ctx, NotificationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    ctx,
                    note.getId().hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        } catch (Exception e) {
            // Silent failure - notification may not have been scheduled
        }
    }

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    public void scheduleNotification(Note note) {
        // Always cancel any existing notification first
        cancelNotification(note);

        if (!globalNotificationsAllowed() || !note.isNotificationsEnabled()) {
            return;
        }

        Date notifyDate = note.getNotificationDate();
        if (!isValidNotificationDate(notifyDate)) {
            if (notifyDate != null) {
                // Show error for past dates
                showMessage("Cannot schedule notification for past date/time", Snackbar.LENGTH_LONG);
            }
            return;
        }

        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            showMessage(ctx.getString(R.string.alarm_service_error), Snackbar.LENGTH_SHORT);
            return;
        }

        if (!canScheduleExactAlarms()) {
            requestExactAlarmPermission();
            return;
        }

        try {
            Intent intent = new Intent(ctx, NotificationReceiver.class);
            intent.putExtra(titleExtra, note.getTitle());
            intent.putExtra(messageExtra, note.getContent());
            intent.putExtra("noteId", note.getId());

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    ctx,
                    note.getId().hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    notifyDate.getTime(),
                    pendingIntent
            );
            
            showMessage(ctx.getString(R.string.notification_scheduled), Snackbar.LENGTH_SHORT);
        } catch (SecurityException e) {
            showMessage(ctx.getString(R.string.alarm_permission_error), Snackbar.LENGTH_LONG);
            requestExactAlarmPermission();
        } catch (Exception e) {
            showMessage(ctx.getString(R.string.notification_schedule_error), Snackbar.LENGTH_SHORT);
        }
    }

}

