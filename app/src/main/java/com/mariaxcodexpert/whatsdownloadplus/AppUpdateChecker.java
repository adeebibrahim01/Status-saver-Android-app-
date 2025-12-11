package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppUpdateChecker {

    private static final String TAG = "AppUpdateChecker";
    private final Activity activity;
    private final String packageName;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AppUpdateChecker(Activity activity) {
        this.activity = activity;
        this.packageName = activity.getPackageName();
    }

    public void checkForUpdate() {
        executor.execute(() -> {
            try {
                String playStoreUrl = "https://play.google.com/store/apps/details?id=" + packageName + "&hl=en";
                HttpURLConnection connection = (HttpURLConnection) new URL(playStoreUrl).openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                Scanner scanner = new Scanner(reader).useDelimiter("\\A");
                String html = scanner.hasNext() ? scanner.next() : "";

                String marker = "Current Version";
                int index = html.indexOf(marker);
                if (index == -1) return;

                String snippet = html.substring(index, Math.min(index + 50, html.length()));
                String latestVersion = snippet.replaceAll("[^0-9.]", "");

                PackageManager pm = activity.getPackageManager();
                PackageInfo info = pm.getPackageInfo(packageName, 0);
                String currentVersion = info.versionName;

                if (!currentVersion.equals(latestVersion)) {
                    mainHandler.post(this::showUpdateDialog);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to check update", e);
            }
        });
    }

    private void showUpdateDialog() {
        new AlertDialog.Builder(activity)
                .setTitle("Update Available")
                .setMessage("A new version of this app is available. Please update to continue using all features.")
                .setCancelable(false)
                .setPositiveButton("Update", (dialog, which) -> openPlayStore())
                .setNegativeButton("Later", null)
                .show();
    }

    private void openPlayStore() {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName)));
        } catch (ActivityNotFoundException e) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }
}
