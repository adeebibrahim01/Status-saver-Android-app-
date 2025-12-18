package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Application;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        StatusWatcherWorker.scheduleWork(this);
    }
}
