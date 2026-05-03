package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.work.Configuration;
import androidx.work.WorkManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyApp extends Application implements DefaultLifecycleObserver, Configuration.Provider {
    private static final String TAG = "WhatsDownload_MyApp";

    private final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors())
    );

    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        backgroundExecutor.execute(() -> {
            try {
                // WorkManager ko refresh karna taake background jobs active rahein
                WorkManager.getInstance(this);

                new Handler(Looper.getMainLooper()).post(() -> {
                    AdManager.init(this);
                });
            } catch (Exception e) {
                Log.e(TAG, "Init Error: " + e.getMessage());
            }
        });
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        // Standard configuration bina kisi extra complication ke
        return new Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .build();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) { Log.d(TAG, "App: FOREGROUND"); }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) { Log.d(TAG, "App: BACKGROUND"); }
}