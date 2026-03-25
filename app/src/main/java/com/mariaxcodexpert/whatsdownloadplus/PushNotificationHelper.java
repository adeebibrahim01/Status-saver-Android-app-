package com.mariaxcodexpert.whatsdownloadplus;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

public class PushNotificationHelper {

    private static final String CHANNEL_ID = "status_expiry_channel"; // Unique ID
    private static final String CHANNEL_NAME = "Status Expiry Alerts";
    private static final String CHANNEL_DESC = "Notifications for statuses about to expire";

    private final Context context;

    public PushNotificationHelper(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // High importance zaroori hai heads-up notification ke liye
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public void sendNotification(String title, String message, Intent intent, int notificationId) {
        // 1. Android 13+ Permission Check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return; // Permission nahi hai to return kar jao
            }
        }

        // 2. PendingIntent setup (Modern Flags)
        PendingIntent pendingIntent = null;
        if (intent != null) {
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE; // Safety for Android 12+
            }

            pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId, // Har notification ka unique request code
                    intent,
                    flags
            );
        }

        // 3. Build Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Ensure this icon exists
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message)) // Expandable text
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent);
        }

        // 4. Show Notification
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notificationId, builder.build());
        }
    }
}