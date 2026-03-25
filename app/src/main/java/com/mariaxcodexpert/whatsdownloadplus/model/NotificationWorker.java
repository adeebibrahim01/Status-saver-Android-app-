package com.mariaxcodexpert.whatsdownloadplus.model;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.mariaxcodexpert.whatsdownloadplus.MainActivity;
import com.mariaxcodexpert.whatsdownloadplus.PushNotificationHelper;

public class NotificationWorker extends Worker {

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        int statusId = getInputData().getInt("statusId", -1);
        long expiryTime = getInputData().getLong("expiryTime", -1L);
        boolean isVideo = getInputData().getBoolean("isVideo", false);
        int type = getInputData().getInt("type", 1);

        if (StatusStorage.isNotified(getApplicationContext(), statusId, type)) return Result.success();
        if (expiryTime <= System.currentTimeMillis()) return Result.success();

        String title = (type == 1) ? "Status Expiring Soon" : "Last Chance!";
        String message = (type == 1) ? "A status will expire in 1 hour. View it now!" : "Only 30 minutes left before this status disappears.";

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra("openFragment", "ImagesAndVideo");
        intent.putExtra("isVideo", isVideo);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        new PushNotificationHelper(getApplicationContext())
                .sendNotification(title, message, intent, (statusId * 10 + type));

        StatusStorage.markAsNotified(getApplicationContext(), statusId, type);
        return Result.success();
    }
}