package com.mariaxcodexpert.whatsdownloadplus.model;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Map;

public class StatusStorage {

    private static final String PREFS_NAME = "NotifiedStatusPrefs";

    public static void saveStatus(Context context, int statusId, long expiryTime, boolean isVideo) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String value = expiryTime + "|" + (isVideo ? 1 : 0);
        prefs.edit().putString(String.valueOf(statusId), value).apply();
    }

    public static void removeStatus(Context context, int statusId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(String.valueOf(statusId));
        // Dono types ke flags remove karein
        editor.remove(statusId + "_notified_" + NotificationReceiver.TYPE_1_HOUR);
        editor.remove(statusId + "_notified_" + NotificationReceiver.TYPE_30_MIN);
        editor.apply();
    }

    public static boolean isVideo(Context context, int statusId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String value = prefs.getString(String.valueOf(statusId), null);
        if (value == null) return false;
        String[] parts = value.split("\\|");
        return parts.length > 1 && parts[1].equals("1");
    }

    public static long getExpiryTime(Context context, int statusId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String value = prefs.getString(String.valueOf(statusId), null);
        if (value == null) return -1;
        try {
            String[] parts = value.split("\\|");
            return Long.parseLong(parts[0]);
        } catch (Exception e) {
            return -1;
        }
    }

    public static Map<String, ?> getAllStatuses(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getAll();
    }

    // ------------------ Updated: Notified flags with Type ------------------

    /**
     * Mark a specific status AND a specific notification type as notified.
     */
    public static void markAsNotified(Context context, int statusId, int type) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Key format: "123_notified_1" or "123_notified_2"
        prefs.edit().putBoolean(statusId + "_notified_" + type, true).apply();
    }

    /**
     * Check if a specific notification type has already been sent for this status.
     */
    public static boolean isNotified(Context context, int statusId, int type) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(statusId + "_notified_" + type, false);
    }
}