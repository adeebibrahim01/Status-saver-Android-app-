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
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import com.mariaxcodexpert.whatsdownloadplus.Permissions.PermissionsActivity;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Splash_screen extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_STATUS_FOLDER_URI = "statusFolderUri";

    // Memory leak se bachne ke liye executor aur handler
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isActivityDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Set Layout First
        setContentView(R.layout.activity_splash_screen);

        // 2. Hide System UI (After setting content to avoid crash on Android 11+)
        hideSystemUI();

        ProgressBar progressBar = findViewById(R.id.progressBar);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // 3. Start Background Logic
        executor.execute(this::checkPermissionsAndRedirect);
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);

            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    private void checkPermissionsAndRedirect() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedUriStr = prefs.getString(KEY_STATUS_FOLDER_URI, null);

        boolean isPermissionValid = false;
        if (savedUriStr != null) {
            try {
                Uri uri = Uri.parse(savedUriStr);
                // Sirf persistence check karein, DocumentFile.canRead() remove kar diya (Slow task)
                isPermissionValid = hasPersistedPermission(uri);
            } catch (Exception e) {
                isPermissionValid = false;
            }
        }

        final boolean finalStatus = isPermissionValid;

        // UI update on main thread with a small delay for smooth UX
        mainHandler.postDelayed(() -> {
            if (isActivityDestroyed) return;

            Intent intent;
            if (finalStatus) {
                intent = new Intent(Splash_screen.this, MainActivity.class);
            } else {
                intent = new Intent(Splash_screen.this, PermissionsActivity.class);
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            applyFadeTransition();
            finish();

        }, 1000); // 1 second delay taake progress bar move hota dikhe
    }

    private boolean hasPersistedPermission(Uri uri) {
        try {
            List<UriPermission> persistedPermissions = getContentResolver().getPersistedUriPermissions();
            for (UriPermission permission : persistedPermissions) {
                if (permission.getUri().equals(uri) && permission.isReadPermission()) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private void applyFadeTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    @Override
    protected void onDestroy() {
        isActivityDestroyed = true;
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}