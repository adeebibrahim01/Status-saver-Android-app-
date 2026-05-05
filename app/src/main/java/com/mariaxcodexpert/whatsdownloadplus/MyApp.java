package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyApp extends Application implements DefaultLifecycleObserver {
    private static final String TAG = "WhatsDownload_MyApp";

    private final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors())
    );

    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        // 1. Notification Channel create karna (Oreo aur upar ke liye zaroori hai)
        createNotificationChannel();

        backgroundExecutor.execute(() -> {
            try {
                // AdManager initialization
                new Handler(Looper.getMainLooper()).post(() -> {
                    AdManager.init(this);
                });
            } catch (Exception e) {
                Log.e(TAG, "Init Error: " + e.getMessage());
            }
        });
    }

    private void createNotificationChannel() {
        // Notification channel sirf Android Oreo (API 26) aur us se upar ke liye chahiye
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "status_alerts_channel"; // Ye ID GitHub script se match karti hai
            CharSequence name = "Status Expiry Alerts";
            String description = "Notifications for statuses expiring in 1 hour";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.setLightColor(Color.YELLOW); // Golden/Yellow theme color
            channel.enableVibration(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification Channel Created Successfully");
            }
        }
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        Log.d(TAG, "App: FOREGROUND");
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        Log.d(TAG, "App: BACKGROUND");
    }
}