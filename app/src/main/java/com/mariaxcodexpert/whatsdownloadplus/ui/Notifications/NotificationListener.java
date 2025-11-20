package com.mariaxcodexpert.whatsdownloadplus.ui.Notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class NotificationListener extends NotificationListenerService {

    private static final String TAG = "NotifListener";
    private static final String CHANNEL_ID = "listener_service_channel";

    @Override
    public void onListenerConnected() {
        Log.d(TAG, "Notification listener connected ✅");
        startForegroundServiceSafe();
    }

    private void startForegroundServiceSafe() {
        try {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Notification Listener Service",
                        NotificationManager.IMPORTANCE_LOW
                );
                manager.createNotificationChannel(channel);
            }

            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Listening for WhatsApp Messages")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .build();

            startForeground(1, notification);
        } catch (Exception e) {
            Log.e(TAG, "Error starting foreground service: " + e.getMessage());
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!"com.whatsapp".equals(sbn.getPackageName())) return;

        Bundle extras = sbn.getNotification().extras;
        String sender = extras.getString("android.title");
        CharSequence text = extras.getCharSequence("android.text");
        if (text == null) text = extras.getCharSequence("android.bigText");
        if (text == null) {
            CharSequence[] lines = extras.getCharSequenceArray("android.textLines");
            if (lines != null && lines.length > 0) text = lines[lines.length - 1];
        }
        String message = text != null ? text.toString() : "";
        long timestamp = sbn.getPostTime();

        // Save to DB
        NotificationDatabaseHelper dbHelper = new NotificationDatabaseHelper(getApplicationContext());
        dbHelper.insertNotification(sender, message, timestamp);

        // Load active keywords from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("tracker_prefs", MODE_PRIVATE);
        Set<String> keywords = prefs.getStringSet("keywords_set", new HashSet<>());
        String activeKeyword = prefs.getString("active_keyword", "");

        // Only broadcast if message contains any keyword
        boolean matches = false;
        for (String kw : keywords) {
            if (message.toLowerCase().contains(kw.toLowerCase())) {
                matches = true;
                break;
            }
        }

        if (matches) {
            Intent intent = new Intent("com.mariaxcodexpert.NEW_NOTIFICATION");
            intent.putExtra("sender", sender);
            intent.putExtra("message", message);
            intent.putExtra("timestamp", timestamp);
            intent.putExtra("active_keyword", activeKeyword);
            sendBroadcast(intent);
        }
    }



    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(TAG, "Notification removed: " + sbn.getPackageName());
    }
}
