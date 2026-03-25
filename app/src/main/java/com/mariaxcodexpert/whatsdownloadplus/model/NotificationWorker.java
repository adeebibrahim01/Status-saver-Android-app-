package com.mariaxcodexpert.whatsdownloadplus.model;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.mariaxcodexpert.whatsdownloadplus.MainActivity;
import com.mariaxcodexpert.whatsdownloadplus.PushNotificationHelper;

import java.util.concurrent.TimeUnit;

public class NotificationWorker extends Worker {

    private static final String TAG = "NotificationWorker";

    public static final String KEY_STATUS_ID = "statusId";
    public static final String KEY_EXPIRY_TIME = "expiryTime";
    public static final String KEY_IS_VIDEO = "isVideo";
    public static final String KEY_TYPE = "type";

    public static final int TYPE_1_HOUR = 1;
    public static final int TYPE_30_MIN = 2;

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Input data retrieve karein
        int statusId = getInputData().getInt(KEY_STATUS_ID, -1);
        long expiryTime = getInputData().getLong(KEY_EXPIRY_TIME, -1L);
        boolean isVideo = getInputData().getBoolean(KEY_IS_VIDEO, false);
        int type = getInputData().getInt(KEY_TYPE, TYPE_1_HOUR);

        long now = System.currentTimeMillis();

        // 1. Updated Validation: Ab hum specific 'type' check kar rahe hain
        if (statusId <= 0 || expiryTime <= now || StatusStorage.isNotified(getApplicationContext(), statusId, type)) {
            Log.w(TAG, "Worker skipped | statusId=" + statusId + " | type=" + type + " | Already notified or expired");
            return Result.success();
        }

        long minutesLeft = TimeUnit.MILLISECONDS.toMinutes(expiryTime - now);

        // 2. Safety Window: WorkManager hamesha exact time par nahi chalta
        // Isliye hum window ko thoda broad rakhte hain
        if (type == TYPE_1_HOUR && (minutesLeft > 70 || minutesLeft < 45)) {
            Log.w(TAG, "WorkManager fired outside 1-hour window, ignored. Minutes left: " + minutesLeft);
            return Result.success();
        }
        if (type == TYPE_30_MIN && (minutesLeft > 40 || minutesLeft < 10)) {
            Log.w(TAG, "WorkManager fired outside 30-min window, ignored. Minutes left: " + minutesLeft);
            return Result.success();
        }

        // 3. Prepare Intent
        Intent openIntent = new Intent(getApplicationContext(), MainActivity.class);
        openIntent.putExtra("openFragment", "ImagesAndVideo");
        openIntent.putExtra("isVideo", isVideo);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // 4. Dynamic Content
        String title = (type == TYPE_1_HOUR) ? "Status Expiring Soon" : "Last Chance!";
        String message = (type == TYPE_1_HOUR) ?
                "This status will expire in about 1 hour. View it now." :
                "Only 30 minutes left before this status disappears.";

        try {
            // 5. Send Notification
            new PushNotificationHelper(getApplicationContext())
                    .sendNotification(title, message, openIntent, generateNotificationId(statusId, type));

            // 6. Updated Mark: Ab specific 'type' ko mark karein
            StatusStorage.markAsNotified(getApplicationContext(), statusId, type);

            Log.i(TAG, "Notification sent | statusId=" + statusId + " | type=" + type + " | minutesLeft=" + minutesLeft);
        } catch (Exception e) {
            Log.e(TAG, "Error in Worker while sending notification", e);
            return Result.retry(); // Agar koi error aaye to dobara koshish karein
        }

        return Result.success();
    }

    private int generateNotificationId(int statusId, int type) {
        return Math.abs(statusId * 10 + type);
    }
}