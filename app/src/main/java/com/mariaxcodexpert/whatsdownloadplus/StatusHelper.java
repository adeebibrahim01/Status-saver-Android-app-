package com.mariaxcodexpert.whatsdownloadplus;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;

public class StatusHelper {

    private static final String TAG = "StatusHelper";

    /**
     * Detects status type (image/video) based on file path
     *
     * @param context  app context
     * @param statusId ID used to identify the status file
     * @return "video" or "image" (defaults to "image")
     */
    public static String getStatusType(Context context, int statusId) {
        try {
            // Example: your status files are stored in:
            // /Android/media/com.whatsapp/WhatsApp/Media/.Statuses
            File statusDir = new File(context.getExternalFilesDir(null),
                    "WhatsApp/Media/.Statuses");

            if (!statusDir.exists()) return "image";

            File[] files = statusDir.listFiles();
            if (files == null || files.length == 0) return "image";

            // For demo: match file by statusId in filename
            for (File file : files) {
                String name = file.getName();
                if (name.contains(String.valueOf(statusId))) {
                    if (isVideoFile(name)) return "video";
                    else return "image";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error detecting status type", e);
        }
        return "image"; // default
    }

    private static boolean isVideoFile(String filename) {
        filename = filename.toLowerCase();
        return filename.endsWith(".mp4") || filename.endsWith(".mkv") || filename.endsWith(".3gp");
    }
}
