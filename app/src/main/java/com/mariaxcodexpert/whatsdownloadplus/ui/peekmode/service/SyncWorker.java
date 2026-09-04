package com.mariaxcodexpert.whatsdownloadplus.ui.peekmode.service;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorkerDebug";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String uid = getInputData().getString("uid");
        String sender = getInputData().getString("sender");
        String body = getInputData().getString("body");
        String time = getInputData().getString("timestamp");
        long createdAt = getInputData().getLong("createdAt", 0);

        // Check if inputs are missing
        if (uid == null || sender == null) {
            Log.e(TAG, "Sync failed: UID or Sender is null! uid=" + uid + ", sender=" + sender);
            return Result.failure();
        }

        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance("https://status-saver-92d48-default-rtdb.firebaseio.com/");
            String safeSender = sender.replaceAll("[./#\\[\\]$]", "_");

            Map<String, Object> data = new HashMap<>();
            data.put("messageBody", body != null ? body : "");
            data.put("timestamp", time != null ? time : "");
            data.put("createdAt", createdAt);

            DatabaseReference messageRef = database.getReference("users")
                    .child(uid)
                    .child("chats")
                    .child(safeSender)
                    .child("messages")
                    .push();

            Log.e(TAG, "Attempting to push data to path: " + messageRef.toString());

            // Tasks.await() background thread par firebase write hone ka wait karega
            Tasks.await(messageRef.setValue(data), 15, TimeUnit.SECONDS);

            Log.e(TAG, "Data successfully synced to Firebase Realtime Database!");
            return Result.success();

        } catch (Exception e) {
            // Yahan exact error Logcat mein print ho jayega (e.g. Permission denied, Network error, etc.)
            Log.e(TAG, "Firebase Sync Exception Error: " + e.getMessage(), e);
            return Result.retry();
        }
    }
}