package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mariaxcodexpert.whatsdownloadplus.Ads.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.ui.language.LanguageManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyApp extends Application implements DefaultLifecycleObserver {

    private static final String TAG = "WhatsDownload_App";

    public static final ExecutorService appExecutor =
            Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));

    @Override
    public void onCreate() {
        super.onCreate();


        LanguageManager.initAppLanguage(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        createNotificationChannel();
        appExecutor.execute(() -> {
            try {

                FirebaseMessaging.getInstance().getToken()
                        .addOnCompleteListener(task -> Log.d(TAG, "FCM Token Ready"));
   new android.os.Handler(android.os.Looper.getMainLooper())
                        .post(() -> AdManager.init(this));

                SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
                AdManager.isPremiumUser = prefs.getBoolean("isPremium", false);

            } catch (Exception e) {
                Log.e(TAG, "Init Error: " + e.getMessage());
            }
        });
    }

    private void createNotificationChannel() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel statusChannel = new NotificationChannel(
                "status_alerts_channel", "Status Expiry Alerts", NotificationManager.IMPORTANCE_HIGH);
        statusChannel.setDescription("Notifications for statuses expiring soon");
        statusChannel.enableLights(true);
        statusChannel.setLightColor(Color.YELLOW);

        NotificationChannel generalChannel = new NotificationChannel(
                "general_updates", "App Updates & Tips", NotificationManager.IMPORTANCE_DEFAULT);

        manager.createNotificationChannel(statusChannel);
        manager.createNotificationChannel(generalChannel);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        Log.d(TAG, "App in Foreground");
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        Log.d(TAG, "App in Background");
    }
}