package com.mariaxcodexpert.whatsdownloadplus.Helper;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

public class VersionHelper {

    private static final String TAG = "VersionHelper_Extreme";
    private static volatile String cachedVersion = null;
    private static volatile long cachedBuildCode = -1;

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
                    return "v1.0.0";
                }
            }
        }
        return cachedVersion;
    }

    private static PackageInfo getPackageInfo(@NonNull Context context) throws PackageManager.NameNotFoundException {
        PackageManager pm = context.getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return pm.getPackageInfo(context.getPackageName(), PackageManager.PackageInfoFlags.of(0));
        } else {
            return pm.getPackageInfo(context.getPackageName(), 0);
        }
    }

    public static long getLongVersionCode(PackageInfo pInfo) {
        return pInfo.getLongVersionCode();
    }
}