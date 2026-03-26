package com.mariaxcodexpert.whatsdownloadplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.mariaxcodexpert.whatsdownloadplus.Permissions.PermissionsActivity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Splash_screen extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_STATUS_FOLDER_URI = "statusFolderUri";
    private static final long SPLASH_DELAY_MS = 1000;

    // 1. Executor Service (Background Threading ke liye)
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // Background thread par execution shuru karein
        executor.execute(this::checkPermissionsAndRedirect);
    }

    private void checkPermissionsAndRedirect() {
        long startTime = System.currentTimeMillis();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedUriStr = prefs.getString(KEY_STATUS_FOLDER_URI, null);

        boolean isPermissionValid = false;

        if (savedUriStr != null) {
            try {
                Uri uri = Uri.parse(savedUriStr);
                isPermissionValid = hasPersistedPermission(uri) && isFolderAvailable(uri);
            } catch (Exception e) {
                isPermissionValid = false;
            }
        }

        final Intent intent;
        if (isPermissionValid) {
            intent = new Intent(this, MainActivity.class);
        } else {
            prefs.edit().remove(KEY_STATUS_FOLDER_URI).apply();
            intent = new Intent(this, PermissionsActivity.class);
        }

        // FLAG_ACTIVITY_CLEAR_TASK zaroori hai history clear karne ke liye
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        long elapsedTime = System.currentTimeMillis() - startTime;
        // Speed Tip: Agar aapko mazeed fast chahiye, to SPLASH_DELAY_MS ko 500 ya 800 kar dein
        long remainingDelay = Math.max(0, SPLASH_DELAY_MS - elapsedTime);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) {
                startActivity(intent);

                // 🔥 overridePendingTransition(0, 0) yahan se remove kar diya hai
                // Ab system ka default animation chalega

                finish();
            }
        }, remainingDelay);
    }

    private boolean hasPersistedPermission(Uri uri) {
        try {
            List<UriPermission> persistedPermissions = getContentResolver().getPersistedUriPermissions();
            for (UriPermission permission : persistedPermissions) {
                if (permission.getUri().equals(uri) && permission.isReadPermission()) {
                    return true;
                }
            }
        } catch (Exception e) { return false; }
        return false;
    }

    private boolean isFolderAvailable(Uri uri) {
        try {
            DocumentFile root = DocumentFile.fromTreeUri(this, uri);
            // 3. canRead() Check (exists() aur isDirectory() se behtar aur fast hai)
            return root != null && root.canRead();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Memory leak se bachne ke liye executor shutdown karein
        executor.shutdownNow();
    }
}