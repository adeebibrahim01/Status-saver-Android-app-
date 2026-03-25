package com.mariaxcodexpert.whatsdownloadplus;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.WorkManager;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class StatusWatcherWorker extends Worker {

    private static final String TAG = "StatusWatcherWorker";
    private static final String PREFS_NAME = "WatcherPrefs";
    private static final String KEY_KNOWN_STATUSES = "known_statuses";
    private static final String KEY_STATUS_FOLDER_URI = "statusFolderUri";

    public StatusWatcherWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        Log.d(TAG, "Worker execution started");

        // 1. Get Saved Folder URI (Ensure key matches your Splash/Permission activity)
        SharedPreferences appPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String folderUriStr = appPrefs.getString(KEY_STATUS_FOLDER_URI, null);

        if (folderUriStr == null) {
            Log.w(TAG, "No folder URI found. User hasn't granted folder access yet.");
            return Result.success();
        }

        try {
            Uri folderUri = Uri.parse(folderUriStr);
            checkStatusesViaSAF(context, folderUri);
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error watching statuses", e);
            return Result.retry();
        }
    }

    private void checkStatusesViaSAF(Context context, Uri folderUri) {
        DocumentFile rootFolder = DocumentFile.fromTreeUri(context, folderUri);
        if (rootFolder == null || !rootFolder.exists()) return;

        DocumentFile[] files = rootFolder.listFiles();
        // Null check for safety
        if (files == null || files.length == 0) return;

        // 2. Load known statuses (Persistence)
        SharedPreferences watcherPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> knownStatuses = watcherPrefs.getStringSet(KEY_KNOWN_STATUSES, new HashSet<>());

        // Create a copy to avoid direct modification error
        Set<String> updatedSet = new HashSet<>(knownStatuses);
        boolean foundNew = false;

        PushNotificationHelper helper = new PushNotificationHelper(context);

        for (DocumentFile file : files) {
            String fileName = file.getName();

            // Skip hidden or nomedia files
            if (fileName == null || fileName.startsWith(".") || fileName.equals("nomedia")) continue;

            if (!knownStatuses.contains(fileName)) {
                foundNew = true;
                updatedSet.add(fileName);

                // 3. Notification Logic
                android.content.Intent intent = new android.content.Intent(context, MainActivity.class);
                intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);

                // Using absolute hash to ensure positive notification ID
                int notificationId = Math.abs(fileName.hashCode());

                helper.sendNotification(
                        "New Status Detected",
                        "Someone just posted a new status! Tap to view.",
                        intent,
                        notificationId
                );

                Log.d(TAG, "New status found: " + fileName);
            }
        }

        // 4. Save updated list
        if (foundNew) {
            watcherPrefs.edit().putStringSet(KEY_KNOWN_STATUSES, updatedSet).apply();
        }
    }

    public static void scheduleWork(Context context) {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                StatusWatcherWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(new Constraints.Builder()
                        .setRequiresStorageNotLow(true)
                        .setRequiresBatteryNotLow(true)
                        .build())
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "StatusWatcherWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }
}