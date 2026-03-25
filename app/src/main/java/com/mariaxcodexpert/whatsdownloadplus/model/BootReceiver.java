package com.mariaxcodexpert.whatsdownloadplus.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.Map;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Map<String, ?> all = StatusStorage.getAllStatuses(context);
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                String key = entry.getKey();
                if (key.contains("_notified")) continue;

                try {
                    int id = Integer.parseInt(key);
                    long expiry = StatusStorage.getExpiryTime(context, id);
                    boolean isVid = StatusStorage.isVideo(context, id);

                    if (expiry > System.currentTimeMillis()) {
                        NotificationScheduler.scheduleNotification(context, id, expiry, isVid, 1);
                        NotificationScheduler.scheduleNotification(context, id, expiry, isVid, 2);
                    }
                } catch (Exception ignored) {}
            }
        }
    }
}