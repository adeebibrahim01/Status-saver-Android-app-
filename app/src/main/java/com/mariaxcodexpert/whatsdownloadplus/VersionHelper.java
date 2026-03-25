package com.mariaxcodexpert.whatsdownloadplus;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

public class VersionHelper {

    private final Context context;

    public VersionHelper(Context context) {
        this.context = context;
    }

    /**
     * Returns the app version name (e.g., "v1.0.4")
     */
    public String getAppVersion() {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pInfo;

            // Handle Deprecation for Android 13 (API 33) and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.PackageInfoFlags.of(0));
            } else {
                pInfo = pm.getPackageInfo(context.getPackageName(), 0);
            }

            return "v" + pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "v1.0.0"; // fallback version
        }
    }

    /**
     * Returns the version code (useful for internal logic/update checks)
     */
    public long getAppVersionCode() {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pInfo;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.PackageInfoFlags.of(0));
            } else {
                pInfo = pm.getPackageInfo(context.getPackageName(), 0);
            }

            // getLongVersionCode supports both old and new versioning schemes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return pInfo.getLongVersionCode();
            } else {
                return pInfo.versionCode;
            }
        } catch (Exception e) {
            return 1;
        }
    }
}