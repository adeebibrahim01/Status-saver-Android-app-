package com.mariaxcodexpert.whatsdownloadplus.model;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Map;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Check if the intent is BOOT_COMPLETED
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(TAG, "Device reboot detected. Rescheduling notifications...");

            // 1. Android 12+ Safety: Check if we can schedule exact alarms
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                    Log.e(TAG, "Cannot reschedule: Exact alarm permission not granted by user.");
                    return;
                }
            }

            long now = System.currentTimeMillis();
            Map<String, ?> allStatuses = StatusStorage.getAllStatuses(context);

            if (allStatuses == null || allStatuses.isEmpty()) {
                Log.i(TAG, "No statuses found to reschedule.");
                return;
            }

            for (Map.Entry<String, ?> entry : allStatuses.entrySet()) {
                try {
                    String key = entry.getKey();

                    // Skip the "_notified" flags or null keys
                    if (key == null || key.endsWith("_notified")) continue;

                    int statusId = Integer.parseInt(key);
                    long expiryTime = StatusStorage.getExpiryTime(context, statusId);
                    boolean isVideo = StatusStorage.isVideo(context, statusId);

                    // 2. Remove expired statuses (Safi logic)
                    if (expiryTime <= now) {
                        StatusStorage.removeStatus(context, statusId);
                        Log.i(TAG, "Removed expired statusId=" + statusId);
                        continue;
                    }

                    // 3. Reschedule Notifications
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

                    Log.i(TAG, "Rescheduled successfully for statusId=" + statusId);

                } catch (NumberFormatException e) {
                    // Ignore keys that are not integers (like string settings)
                    Log.w(TAG, "Skipping non-integer key: " + entry.getKey());
                } catch (Exception e) {
                    Log.e(TAG, "Error rescheduling statusId=" + entry.getKey(), e);
                }
            }
        }
    }
}