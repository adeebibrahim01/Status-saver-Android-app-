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

        int statusId = intent.getIntExtra(NotificationWorker.KEY_STATUS_ID, -1);
        long expiryTime = intent.getLongExtra(NotificationWorker.KEY_EXPIRY_TIME, -1L);
        boolean isVideo = intent.getBooleanExtra(NotificationWorker.KEY_IS_VIDEO, false);
        int type = intent.getIntExtra(NotificationWorker.KEY_TYPE, TYPE_1_HOUR);

        long now = System.currentTimeMillis();
        if (statusId <= 0 || expiryTime <= now || StatusStorage.isNotified(context, statusId)) return;

        long minutesLeft = TimeUnit.MILLISECONDS.toMinutes(expiryTime - now);

        // Safety check: only fire when close
        if ((type == TYPE_1_HOUR && minutesLeft > 61) ||
                (type == TYPE_30_MIN && minutesLeft > 31)) return;

        // Open app intent
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.putExtra("openFragment", "ImagesAndVideo");
        openIntent.putExtra("isVideo", isVideo);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Notification content
        String title = (type == TYPE_1_HOUR) ? "Status Expiring Soon" : "Last Chance!";
        String message = (type == TYPE_1_HOUR) ?
                "This status will expire in about 1 hour. View it now." :
                "Only 30 minutes left before this status disappears.";

        // Send notification
        new PushNotificationHelper(context)
                .sendNotification(title, message, openIntent, generateNotificationId(statusId, type));

        // Mark as notified
        StatusStorage.markAsNotified(context, statusId);

        Log.i(TAG, "Notification sent | statusId=" + statusId + " | type=" + type + " | minutesLeft=" + minutesLeft);
    }

    private int generateNotificationId(int statusId, int type) {
        return Math.abs(statusId * 10 + type);
    }
}
