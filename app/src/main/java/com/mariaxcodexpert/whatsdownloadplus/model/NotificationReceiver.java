package com.mariaxcodexpert.whatsdownloadplus.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mariaxcodexpert.whatsdownloadplus.MainActivity;
import com.mariaxcodexpert.whatsdownloadplus.PushNotificationHelper;

import java.util.concurrent.TimeUnit;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationReceiver";

    public static final int TYPE_1_HOUR = 1;
    public static final int TYPE_30_MIN = 2;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        // Note: Make sure NotificationWorker keys match exactly what you send from NotificationScheduler
        int statusId = intent.getIntExtra("statusId", -1);
        long expiryTime = intent.getLongExtra("expiryTime", -1L);
        boolean isVideo = intent.getBooleanExtra("isVideo", false);
        int type = intent.getIntExtra("type", TYPE_1_HOUR);

        long now = System.currentTimeMillis();

        // 1. Basic Validation
        if (statusId <= 0 || expiryTime <= now) return;

        // 2. Optimized Notified Check (Har type ke liye alag check hona chahiye)
        // Agar status pehle hi notified hai (for this specific type), to return karein
        if (StatusStorage.isNotified(context, statusId, type)) return;

        long minutesLeft = TimeUnit.MILLISECONDS.toMinutes(expiryTime - now);

        // 3. Safety Window: AlarmManager kabhi kabhi thoda jaldi ya dair se chalta hai
        if (type == TYPE_1_HOUR && (minutesLeft > 70 || minutesLeft < 45)) return;
        if (type == TYPE_30_MIN && (minutesLeft > 40 || minutesLeft < 10)) return;

        // 4. Notification Intent Setup
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.putExtra("openFragment", "ImagesAndVideo");
        openIntent.putExtra("isVideo", isVideo);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // 5. Dynamic Content
        String title = (type == TYPE_1_HOUR) ? "Status Expiring Soon" : "Last Chance!";
        String message = (type == TYPE_1_HOUR) ?
                "This status will expire in about 1 hour. View it now." :
                "Only 30 minutes left before this status disappears.";

        // 6. Execution
        try {
            new PushNotificationHelper(context)
                    .sendNotification(title, message, openIntent, generateNotificationId(statusId, type));

            // Mark this SPECIFIC type as notified
            StatusStorage.markAsNotified(context, statusId, type);

            Log.i(TAG, "Notification sent | statusId=" + statusId + " | type=" + type + " | minutesLeft=" + minutesLeft);
        } catch (Exception e) {
            Log.e(TAG, "Error sending notification", e);
        }
    }

    private int generateNotificationId(int statusId, int type) {
        // Unique ID for each status + type combination
        return Math.abs(statusId * 10 + type);
    }
}