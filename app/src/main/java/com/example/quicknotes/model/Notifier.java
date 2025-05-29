package com.example.quicknotes.model;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.preference.PreferenceManager;

import com.example.quicknotes.R;
import com.example.quicknotes.controller.NotificationReceiver;

import java.util.Date;

public class Notifier {
    private final Context ctx;

    public static final String channelID = "channel1";
    public static final String titleExtra = "titleExtra";
    public static final String messageExtra = "messageExtra";

    public Notifier(Context ctx) {
        this.ctx = ctx;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true; // No permission needed for Android 11 and below
    }

    /**
     * Opens the system settings to request exact alarm permission.
     */
    public void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                ctx.startActivity(intent);
                Toast.makeText(ctx, R.string.alarm_permission_request, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(ctx, R.string.alarm_permission_error, Toast.LENGTH_LONG).show();
            }
        }
    }

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    public void scheduleNotification(Note note) {
        if (!globalNotificationsAllowed() || !note.isNotificationsEnabled()) {
            return;
        }

        Date notifyDate = note.getNotificationDate();
        if (notifyDate == null || notifyDate.before(new Date())) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Toast.makeText(ctx, R.string.alarm_service_error, Toast.LENGTH_SHORT).show();
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

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    ctx,
                    note.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    notifyDate.getTime(),
                    pendingIntent
            );
            
            Toast.makeText(ctx, R.string.notification_scheduled, Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(ctx, R.string.alarm_permission_error, Toast.LENGTH_LONG).show();
            requestExactAlarmPermission();
        } catch (Exception e) {
            Toast.makeText(ctx, R.string.notification_schedule_error, Toast.LENGTH_SHORT).show();
        }
    }
}

