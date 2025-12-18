package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SavedFilesDB {

    private final DBHelper dbHelper;
    private final Set<String> cache = Collections.synchronizedSet(new HashSet<>());

    // Stats counters
    private int todayCount = 0;
    private int last7DaysCount = 0;

    public SavedFilesDB(Context context) {
        dbHelper = new DBHelper(context.getApplicationContext());
        loadCache();
        calculateStats();
    }

    /* ================= CACHE ================= */
    private void loadCache() {
        cache.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor c = db.query(
                DBHelper.TABLE_NAME,
                new String[]{DBHelper.COLUMN_NAME},
                null, null, null, null, null
        )) {
            if (c == null) return;

            while (c.moveToNext()) {
                String id = c.getString(0);
                if (id != null) cache.add(id);
            }
        }
    }

    /* ================= CRUD ================= */
    public synchronized boolean addFile(String uniqueId) {
        if (uniqueId == null || cache.contains(uniqueId)) return false;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_NAME, uniqueId);
        long timestamp = System.currentTimeMillis();
        values.put(DBHelper.COLUMN_TIME, timestamp);

        try {
            long rowId = db.insertWithOnConflict(
                    DBHelper.TABLE_NAME,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_IGNORE
            );

            if (rowId != -1) {
                cache.add(uniqueId);
                // Update stats
                updateStatsOnAdd(timestamp);
                return true;
            }
        } catch (Exception ignored) {}

        return false;
    }

    public synchronized void removeFile(String fileName) {
        if (fileName == null) return;

        // Get timestamp before deletion
        Long timestamp = getTimestampForFile(fileName);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(
                DBHelper.TABLE_NAME,
                DBHelper.COLUMN_NAME + "=?",
                new String[]{fileName}
        );

        if (rows > 0) {
            cache.remove(fileName);
            // Update stats
            if (timestamp != null) updateStatsOnRemove(timestamp);
        }
    }

    private Long getTimestampForFile(String fileName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(DBHelper.TABLE_NAME,
                new String[]{DBHelper.COLUMN_TIME},
                DBHelper.COLUMN_NAME + "=?",
                new String[]{fileName}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                return c.getLong(0);
            }
        } catch (Exception ignored) {}
        return null;
    }

    public boolean isFileSaved(String fileName) {
        return fileName != null && cache.contains(fileName);
    }

    public Set<String> getAllSavedFiles() {
        return new HashSet<>(cache);
    }

    /* ================= STATS ================= */

    private void calculateStats() {
        todayCount = getTodayCountFromDB();
        last7DaysCount = getLast7DaysCountFromDB();
    }

    private void updateStatsOnAdd(long timestamp) {
        long startOfToday = getStartOfToday();
        long sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;

        if (timestamp >= startOfToday) {
            todayCount++;
            last7DaysCount++;
        } else if (timestamp >= sevenDaysAgo) {
            last7DaysCount++;
        }
    }

    private void updateStatsOnRemove(long timestamp) {
        long startOfToday = getStartOfToday();
        long sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;

        if (timestamp >= startOfToday) {
            todayCount = Math.max(0, todayCount - 1);
            last7DaysCount = Math.max(0, last7DaysCount - 1);
        } else if (timestamp >= sevenDaysAgo) {
            last7DaysCount = Math.max(0, last7DaysCount - 1);
        }
    }

    public int getTodayCount() {
        return todayCount;
    }

    public int getLast7DaysCount() {
        return last7DaysCount;
    }

    private int getTodayCountFromDB() {
        long startOfDay = getStartOfToday();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + DBHelper.TABLE_NAME +
                        " WHERE " + DBHelper.COLUMN_TIME + " >= ?",
                new String[]{String.valueOf(startOfDay)}
        )) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    private int getLast7DaysCountFromDB() {
        long sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + DBHelper.TABLE_NAME +
                        " WHERE " + DBHelper.COLUMN_TIME + " >= ?",
                new String[]{String.valueOf(sevenDaysAgo)}
        )) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    private long getStartOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /* ================= DB HELPER ================= */
    private static class DBHelper extends SQLiteOpenHelper {

        private static final String DB_NAME = "downloads.db";
        private static final int DB_VERSION = 3;

        private static final String TABLE_NAME = "saved_files";
        private static final String COLUMN_NAME = "file_name";
        private static final String COLUMN_TIME = "saved_time";

        DBHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                            COLUMN_NAME + " TEXT PRIMARY KEY, " +
                            COLUMN_TIME + " INTEGER)"
            );

            // 🔹 INDEX FOR FASTER STATS QUERIES
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_saved_time ON " +
                            TABLE_NAME + "(" + COLUMN_TIME + ")"
            );
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 3) {
                onCreate(db);
            }
        }
    }
}
