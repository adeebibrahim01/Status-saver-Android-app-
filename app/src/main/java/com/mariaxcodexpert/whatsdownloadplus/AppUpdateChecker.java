package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallException;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallErrorCode;
import com.google.android.play.core.install.model.UpdateAvailability;

public class AppUpdateChecker {

    private static final String TAG = "AppUpdateChecker";
    private static final int UPDATE_REQUEST_CODE = 1001;

    private final Activity activity;
    private final AppUpdateManager appUpdateManager;

    public AppUpdateChecker(Activity activity) {
        this.activity = activity;
        this.appUpdateManager = AppUpdateManagerFactory.create(activity);
    }

    // Check for updates (Safe & Silent)
    public void checkForUpdate() {

        Task<AppUpdateInfo> task = appUpdateManager.getAppUpdateInfo();

        task.addOnSuccessListener(appUpdateInfo -> {

            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {

                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            activity,
                            UPDATE_REQUEST_CODE
                    );

                } catch (Exception e) {
                    // REAL failure → optional fallback
                    openPlayStore();
                }
            }
            // else → silently ignore (no update / not allowed)

        });

        task.addOnFailureListener(e -> {

            // ✅ SILENTLY IGNORE: App not owned (sideload / APK install)
            if (e instanceof InstallException) {
                InstallException ie = (InstallException) e;

                if (ie.getErrorCode() == InstallErrorCode.ERROR_APP_NOT_OWNED) {
                    return; // 🔇 absolutely nothing
                }
            }

            // Other unexpected errors (optional log)
            Log.w(TAG, "Update check failed silently");
        });
    }

    private void openPlayStore() {
        try {
            activity.startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + activity.getPackageName())
            ));
        } catch (ActivityNotFoundException e) {
            activity.startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + activity.getPackageName())
            ));
        }
    }

    // Handle update result
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == UPDATE_REQUEST_CODE) {
            // 🔇 User cancel / fail → no action
        }
    }
}
