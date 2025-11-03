package io.github.patricksmill.quicknotes.controller

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.github.patricksmill.quicknotes.R
import io.github.patricksmill.quicknotes.model.Notifier
import io.github.patricksmill.quicknotes.model.note.Note
import io.github.patricksmill.quicknotes.model.note.NoteLibrary
import java.util.function.Consumer

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (ACTION_DISMISS == action) {
            // Handle dismiss action
            val notificationId = intent.getIntExtra("notificationId", -1)
            if (notificationId != -1) {
                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
                manager?.cancel(notificationId)
            }
            return
        } else if (ACTION_DELETE == action) {
            val noteId = intent.getStringExtra("noteId")
            if (noteId != null) {
                val noteLibrary = NoteLibrary(context)
                noteLibrary.getNotes().stream()
                    .filter { note: Note? -> noteId == note!!.id }
                    .findFirst()
                    .ifPresent(Consumer { note: Note? -> noteLibrary.deleteNote(note!!) })
            }
            val notificationId = intent.getIntExtra("notificationId", -1)
            if (notificationId != -1) {
                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
                manager?.cancel(notificationId)
            }
            return
        }

        // Default behavior - show notification with actions
        val title = intent.getStringExtra(Notifier.titleExtra)
        val content = intent.getStringExtra(Notifier.messageExtra)
        val noteId = intent.getStringExtra("noteId")
        val notificationId = System.currentTimeMillis().toInt()

        // Create channel
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        if (manager != null) {
            val channel = NotificationChannel(
                Notifier.channelID,
                "QuickNotes Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Reminders for your notes"
            manager.createNotificationChannel(channel)
        }

        // Create intent to view the note - directly launch activity to avoid trampoline
        val viewNoteIntent = Intent(context, ControllerActivity::class.java)
        viewNoteIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        viewNoteIntent.putExtra("noteId", noteId)
        viewNoteIntent.putExtra("action", "viewNote")
        val viewPendingIntent = PendingIntent.getActivity(
            context,
            notificationId * 2,
            viewNoteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create intent to delete the note
        val deleteIntent = Intent(context, NotificationReceiver::class.java)
        deleteIntent.action = ACTION_DELETE
        deleteIntent.putExtra("notificationId", notificationId)
        deleteIntent.putExtra(Notifier.titleExtra, title)
        deleteIntent.putExtra("noteId", noteId)
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 3,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create intent to dismiss the notification
        val dismissIntent = Intent(context, NotificationReceiver::class.java)
        dismissIntent.action = ACTION_DISMISS
        dismissIntent.putExtra("notificationId", notificationId)
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 2 + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create the main tap intent to open the app
        val mainIntent = Intent(context, ControllerActivity::class.java)
        mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        mainIntent.putExtra("noteId", noteId)
        mainIntent.putExtra("action", "viewNote")
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, Notifier.channelID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_visibility, "View Note", viewPendingIntent)
            .addAction(R.drawable.ic_delete, "Delete", deletePendingIntent)
            .addAction(R.drawable.ic_close, "Dismiss", dismissPendingIntent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(content)
                    .setBigContentTitle(title)
                    .setSummaryText("QuickNotes Reminder")
            )

        manager?.notify(notificationId, builder.build())
    }

    companion object {
        private const val ACTION_DISMISS = "io.github.patricksmill.quicknotes.ACTION_DISMISS"
        private const val ACTION_DELETE = "io.github.patricksmill.quicknotes.ACTION_DELETE"
    }
}
