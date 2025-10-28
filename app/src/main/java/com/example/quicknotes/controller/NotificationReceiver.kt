package com.example.quicknotes.controller;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.example.quicknotes.R;
import com.example.quicknotes.model.NoteLibrary;
import com.example.quicknotes.model.Notifier;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String ACTION_DISMISS = "com.example.quicknotes.ACTION_DISMISS";
    private static final String ACTION_DELETE = "com.example.quicknotes.ACTION_DELETE";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (ACTION_DISMISS.equals(action)) {
            // Handle dismiss action
            int notificationId = intent.getIntExtra("notificationId", -1);
            if (notificationId != -1) {
                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.cancel(notificationId);
                }
            }
            return;
        } else if (ACTION_DELETE.equals(action)) {
            String noteId = intent.getStringExtra("noteId");
            if (noteId != null) {
                NoteLibrary noteLibrary = new NoteLibrary(context);
                noteLibrary.getNotes().stream()
                        .filter(note -> noteId.equals(note.getId()))
                        .findFirst()
                        .ifPresent(noteLibrary::deleteNote);
            }
            int notificationId = intent.getIntExtra("notificationId", -1);
            if (notificationId != -1) {
                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.cancel(notificationId);
                }
            }
            return;
        }

        // Default behavior - show notification with actions
        String title = intent.getStringExtra(Notifier.titleExtra);
        String content = intent.getStringExtra(Notifier.messageExtra);
        String noteId = intent.getStringExtra("noteId");
        int notificationId = (int) System.currentTimeMillis();

        // Create channel
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            NotificationChannel channel = new NotificationChannel(
                    Notifier.channelID,
                    "QuickNotes Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Reminders for your notes");
            manager.createNotificationChannel(channel);
        }

        // Create intent to view the note - directly launch activity to avoid trampoline
        Intent viewNoteIntent = new Intent(context, ControllerActivity.class);
        viewNoteIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        viewNoteIntent.putExtra("noteId", noteId);
        viewNoteIntent.putExtra("action", "viewNote");
        PendingIntent viewPendingIntent = PendingIntent.getActivity(
                context,
                notificationId * 2,
                viewNoteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create intent to delete the note
        Intent deleteIntent = new Intent(context, NotificationReceiver.class);
        deleteIntent.setAction(ACTION_DELETE);
        deleteIntent.putExtra("notificationId", notificationId);
        deleteIntent.putExtra(Notifier.titleExtra, title);
        deleteIntent.putExtra("noteId", noteId);
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId * 3,
                deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create intent to dismiss the notification
        Intent dismissIntent = new Intent(context, NotificationReceiver.class);
        dismissIntent.setAction(ACTION_DISMISS);
        dismissIntent.putExtra("notificationId", notificationId);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId * 2 + 1,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create the main tap intent to open the app
        Intent mainIntent = new Intent(context, ControllerActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mainIntent.putExtra("noteId", noteId);
        mainIntent.putExtra("action", "viewNote");
        PendingIntent mainPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Notifier.channelID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(mainPendingIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_visibility, "View Note", viewPendingIntent)
                .addAction(R.drawable.ic_delete, "Delete", deletePendingIntent)
                .addAction(R.drawable.ic_close, "Dismiss", dismissPendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(content)
                        .setBigContentTitle(title)
                        .setSummaryText("QuickNotes Reminder"));

        if (manager != null) {
            manager.notify(notificationId, builder.build());
        }
    }
}
