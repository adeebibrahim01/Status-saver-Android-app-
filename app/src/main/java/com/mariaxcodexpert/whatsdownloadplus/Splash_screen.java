package com.mariaxcodexpert.whatsdownloadplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.mariaxcodexpert.whatsdownloadplus.Permissions.PermissionsActivity;

public class Splash_screen extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_STATUS_FOLDER_URI = "statusFolderUri";
    private static final long SPLASH_DELAY_MS = 1500;

    private SharedPreferences prefs;
    private Uri selectedStatusFolderUri;

    // Flag to prevent multiple navigations
    private boolean hasNavigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        restoreFolderUri();

        new Handler(Looper.getMainLooper()).postDelayed(this::checkPermissionsAndRedirect, SPLASH_DELAY_MS);
    }

    /**
     * Restore previously selected folder URI (if any) and validate persisted permission.
     * Removes invalid URI from preferences.
     */
    private void restoreFolderUri() {
        String savedUri = prefs.getString(KEY_STATUS_FOLDER_URI, null);
        if (savedUri != null) {
            Uri uri = Uri.parse(savedUri);
            try {
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
                selectedStatusFolderUri = uri;
            } catch (SecurityException e) {
                // Invalid or revoked URI, clean up SharedPreferences
                selectedStatusFolderUri = null;
                prefs.edit().remove(KEY_STATUS_FOLDER_URI).apply();
            }
        }
    }

    /**
     * Navigate to MainActivity or PermissionsActivity based on folder permission.
     * Ensures only single navigation even if called multiple times.
     */
    private void checkPermissionsAndRedirect() {
        if (hasNavigated) return;
        hasNavigated = true;

        Intent intent;
        if (selectedStatusFolderUri != null) {
            intent = new Intent(this, MainActivity.class);
        } else {
            intent = new Intent(this, PermissionsActivity.class);
        }

        startActivity(intent);
        finish();
    }
}
