package com.mariaxcodexpert.whatsdownloadplus.ui.peekmode.service;

import android.app.Notification;
import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase;
import com.mariaxcodexpert.whatsdownloadplus.data.local.PeekMessageEntity.PeekMessageEntity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MyNotificationListener extends NotificationListenerService {

    private static final long DEBOUNCE_THRESHOLD = 5000;
    private static String lastSavedMessageHash = "";
    private static long lastSavedTimestamp = 0;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        SharedPreferences settings = getSharedPreferences("peek_settings", MODE_PRIVATE);
        if (!settings.getBoolean("is_peek_on", false)) return;
        String packageName = sbn.getPackageName();
        if (!"com.whatsapp".equals(packageName) && !"com.whatsapp.w4b".equals(packageName)) return;
        Notification notification = sbn.getNotification();
        if (notification == null || notification.extras == null) return;

        String title = notification.extras.getString("android.title");
        String text = notification.extras.getString("android.text", "");

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(text) ||
                "WhatsApp".equals(title) || text.contains("new messages") || text.contains("messages")) return;

        long currentTime = System.currentTimeMillis();
        String currentHash = title + ":" + text;

        if (currentHash.equals(lastSavedMessageHash) && (currentTime - lastSavedTimestamp < DEBOUNCE_THRESHOLD)) return;

        lastSavedMessageHash = currentHash;
        lastSavedTimestamp = currentTime;
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            String currentUid = db.profileDao().getFirstUserUid();

            if (currentUid != null) {
                PeekMessageEntity entity = new PeekMessageEntity();
                entity.senderName = title;
                entity.messageBody = text;
                entity.timestamp = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
                entity.unreadCount = 1;
                entity.userId = currentUid;
                entity.createdAt = System.currentTimeMillis();

                db.peekDao().insertMessage(entity);

                Data syncData = new Data.Builder()
                        .putString("uid", currentUid)
                        .putString("sender", title)
                        .putString("body", text)
                        .putString("timestamp", entity.timestamp)
                        .putLong("createdAt", entity.createdAt)
                        .build();

                OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                        .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                        .setInputData(syncData)
                        .build();

                WorkManager.getInstance(getApplicationContext()).enqueue(syncRequest);
            }
        }).start();
    }
}