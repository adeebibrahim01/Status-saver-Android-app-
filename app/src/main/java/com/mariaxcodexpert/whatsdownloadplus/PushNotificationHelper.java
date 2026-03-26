package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class PushNotificationHelper {
    private final Context context;
    private static final String CHANNEL_ID = "status_expiry_channel";

    public PushNotificationHelper(Context context) {
        this.context = context;
        createChannel();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Status Expiry Alerts",
                    NotificationManager.IMPORTANCE_HIGH); // 🔥 Level 4 Importance (Pop-up)

            channel.setDescription("Alerts for WhatsApp status expiry");
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.enableVibration(true);
            // Lock screen par content dikhane ke liye
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    public void sendNotification(String title, String message, Intent intent, int id) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int flags = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                ? (PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)
                : PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent pi = PendingIntent.getActivity(context, id, intent, flags);

        // Professional Touch: Default notification sound set karna
        android.net.Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri) // Sound add kiya
                .setVibrate(new long[]{1000, 1000, 1000}) // Vibration pattern
                .setPriority(NotificationCompat.PRIORITY_MAX) // 🔥 MAX Priority for Heads-up
                .setCategory(NotificationCompat.CATEGORY_ALARM) // System ko batata hai ke ye urgent hai
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pi);

        if (nm != null) {
            nm.notify(id, builder.build());
            Log.d("PushNotificationHelper", "High Priority notification sent. ID: " + id);
        }
    }
}