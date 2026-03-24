package com.mariaxcodexpert.whatsdownloadplus.model;

import android.content.Context;
import android.content.SharedPreferences;

public class StatusStorage {

    private static final String PREFS_NAME = "NotifiedStatusPrefs";

    public static void saveStatus(Context context, int statusId, long expiryTime, boolean isVideo) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String value = expiryTime + "|" + (isVideo ? 1 : 0);
        prefs.edit().putString(String.valueOf(statusId), value).apply();
    }

    public static void removeStatus(Context context, int statusId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(String.valueOf(statusId)).apply();
        prefs.edit().remove(statusId + "_notified").apply(); // remove notified flag
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
        String[] parts = value.split("\\|");
        return Long.parseLong(parts[0]);
    }

    public static java.util.Map<String, ?> getAllStatuses(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getAll();
    }

    // ------------------ New: Notified flag ------------------
    public static void markAsNotified(Context context, int statusId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(statusId + "_notified", true).apply();
    }

    public static boolean isNotified(Context context, int statusId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(statusId + "_notified", false);
    }
}
