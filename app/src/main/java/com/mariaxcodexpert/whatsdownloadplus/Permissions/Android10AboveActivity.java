package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationManagerCompat;

import java.util.Set;

public class Android10AboveActivity {

    private final Activity activity;
    private final SharedPreferences prefs;

    private static final String KEY_STATUS_FOLDER_URI = "statusFolderUri";

    public Android10AboveActivity(Activity activity, SharedPreferences prefs) {
        this.activity = activity;
        this.prefs = prefs;
    }

    /** ------------------ Notification Permission ------------------ */
    public boolean isNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) return true;

        Set<String> enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(activity);
        return enabledPackages != null && enabledPackages.contains(activity.getPackageName());
    }

    public void showNotificationAccessDialog() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) return;

        new AlertDialog.Builder(activity)
                .setTitle("Enable Notification Access")
                .setMessage("To recover deleted messages, allow Notification Access.")
                .setPositiveButton("Allow", (d, w) -> {
                    try {
                        activity.startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    } catch (Exception e) {
                        Toast.makeText(activity, "Enable manually", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** ------------------ Folder Picker for Android 10+ ------------------ */
    public void openStatusFolderPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

            activity.startActivityForResult(intent, 1001);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(activity, "Select folder manually", Toast.LENGTH_LONG).show();
        }
    }
}
