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
    public static final int UPDATE_REQUEST_CODE = 1001;

    // Yahan se aap testing on/off kar sakte hain
    private final boolean isTesting = false;

    private final Activity activity;
    private final AppUpdateManager appUpdateManager;

    public AppUpdateChecker(Activity activity) {
        this.activity = activity;
        this.appUpdateManager = AppUpdateManagerFactory.create(activity);
    }

    public void checkForUpdate() {
        // Agar testing true hai, to direct Play Store popup check karein
        if (isTesting) {
            Log.d(TAG, "Testing mode: Opening Play Store directly");
            openPlayStore();
            return;
        }

        // Production Logic
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
                    openPlayStore();
                }
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

    public void onResume() {
        // Resume logic testing mein ignore hogi
        if (isTesting) return;

        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            activity,
                            UPDATE_REQUEST_CODE
                    );
                } catch (Exception ignored) {}
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                Log.e(TAG, "Update flow failed! Result code: " + resultCode);
            }
        }
    }
}