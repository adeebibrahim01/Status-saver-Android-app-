package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageDao;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoDao;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseCleaner {

    private static final String PREF_NAME = "cleanup_prefs";
    private static final String KEY_LAST_CLEANUP = "last_cleanup_date";

    public static void performSilentCleanup(Context context, ImageDao imgDao, VideoDao vidDao, List<String> activeFiles) {

        if (context == null || imgDao == null || vidDao == null) {
            Log.e("DB_CLEANER", "Cleanup aborted: Context or DAOs are null");
            return;
        }

        try {
            if (activeFiles == null || activeFiles.isEmpty()) {
                imgDao.clearAllUnsavedImages();
                vidDao.clearAllUnsavedVideos();
                Log.d("DB_CLEANER", "All unsaved records cleared (Empty Disk)");
            } else {
                imgDao.deleteGhostImages(activeFiles);
                vidDao.deleteGhostVideos(activeFiles);
                Log.d("DB_CLEANER", "Ghost records synced with disk");
            }

            SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

            String lastCleanup = prefs.getString(KEY_LAST_CLEANUP, "");

            if (!today.equals(lastCleanup)) {
                long weekAgo = System.currentTimeMillis() - 604800000L;

                imgDao.deleteOldRecords(weekAgo);
                vidDao.deleteOldRecords(weekAgo);
                prefs.edit().putString(KEY_LAST_CLEANUP, today).apply();
                Log.d("DB_CLEANER", "7-Day History Cleanup Done: " + today);
            }

        } catch (Exception e) {
            Log.e("DB_CLEANER", "Cleanup Error: " + e.getMessage());
        }
    }
}