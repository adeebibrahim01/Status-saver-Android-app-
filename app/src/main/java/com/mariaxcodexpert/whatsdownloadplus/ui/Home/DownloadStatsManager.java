package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo.SavedFilesDB;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class DownloadStatsManager {

    private final SavedFilesDB savedFilesDB; // reuse the existing DB
    private final Set<Long> cache = new HashSet<>();
    private OnDatabaseChangeListener listener;

    public DownloadStatsManager(Context context, SavedFilesDB savedFilesDB) {
        this.savedFilesDB = savedFilesDB;
        loadCache(); // load from existing table safely
    }

    /** Listener interface */
    public interface OnDatabaseChangeListener {
        void onDatabaseChanged();
    }

    public void setOnDatabaseChangeListener(OnDatabaseChangeListener listener) {
        this.listener = listener;
    }

    /** Load cache safely from saved_files table */
    private synchronized void loadCache() {
        cache.clear();
        try {
            Set<String> files = savedFilesDB.getAllSavedFiles();
            for (String fileName : files) {
                // convert file_name to timestamp if needed, or just use index
                // Here, assuming you store timestamp in file_name format, otherwise skip this
                try {
                    cache.add(Long.parseLong(fileName));
                } catch (NumberFormatException ignored) {}
            }
        } catch (Exception ignored) {
            // table missing → treat as empty
            cache.clear();
        }
        notifyChange();
    }

    public synchronized void addDownload(long timestamp) {
        if (timestamp <= 0) timestamp = System.currentTimeMillis();
        if (cache.contains(timestamp)) return;

        // Use the savedFilesDB to store as string
        savedFilesDB.addFile(String.valueOf(timestamp));
        cache.add(timestamp);
        notifyChange();
    }

    public synchronized void removeDownload(long timestamp) {
        if (!cache.contains(timestamp)) return;

        savedFilesDB.removeFile(String.valueOf(timestamp));
        cache.remove(timestamp);
        notifyChange();
    }

    public int getTodayDownloads() {
        long[] range = getTodayTimeRange();
        return getCountInRange(range[0], range[1]);
    }

    public int getLast7DaysDownloads() {
        long now = System.currentTimeMillis();
        long sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000;
        return getCountInRange(sevenDaysAgo, now);
    }

    private synchronized int getCountInRange(long startTime, long endTime) {
        int count = 0;
        for (Long time : cache) {
            if (time >= startTime && time <= endTime) count++;
        }
        return count;
    }

    private long[] getTodayTimeRange() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MILLISECOND, -1);
        long end = cal.getTimeInMillis();

        return new long[]{start, end};
    }

    private void notifyChange() {
        if (listener != null) {
            new Handler(Looper.getMainLooper()).post(listener::onDatabaseChanged);
        }
    }
}
