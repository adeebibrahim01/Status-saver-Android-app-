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
        prefs.edit().remove(String.valueOf(statusId))
                .remove(statusId + "_notified_1")
                .remove(statusId + "_notified_2")
                .apply();
    }

    public static long getExpiryTime(Context context, int statusId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String value = prefs.getString(String.valueOf(statusId), null);
        if (value == null) return -1;
        return Long.parseLong(value.split("\\|")[0]);
    }

    public static boolean isVideo(Context context, int statusId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String value = prefs.getString(String.valueOf(statusId), null);
        return value != null && value.split("\\|")[1].equals("1");
    }

    public static Map<String, ?> getAllStatuses(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getAll();
    }

    public static void markAsNotified(Context context, int statusId, int type) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(statusId + "_notified_" + type, true).apply();
    }

    public static boolean isNotified(Context context, int statusId, int type) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(statusId + "_notified_" + type, false);
    }
}