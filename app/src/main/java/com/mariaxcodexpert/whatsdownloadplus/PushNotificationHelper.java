package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
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
        // 🔥 Android 10 (API 29) mein Notification Channels lazmi hain.
        // Hum "if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)" nikaal sakte hain agar minSDK 26+ ho.
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Status Expiry Alerts",
                NotificationManager.IMPORTANCE_HIGH); // Heads-up notification ke liye High Importance

        channel.setDescription("Alerts for WhatsApp status expiry");
        channel.enableLights(true);
        channel.setLightColor(Color.GREEN);
        channel.enableVibration(true);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.createNotificationChannel(channel);
        }
    }

    public void sendNotification(String title, String message, Intent intent, int id) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 🔥 CLEANUP: SDK 29+ par PendingIntent.FLAG_IMMUTABLE hamesha chahiye hota hai.
        // Purana Marshmallow (M) wala check nikaal diya hai.
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        PendingIntent pi = PendingIntent.getActivity(context, id, intent, flags);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setVibrate(new long[]{0, 500, 200, 500}) // Standard vibration pattern
                .setPriority(NotificationCompat.PRIORITY_MAX) // Heads-up display trigger karega
                .setCategory(NotificationCompat.CATEGORY_EVENT) // Expiry alerts ke liye EVENT category behtar hai
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pi);

        if (nm != null) {
            nm.notify(id, builder.build());
            Log.d("PushNotificationHelper", "Modern High Priority Notification Sent. ID: " + id);
        }
    }
}