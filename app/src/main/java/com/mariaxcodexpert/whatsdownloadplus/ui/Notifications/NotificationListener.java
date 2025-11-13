package com.mariaxcodexpert.whatsdownloadplus.ui.Notifications;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationListener extends NotificationListenerService {

    private static final String TAG = "NotifListener";

    @Override
    public void onListenerConnected() {
        Log.d(TAG, "Notification listener connected ✅");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        Log.d(TAG, "Notification posted: " + packageName);

        // Only handle WhatsApp notifications
        if ("com.whatsapp".equals(packageName)) {
            String sender = sbn.getNotification().extras.getString("android.title");
            CharSequence messageCs = sbn.getNotification().extras.getCharSequence("android.text");
            String message = messageCs != null ? messageCs.toString() : "";
            long timestamp = sbn.getPostTime();

            // Ignore system/call notifications
            if (NotificationsFragment.shouldIgnoreNotification(message)) {
                Log.d(TAG, "Ignored system notification: " + message);
                return;
            }

            // Save to database (duplicates automatically ignored)
            NotificationDatabaseHelper dbHelper = new NotificationDatabaseHelper(getApplicationContext());
            dbHelper.insertNotification(sender, message, timestamp);
            Log.d(TAG, "Saved WhatsApp notification: " + sender + " -> " + message);

            // Optional: cancel original WhatsApp notification to prevent double display
            cancelNotification(sbn.getKey());
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(TAG, "Notification removed: " + sbn.getPackageName());
    }
}
