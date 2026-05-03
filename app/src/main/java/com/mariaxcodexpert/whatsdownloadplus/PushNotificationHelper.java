package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class PushNotificationHelper {
    private final Context context;
    // 🟢 Version change karne se settings refresh ho jati hain
    private static final String CHANNEL_ID = "status_alerts_high_priority_v2";

    public PushNotificationHelper(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 🔴 Importance HIGH rakha hai taake Heads-up (pop-up) notification aaye
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Status Expiry Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription("Critical alerts for saving statuses before they expire.");
            channel.enableLights(true);
            channel.setLightColor(ContextCompat.getColor(context, R.color.primary_color));
            channel.enableVibration(true);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            // Sound set karna background reliability ke liye behtar hai
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(defaultSoundUri, audioAttributes);

            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
                Log.d("NotifHelper", "✅ High Priority Channel Created");
            }
        }
    }

    public void sendNotification(String title, String message, Intent intent, int id) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pi = PendingIntent.getActivity(context, id, intent, flags);

        // 🟢 Build Notification with MAX Priority
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Ensure this is a white silhouette icon
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message)) // Expandable text
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setPriority(NotificationCompat.PRIORITY_MAX) // 🔴 Max priority for background
                .setCategory(NotificationCompat.CATEGORY_REMINDER) // OS knows it's a reminder
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setColor(ContextCompat.getColor(context, R.color.primary_color))
                .setContentIntent(pi)
                .setOnlyAlertOnce(false); // Baar-baar alert kar sake agar zaroori ho

        // 🔴 Extra Trick: Infinix/Tecno ke liye "FullScreenIntent" trigger (Optional but powerful)
        // builder.setFullScreenIntent(pi, true);

        Log.d("NotifHelper", "🚀 Firing notification for ID: " + id);
        nm.notify(id, builder.build());
    }
}