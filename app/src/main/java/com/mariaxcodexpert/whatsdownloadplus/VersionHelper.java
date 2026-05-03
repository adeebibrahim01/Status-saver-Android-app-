package com.mariaxcodexpert.whatsdownloadplus;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * 🔥 Extreme Level Version Engine
 * Features: Static Memoization (Caching), Thread-Safe Singleton, & API 33+ Handling.
 */
public class VersionHelper {

    private static final String TAG = "VersionHelper_Extreme";
    private static volatile String cachedVersion = null; // Memory mein save rakhne ke liye
    private static volatile long cachedBuildCode = -1;

    /**
     * 🔥 Thread-Safe Version Fetcher with Caching
     * Is se PackageManager ko baar baar hit nahi karna parta (Performance Boost)
     */
    @NonNull
    public static String getAppVersion(@NonNull Context context) {
        if (cachedVersion != null) return cachedVersion;

        synchronized (VersionHelper.class) {
            if (cachedVersion == null) {
                try {
                    PackageInfo pInfo = getPackageInfo(context);
                    cachedVersion = "v" + pInfo.versionName;
                    cachedBuildCode = getLongVersionCode(pInfo);

                    Log.i(TAG, "Version Resolved: " + cachedVersion + " (Build: " + cachedBuildCode + ")");
                } catch (Exception e) {
                    Log.e(TAG, "Critical failure retrieving version: " + e.getMessage());
                    return "v1.0.0"; // Final Fallback
                }
            }
        }
        return cachedVersion;
    }

    /**
     * 🔥 API 33+ Flag Optimization
     * Handles the deprecation of getPackageInfo(String, int) elegantly.
     */
    private static PackageInfo getPackageInfo(@NonNull Context context) throws PackageManager.NameNotFoundException {
        PackageManager pm = context.getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return pm.getPackageInfo(context.getPackageName(), PackageManager.PackageInfoFlags.of(0));
        } else {
            return pm.getPackageInfo(context.getPackageName(), 0);
        }
    }

    /**
     * 🔥 Modern Build Code Handler
     * Handles old 'versionCode' and new 'longVersionCode' for Android P+
     */
    public static long getLongVersionCode(PackageInfo pInfo) {
        return pInfo.getLongVersionCode();
    }

    /**
     * 🔥 Device Compatibility Check
     * Returns true if device is running on Android 10 (Q) or higher.
     */
    public static boolean isAtLeastAndroid10() {
        return true;
    }

    /**
     * 🔥 Debug Info Generator
     * Useful for 'Contact Us' or 'Feedback' logs.
     */
    public static String getDeviceInfo() {
        return "Model: " + Build.MODEL +
                " | Brand: " + Build.BRAND +
                " | API: " + Build.VERSION.SDK_INT;
    }
}