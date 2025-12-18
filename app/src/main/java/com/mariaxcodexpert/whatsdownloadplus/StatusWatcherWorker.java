package com.mariaxcodexpert.whatsdownloadplus;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.WorkManager;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class StatusWatcherWorker extends Worker {

    private static final String TAG = "StatusWatcherWorker";
    private static final String CHANNEL_ID = "status_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final String UNIQUE_WORK_NAME = "StatusWatcherWork";

    private static final boolean DEBUG = true;
    private static Set<String> knownStatuses = new HashSet<>();

    public StatusWatcherWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        debugLog("Worker started");

        if (!hasRequiredPermissions(context)) {
            debugLog("Permissions missing, retrying...");
            return Result.retry();
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkStatusesAndroid13(context);
            } else {
                checkStatusesLegacy();
            }
            debugLog("Worker finished successfully");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error in Worker", e);
            return Result.retry();
        }
    }

    private boolean hasRequiredPermissions(Context context) {
        boolean hasPermissions;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermissions = ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            hasPermissions = ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }

        debugLog(hasPermissions ? "All required permissions granted ✅"
                : "Missing required permissions ❌");

        return hasPermissions;
    }

    private void checkStatusesLegacy() {
        String statusDir = "/WhatsApp/Media/.Statuses/";
        File folder = new File(Environment.getExternalStorageDirectory() + statusDir);

        if (!folder.exists() || !folder.isDirectory()) {
            debugLog("Legacy folder not found: " + folder.getAbsolutePath());
            return;
        }

        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            debugLog("No files found in legacy folder.");
            return;
        }

        for (File file : files) {
            String name = file.getName();
            if (!knownStatuses.contains(name)) {
                knownStatuses.add(name);
                debugLog("New legacy status: " + name);
                sendNotification("New WhatsApp Status", "New status: " + name);
            } else {
                debugLog("Already known status: " + name);
            }
        }
    }

    private void checkStatusesAndroid13(Context context) {
        String[] projection = new String[]{MediaStore.Images.Media.DISPLAY_NAME};
        String selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
        String[] selectionArgs = new String[]{"WhatsApp/Media/.Statuses/%"};

        try (android.database.Cursor cursor = context.getContentResolver()
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null)) {

            if (cursor == null || cursor.getCount() == 0) {
                debugLog("No files found in MediaStore for Android 13+");
                return;
            }

            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                if (!knownStatuses.contains(name)) {
                    knownStatuses.add(name);
                    debugLog("New status (Android 13+): " + name);
                    sendNotification("New WhatsApp Status", "New status: " + name);
                } else {
                    debugLog("Already known status (Android 13+): " + name);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading MediaStore", e);
        }
    }

    private void sendNotification(String title, String content) {
        NotificationManager manager = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "WhatsApp Status Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for new WhatsApp statuses");
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (manager != null) {
            manager.notify(NOTIFICATION_ID + content.hashCode(), builder.build());
        }
        debugLog("Notification sent: " + content);
    }

    private void debugLog(String message) {
        if (DEBUG) {
            Log.d(TAG, message);
        }
    }

    public static void scheduleWork(Context context) {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                StatusWatcherWorker.class,
                15, TimeUnit.MINUTES)
                .setInitialDelay(5, TimeUnit.SECONDS) // debug quickly
                .setConstraints(new Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build())
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }
}
