package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallException;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallErrorCode;
import com.google.android.play.core.install.model.UpdateAvailability;

public class AppUpdateChecker {

    private static final String TAG = "AppUpdateChecker";
    private final boolean isTesting = false;

    private final Activity activity;
    private final AppUpdateManager appUpdateManager;
    private final ActivityResultLauncher<IntentSenderRequest> updateLauncher;

    // Constructor mein launcher lazmi pass karein
    public AppUpdateChecker(Activity activity, ActivityResultLauncher<IntentSenderRequest> updateLauncher) {
        this.activity = activity;
        this.appUpdateManager = AppUpdateManagerFactory.create(activity);
        this.updateLauncher = updateLauncher;
    }

    public void checkForUpdate() {
        if (isTesting) {
            Log.d(TAG, "Testing mode: Opening Play Store directly");
            openPlayStore();
            return;
        }

        Task<AppUpdateInfo> task = appUpdateManager.getAppUpdateInfo();
        task.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                startUpdate(appUpdateInfo);
            }
        });

        task.addOnFailureListener(e -> {
            if (e instanceof InstallException ie) {
                if (ie.getErrorCode() == InstallErrorCode.ERROR_APP_NOT_OWNED) {
                    return;
                }
            }
            Log.w(TAG, "Update check failed silently");
        });
    }

    private void startUpdate(AppUpdateInfo appUpdateInfo) {
        try {
            // Modern Way: Using AppUpdateOptions and Launcher
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
            );
        } catch (Exception e) {
            Log.e(TAG, "Update flow failed", e);
            openPlayStore();
        }
    }

    public void onResume() {
        if (isTesting) return;

        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            // Agar update pehle se chal rahi hai (InProgress), toh usay foran dikhao
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startUpdate(appUpdateInfo);
            }
        });
    }

    private void openPlayStore() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + activity.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + activity.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        }
    }
}