package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;

import androidx.appcompat.app.AlertDialog;

public class AccessibilityBelow10Helper {

    private final Activity activity;

    public AccessibilityBelow10Helper(Activity activity) {
        this.activity = activity;
    }

    /** ----------------------------------------
     * CHECK IF ACCESSIBILITY SERVICE IS ENABLED
     * serviceId = "com.package.name/.Permissions.WhatsAccessibilityService"
     * Works for Android 4.4 → 10
     * ---------------------------------------- */
    public boolean isAccessibilityEnabled(String serviceId) {
        try {
            AccessibilityManager am =
                    (AccessibilityManager) activity.getSystemService(Activity.ACCESSIBILITY_SERVICE);

            if (am == null || !am.isEnabled()) return false;

            String enabledServices = Settings.Secure.getString(
                    activity.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );

            if (!TextUtils.isEmpty(enabledServices)) {
                String[] services = enabledServices.split(":");
                for (String service : services) {
                    if (service.equalsIgnoreCase(serviceId)) {
                        return true;
                    }
                }
            }

        } catch (Exception ignored) {}

        return false;
    }

    /** ----------------------------------------
     * SHOW DIALOG TO ENABLE ACCESSIBILITY SERVICE
     * Prompts user to open Accessibility Settings
     * ---------------------------------------- */
    public void showAccessibilityDialog() {
        new AlertDialog.Builder(activity)
                .setTitle("Enable Accessibility Service")
                .setMessage("For message recovery and advanced features, please enable Accessibility Service.")
                .setCancelable(false)
                .setPositiveButton("Enable", (dialog, which) -> openAccessibilitySettings())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** ----------------------------------------
     * OPEN ACCESSIBILITY SETTINGS
     * Directs user to specific page for enabling service
     * ---------------------------------------- */
    public void openAccessibilitySettings() {
        try {
            // Opens Accessibility Settings page where the user can enable the service
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** ----------------------------------------
     * ENSURE ACCESSIBILITY IS ENABLED
     * Checks and prompts user if not enabled
     * ---------------------------------------- */
    public void ensureAccessibilityEnabled(String serviceId) {
        if (!isAccessibilityEnabled(serviceId)) {
            showAccessibilityDialog();
        }
    }
}
