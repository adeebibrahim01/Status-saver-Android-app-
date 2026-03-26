package com.mariaxcodexpert.whatsdownloadplus.model;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public class NotificationScheduler {
    private static final String TAG = "NotificationScheduler";
    public static final int TYPE_1_HOUR = 1;

    public static void schedule(@NonNull Context context, int statusId, long expiryTime, boolean isVideo) {
        // 🔥 FIX 1: Check karein ke kya ye status pehle hi notify ho chuka hai?
        // Agar notify ho chuka hai, toh dobara schedule karne ki zaroorat nahi.
        if (StatusStorage.isNotified(context, statusId, TYPE_1_HOUR)) {
            return;
        }

        // 🔥 FIX 2: Check karein ke kya iska expiry time guzar toh nahi gaya?
        if (expiryTime <= System.currentTimeMillis()) {
            return;
        }

        scheduleNotification(context, statusId, expiryTime, isVideo, TYPE_1_HOUR);
    }

    public static void scheduleNotification(@NonNull Context context, int statusId, long expiryTime, boolean isVideo, int type) {
        long now = System.currentTimeMillis();
        long triggerTime = expiryTime - (60 * 60 * 1000L);
        long delay = triggerTime - now;

        // Smart Delay Logic
        if (delay <= 0) {
            if (expiryTime > now) {
                delay = 5000; // 5 seconds
            } else {
                return;
            }
        }

        String uniqueWorkName = "status_notify_" + statusId;

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

        // 🔥 FIX 3: KEEP use karein REPLACE ki jagah.
        // KEEP ka matlab hai: Agar "status_notify_123" pehle se queue mein hai,
        // toh naya wala ignore kar do. Isse purana timer distrub nahi hoga
        // aur CPU power bachegi.
        WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.KEEP,
                workRequest
        );

        Log.d(TAG, "Tried scheduling ID: " + statusId + ". WorkManager will KEEP existing or add new.");
    }
}