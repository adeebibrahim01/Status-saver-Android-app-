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

        // Registering the observer
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        // AdManager initialization - App khulte hi pehla ad load hoga
        AdManager.init(this);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
        isInForeground = true;
        Log.d(TAG, "App in FOREGROUND");

        // Agar user app se bahar ja kar wapas aaye aur ad loaded na ho, to try karein
        if (!AdManager.isAdLoaded()) {
            AdManager.preloadAd(this);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStop(owner);
        isInForeground = false;
        Log.d(TAG, "App in BACKGROUND");
    }

    public static boolean isAppInForeground() {
        return isInForeground;
    }
}