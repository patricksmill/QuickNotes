package com.example.quicknotes.model

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.view.View
import androidx.annotation.RequiresPermission
import androidx.preference.PreferenceManager
import com.example.quicknotes.R
import com.example.quicknotes.controller.NotificationReceiver
import com.google.android.material.snackbar.Snackbar
import java.util.Date

class Notifier(private val ctx: Context) {
    private var rootView: View? = null // For Snackbar display

    /**
     * Sets the root view for displaying Snackbar messages.
     * @param rootView The root view to anchor Snackbar messages to
     */
    fun setRootView(rootView: View?) {
        this.rootView = rootView
    }

    /**
     * Shows a message using Snackbar if rootView is available, otherwise falls back to basic logging.
     */
    private fun showMessage(message: String, duration: Int) {
        if (rootView != null) {
            Snackbar.make(rootView!!, message, duration).show()
        }
        // If no root view available, the message won't be shown
        // This is better than using Toast which doesn't fit the app's design
    }

    private val prefs: SharedPreferences?
        get() = PreferenceManager.getDefaultSharedPreferences(ctx)

    fun globalNotificationsAllowed(): Boolean {
        return this.prefs!!.getBoolean("pref_noti", false)
    }

    /**
     * Checks if the app has permission to schedule exact alarms.
     * @return true if permission is granted, false otherwise
     */
    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
            return alarmManager != null && alarmManager.canScheduleExactAlarms()
        }
        return true // No permission needed for Android 11 and below
    }

    /**
     * Opens the system settings to request exact alarm permission.
     */
    fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            try {
                ctx.startActivity(intent)
                showMessage(ctx.getString(R.string.alarm_permission_request), Snackbar.LENGTH_LONG)
            } catch (_: Exception) {
                showMessage(ctx.getString(R.string.alarm_permission_error), Snackbar.LENGTH_LONG)
            }
        }
    }

    /**
     * Validates that the notification date is not in the past.
     * @param notifyDate The date to validate
     * @return true if the date is valid (in the future), false otherwise
     */
    fun isValidNotificationDate(notifyDate: Date?): Boolean {
        if (notifyDate == null) return false
        return notifyDate.after(Date())
    }

    /**
     * Cancels any existing notification for the given note.
     * @param note The note whose notification should be canceled
     */
    fun cancelNotification(note: Note?) {
        if (note == null) return

        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
        if (alarmManager == null) return

        try {
            val intent = Intent(ctx, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                ctx,
                note.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (_: Exception) {
            // Silent failure - notification may not have been scheduled
        }
    }

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    fun scheduleNotification(note: Note) {
        // Always cancel any existing notification first
        cancelNotification(note)

        if (!globalNotificationsAllowed() || !note.isNotificationsEnabled) {
            return
        }

        val notifyDate = note.notificationDate
        if (!isValidNotificationDate(notifyDate)) {
            if (notifyDate != null) {
                // Show error for past dates
                showMessage("Cannot schedule notification for past date/time", Snackbar.LENGTH_LONG)
            }
            return
        }

        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
        if (alarmManager == null) {
            showMessage(ctx.getString(R.string.alarm_service_error), Snackbar.LENGTH_SHORT)
            return
        }

        if (!canScheduleExactAlarms()) {
            requestExactAlarmPermission()
            return
        }

        try {
            val intent = Intent(ctx, NotificationReceiver::class.java)
            intent.putExtra(titleExtra, note.title)
            intent.putExtra(messageExtra, note.content)
            intent.putExtra("noteId", note.id)

            val pendingIntent = PendingIntent.getBroadcast(
                ctx,
                note.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                notifyDate!!.time,
                pendingIntent
            )

            showMessage(ctx.getString(R.string.notification_scheduled), Snackbar.LENGTH_SHORT)
        } catch (_: SecurityException) {
            showMessage(ctx.getString(R.string.alarm_permission_error), Snackbar.LENGTH_LONG)
            requestExactAlarmPermission()
        } catch (_: Exception) {
            showMessage(ctx.getString(R.string.notification_schedule_error), Snackbar.LENGTH_SHORT)
        }
    }

    /**
     * Updates notification scheduling for a note.
     * This method handles both scheduling new notifications and canceling existing ones.
     * @param note The note to update notifications for
     * @param enabled Whether notifications should be enabled
     * @param date The notification date (can be null if disabled)
     */
    fun updateNotification(note: Note, enabled: Boolean, date: Date?) {
        note.isNotificationsEnabled = enabled
        note.notificationDate = date

        if (enabled && date != null) {
            scheduleNotification(note)
        } else {
            cancelNotification(note)
        }
    }

    companion object {
        const val channelID: String = "channel1"
        const val titleExtra: String = "titleExtra"
        const val messageExtra: String = "messageExtra"
    }
}

