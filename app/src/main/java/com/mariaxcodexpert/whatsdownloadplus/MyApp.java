package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

public class MyApp extends Application implements DefaultLifecycleObserver {

    private static boolean isInForeground = false;

    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        isInForeground = true;
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        isInForeground = false;
    }

    public static boolean isAppInForeground() {
        return isInForeground;
    }
}
