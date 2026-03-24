package com.mariaxcodexpert.whatsdownloadplus.model;

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

    public NotificationWorker(@NonNull android.content.Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        int statusId = getInputData().getInt(KEY_STATUS_ID, -1);
        long expiryTime = getInputData().getLong(KEY_EXPIRY_TIME, -1L);
        boolean isVideo = getInputData().getBoolean(KEY_IS_VIDEO, false);
        int type = getInputData().getInt(KEY_TYPE, TYPE_1_HOUR);

        long now = System.currentTimeMillis();

        if (statusId <= 0 || expiryTime <= now || StatusStorage.isNotified(getApplicationContext(), statusId)) {
            Log.w(TAG, "Worker skipped | statusId=" + statusId + " | type=" + type);
            return Result.success();
        }

        long minutesLeft = TimeUnit.MILLISECONDS.toMinutes(expiryTime - now);
        if ((type == TYPE_1_HOUR && minutesLeft > 61) || (type == TYPE_30_MIN && minutesLeft > 31)) {
            Log.w(TAG, "WorkManager fired too early, ignored");
            return Result.success();
        }

        Intent openIntent = new Intent(getApplicationContext(), MainActivity.class);
        openIntent.putExtra("openFragment", "ImagesAndVideo");
        openIntent.putExtra("isVideo", isVideo);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        String title = (type == TYPE_1_HOUR) ? "Status Expiring Soon" : "Last Chance!";
        String message = (type == TYPE_1_HOUR) ?
                "This status will expire in about 1 hour. View it now." :
                "Only 30 minutes left before this status disappears.";

        new PushNotificationHelper(getApplicationContext())
                .sendNotification(title, message, openIntent, generateNotificationId(statusId, type));

        StatusStorage.markAsNotified(getApplicationContext(), statusId);

        Log.i(TAG, "Notification sent | statusId=" + statusId + " | type=" + type + " | minutesLeft=" + minutesLeft);

        return Result.success();
    }

    private int generateNotificationId(int statusId, int type) {
        return Math.abs(statusId * 10 + type);
    }
}
