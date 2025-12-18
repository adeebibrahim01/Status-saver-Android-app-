package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

public class DownloadStatsManager {

    private static final String PREFS_NAME = "status_prefs";
    private static final String KEY_TODAY_DOWNLOADS = "today_downloads";
    private static final String KEY_LAST_DOWNLOAD_DATE = "last_download_date";
    private static final String KEY_WEEK_DOWNLOADS = "week_downloads"; // format: "dayMillis:count,..."

    private final SharedPreferences prefs;

    public DownloadStatsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // --- ADD DOWNLOAD ---
    public void addDownload() {
        long todayMillis = System.currentTimeMillis();
        int todayCount = getTodayDownloads();
        todayCount++; // increment

        // Update week data
        String weekData = prefs.getString(KEY_WEEK_DOWNLOADS, "");
        weekData = updateWeekData(weekData, todayMillis, todayCount);

        // Save
        prefs.edit()
                .putInt(KEY_TODAY_DOWNLOADS, todayCount)
                .putLong(KEY_LAST_DOWNLOAD_DATE, todayMillis)
                .putString(KEY_WEEK_DOWNLOADS, weekData)
                .apply();
    }

    public void removeDownload(long downloadTimeMillis) {

        long todayMillis = System.currentTimeMillis();

        // 1️⃣ TODAY CHECK
        if (isSameDay(downloadTimeMillis, todayMillis)) {
            int todayCount = getTodayDownloads();
            if (todayCount > 0) {
                todayCount--;
                prefs.edit().putInt(KEY_TODAY_DOWNLOADS, todayCount).apply();
            }
        }

        // 2️⃣ WEEK DATA UPDATE
        String weekData = prefs.getString(KEY_WEEK_DOWNLOADS, "");
        if (weekData.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        String[] entries = weekData.split(",");
        long sevenDaysAgo = todayMillis - 7L * 24 * 60 * 60 * 1000;

        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length != 2) continue;

            long dayMillis = Long.parseLong(parts[0]);
            int count = Integer.parseInt(parts[1]);

            // sirf last 7 days rakho
            if (dayMillis >= sevenDaysAgo) {

                // jis din delete hui usi din ka count kam
                if (isSameDay(dayMillis, downloadTimeMillis)) {
                    count = Math.max(0, count - 1);
                }

                sb.append(dayMillis).append(":").append(count).append(",");
            }
        }

        if (sb.length() > 0) sb.setLength(sb.length() - 1);

        prefs.edit().putString(KEY_WEEK_DOWNLOADS, sb.toString()).apply();
    }


    // --- GET TODAY DOWNLOADS ---
    public int getTodayDownloads() {
        checkAndResetTodayIfNeeded();
        return prefs.getInt(KEY_TODAY_DOWNLOADS, 0);
    }

    // --- GET LAST 7 DAYS DOWNLOADS ---
    public int getLast7DaysDownloads() {
        long todayMillis = System.currentTimeMillis();
        String weekData = prefs.getString(KEY_WEEK_DOWNLOADS, "");
        int total = 0;

        if (weekData.isEmpty()) return 0;

        String[] entries = weekData.split(",");
        long sevenDaysAgo = todayMillis - 7L * 24 * 60 * 60 * 1000;

        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length != 2) continue;
            long dayMillis = Long.parseLong(parts[0]);
            int count = Integer.parseInt(parts[1]);
            if (dayMillis >= sevenDaysAgo) {
                total += count;
            }
        }
        return total;
    }

    // --- HELPER: reset today if new day ---
    private void checkAndResetTodayIfNeeded() {
        long todayMillis = System.currentTimeMillis();
        long lastDownload = prefs.getLong(KEY_LAST_DOWNLOAD_DATE, 0);

        if (!isSameDay(todayMillis, lastDownload)) {
            prefs.edit()
                    .putInt(KEY_TODAY_DOWNLOADS, 0)
                    .putLong(KEY_LAST_DOWNLOAD_DATE, todayMillis)
                    .apply();
        }
    }

    // --- HELPER: same day check ---
    private boolean isSameDay(long time1, long time2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(time1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(time2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    // --- HELPER: update week data (for add/remove) ---
    private String updateWeekData(String oldData, long todayMillis, int todayCount) {
        StringBuilder sb = new StringBuilder();
        String[] entries = oldData.isEmpty() ? new String[0] : oldData.split(",");
        long sevenDaysAgo = todayMillis - 7L * 24 * 60 * 60 * 1000;
        boolean todayAdded = false;

        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length != 2) continue;
            long dayMillis = Long.parseLong(parts[0]);
            int count = Integer.parseInt(parts[1]);

            // Keep only last 7 days
            if (dayMillis >= sevenDaysAgo) {
                if (isSameDay(dayMillis, todayMillis)) {
                    if (!todayAdded) {
                        sb.append(todayMillis).append(":").append(todayCount).append(",");
                        todayAdded = true;
                    }
                } else {
                    sb.append(dayMillis).append(":").append(count).append(",");
                }
            }
        }

        // Append today if not added yet
        if (!todayAdded) {
            sb.append(todayMillis).append(":").append(todayCount);
        } else if (sb.charAt(sb.length() - 1) == ',') {
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }
}
