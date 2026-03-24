package com.mariaxcodexpert.whatsdownloadplus.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Map;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(TAG, "Device reboot detected. Rescheduling notifications...");

            long now = System.currentTimeMillis();
            Map<String, ?> allStatuses = StatusStorage.getAllStatuses(context);

            for (Map.Entry<String, ?> entry : allStatuses.entrySet()) {
                try {
                    // Skip the "_notified" flags
                    if (entry.getKey().endsWith("_notified")) continue;

                    int statusId = Integer.parseInt(entry.getKey());
                    long expiryTime = StatusStorage.getExpiryTime(context, statusId);
                    boolean isVideo = StatusStorage.isVideo(context, statusId);

                    // Remove expired statuses
                    if (expiryTime <= now) {
                        StatusStorage.removeStatus(context, statusId);
                        continue;
                    }

                    // Schedule 1-hour notification
                    NotificationScheduler.scheduleNotification(
                            context,
                            statusId,
                            expiryTime,
                            isVideo,
                            NotificationScheduler.TYPE_1_HOUR
                    );

                    // Schedule 30-min notification
                    NotificationScheduler.scheduleNotification(
                            context,
                            statusId,
                            expiryTime,
                            isVideo,
                            NotificationScheduler.TYPE_30_MIN
                    );

                    Log.i(TAG, "Rescheduled notifications for statusId=" + statusId +
                            " | type=" + (isVideo ? "video" : "image"));

                } catch (Exception e) {
                    Log.e(TAG, "Error rescheduling statusId=" + entry.getKey(), e);
                }
            }
        }
    }
}
