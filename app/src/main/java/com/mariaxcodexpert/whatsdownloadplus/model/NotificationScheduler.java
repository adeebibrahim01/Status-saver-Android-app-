package com.mariaxcodexpert.whatsdownloadplus.model;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public class NotificationScheduler {
    public static final int TYPE_1_HOUR = 1;
    public static final int TYPE_30_MIN = 2;

    public static void scheduleNotification(@NonNull Context context, int statusId, long expiryTime, boolean isVideo, int type) {
        long now = System.currentTimeMillis();
        long triggerTime = expiryTime - (type == TYPE_1_HOUR ? 60 * 60 * 1000 : 30 * 60 * 1000);
        long delay = triggerTime - now;

        if (delay <= 0) return; // Time nikal chuka hai

        String uniqueWorkName = "status_notify_" + statusId + "_" + type;

        Data inputData = new Data.Builder()
                .putInt("statusId", statusId)
                .putLong("expiryTime", expiryTime)
                .putBoolean("isVideo", isVideo)
                .putInt("type", type)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.KEEP,
                workRequest
        );
    }
}