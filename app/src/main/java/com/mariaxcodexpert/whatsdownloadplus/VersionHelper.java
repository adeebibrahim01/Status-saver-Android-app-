package com.mariaxcodexpert.whatsdownloadplus;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class VersionHelper {

    private final Context context;

    public VersionHelper(Context context) {
        this.context = context;
    }

    public String getAppVersion() {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pInfo = pm.getPackageInfo(context.getPackageName(), 0);
            return "v" + pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "v1.0.0"; // fallback
        }
    }
}
