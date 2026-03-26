package com.mariaxcodexpert.whatsdownloadplus.model;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
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
        Context context = getApplicationContext();

        Log.d("NotificationWorker", "Worker started for Status ID: " + statusId);

        // Security Checks
        if (StatusStorage.isNotified(context, statusId, type)) return Result.success();
        if (expiryTime <= System.currentTimeMillis()) return Result.success();


        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("openFragment", "ImagesAndVideo");
        intent.putExtra("isVideo", isVideo);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Notification Bhejna
        PushNotificationHelper helper = new PushNotificationHelper(context);
        helper.sendNotification("Status Expiring Soon", "A status will expire in 1 hour. View it now!", intent, statusId);

        // Mark as notified taake baar baar na aaye
        StatusStorage.markAsNotified(context, statusId, type);

        return Result.success();
    }
}