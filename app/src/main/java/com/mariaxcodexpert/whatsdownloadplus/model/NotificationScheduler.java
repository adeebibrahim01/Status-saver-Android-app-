package com.mariaxcodexpert.whatsdownloadplus.model;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class NotificationScheduler {

    private static final String TAG = "NotificationScheduler";

    public static final int TYPE_1_HOUR = 1;
    public static final int TYPE_30_MIN = 2;

    public static void scheduleNotification(
            @NonNull Context context,
            int statusId,
            long expiryTime,
            boolean isVideo,
            int type
    ) {
        long now = System.currentTimeMillis();

        if (expiryTime <= now) {
            cancelOldNotification(context, statusId);
            return;
        }

        long triggerTime = expiryTime - (type == TYPE_1_HOUR ? 60 * 60 * 1000 : 30 * 60 * 1000);
        long delay = triggerTime - now;

        if (delay <= 5_000) {
            Log.i(TAG, "Too late to schedule notification for statusId " + statusId);
            return;
        }

        String uniqueWorkName = "status_notification_" + statusId + "_" + type;

        Data inputData = new Data.Builder()
                .putInt(NotificationWorker.KEY_STATUS_ID, statusId)
                .putLong(NotificationWorker.KEY_EXPIRY_TIME, expiryTime)
                .putBoolean(NotificationWorker.KEY_IS_VIDEO, isVideo)
                .putInt(NotificationWorker.KEY_TYPE, type)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .setInputData(inputData)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager workManager = WorkManager.getInstance(context);

        workManager.getWorkInfosForUniqueWorkLiveData(uniqueWorkName)
                .observeForever(workInfos -> {
                    boolean alreadyScheduled = false;
                    if (workInfos != null) {
                        for (WorkInfo info : workInfos) {
                            if (info.getState() == WorkInfo.State.ENQUEUED
                                    || info.getState() == WorkInfo.State.RUNNING) {
                                alreadyScheduled = true;
                                break;
                            }
                        }
                    }
                    if (!alreadyScheduled) {
                        workManager.enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.KEEP, workRequest);
                        Log.i(TAG, "WorkManager notification scheduled for statusId " + statusId + " | type=" + type);
                    }
                });

        scheduleAlarmFallback(context, statusId, expiryTime, isVideo, type);
    }

    private static void scheduleAlarmFallback(@NonNull Context context, int statusId, long expiryTime, boolean isVideo, int type) {
        long triggerTime = expiryTime - (type == TYPE_1_HOUR ? 60 * 60 * 1000 : 30 * 60 * 1000);
        if (triggerTime <= System.currentTimeMillis()) return;

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra(NotificationWorker.KEY_STATUS_ID, statusId);
        intent.putExtra(NotificationWorker.KEY_EXPIRY_TIME, expiryTime);
        intent.putExtra(NotificationWorker.KEY_IS_VIDEO, isVideo);
        intent.putExtra(NotificationWorker.KEY_TYPE, type);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                statusId + type, // unique per statusId + type
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0)
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                Log.i(TAG, "AlarmManager fallback scheduled for statusId " + statusId + " | type=" + type);
            } catch (SecurityException e) {
                e.printStackTrace();
                Log.e(TAG, "AlarmManager scheduling failed for statusId " + statusId, e);
            }
        }
    }

    public static void cancelOldNotification(@NonNull Context context, int statusId) {
        WorkManager.getInstance(context).cancelUniqueWork("status_notification_" + statusId);

        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                statusId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0)
        );
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}
