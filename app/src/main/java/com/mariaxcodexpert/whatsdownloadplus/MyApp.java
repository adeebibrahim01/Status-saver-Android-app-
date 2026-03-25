package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

public class MyApp extends Application implements DefaultLifecycleObserver {

    private static final String TAG = "MyApp";
    private static boolean isInForeground = false;

    @Override
    public void onCreate() {
        super.onCreate();

        // Registering the observer to track app-level lifecycle
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        // Yahan aap Ads ya Firebase initialize kar sakte hain
        // AdManager.init(this);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
        isInForeground = true;
        Log.d(TAG, "App moved to FOREGROUND");
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStop(owner);
        isInForeground = false;
        Log.d(TAG, "App moved to BACKGROUND");
    }

    /**
     * Helper method to check app state from any class
     */
    public static boolean isAppInForeground() {
        return isInForeground;
    }
}