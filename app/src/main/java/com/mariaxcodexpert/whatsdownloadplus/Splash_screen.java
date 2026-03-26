package com.mariaxcodexpert.whatsdownloadplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import com.mariaxcodexpert.whatsdownloadplus.Permissions.PermissionsActivity;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Splash_screen extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_STATUS_FOLDER_URI = "statusFolderUri";
    private ProgressBar progressBar;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        progressBar = findViewById(R.id.progressBar);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        executor.execute(this::checkPermissionsAndRedirect);
    }

    private void checkPermissionsAndRedirect() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedUriStr = prefs.getString(KEY_STATUS_FOLDER_URI, null);

        boolean isPermissionValid = false;
        if (savedUriStr != null) {
            try {
                Uri uri = Uri.parse(savedUriStr);
                isPermissionValid = hasPersistedPermission(uri) && isFolderAvailable(uri);
            } catch (Exception e) { isPermissionValid = false; }
        }

        final Intent intent;
        if (isPermissionValid) {
            intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

            mainHandler.post(() -> {
                startActivity(intent);
                // 🔥 MAGIC START: MainActivity ke ready hone ka intezar karein
                waitForMainActivityReady();
            });
        } else {
            intent = new Intent(this, PermissionsActivity.class);
            mainHandler.post(() -> {
                startActivity(intent);
                finish();
            });
        }
    }

    // 🔥 Ye method check karta rahega ke MainActivity ready hui ya nahi
    private void waitForMainActivityReady() {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (MainActivity.isUIReady) {
                    // Jab MainActivity ready ho jaye, tab Splash finish karein
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out);
                    } else {
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    }
                    finish();
                } else {
                    // Agar ready nahi hui, toh 100ms baad phir check karein
                    mainHandler.postDelayed(this, 100);
                }
            }
        }, 100);
    }

    private boolean hasPersistedPermission(Uri uri) {
        try {
            List<UriPermission> persistedPermissions = getContentResolver().getPersistedUriPermissions();
            for (UriPermission permission : persistedPermissions) {
                if (permission.getUri().equals(uri) && permission.isReadPermission()) return true;
            }
        } catch (Exception e) { return false; }
        return false;
    }

    private boolean isFolderAvailable(Uri uri) {
        try {
            DocumentFile root = DocumentFile.fromTreeUri(this, uri);
            return root != null && root.canRead();
        } catch (Exception e) { return false; }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
    }
}