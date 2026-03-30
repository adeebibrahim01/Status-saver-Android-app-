package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

/**
 * Android 10+ Optimized Application Class.
 * Handled with Modern Lifecycle Observers for Ads and Visibility.
 */
public class MyApp extends Application implements DefaultLifecycleObserver {

    private static final String TAG = "MyApp_StatusSaver";
    private static boolean isInForeground = false;

    @Override
    public void onCreate() {
        super.onCreate();

        // App ki lifecycle track karne ke liye (Ads ke liye zaroori hai)
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        // AdManager ko start-up par initialize karna
        AdManager.init(this);

        Log.d(TAG, "Application Started - MariaXCodeExpert");
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        isInForeground = true;
        Log.d(TAG, "App Status: FOREGROUND");

        // Background se wapas aane par Ad load karna taake user ko delay na mile
        if (AdManager.canRequestAds() && !AdManager.isAdLoaded()) {
            AdManager.preloadAd(this);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        isInForeground = false;
        Log.d(TAG, "App Status: BACKGROUND");
    }

    public static boolean isAppInForeground() {
        return isInForeground;
    }
}