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

        // Delay for splash effect (1.5s)
        new Handler(Looper.getMainLooper()).postDelayed(this::checkPermissionsAndRedirect, 1500);
    }

    /**
     * Restore previously selected folder URI (if any) and validate persisted permission
     */
    private void restoreFolderUri() {
        String savedUri = prefs.getString(KEY_STATUS_FOLDER_URI, null);
        if (savedUri != null) {
            selectedStatusFolderUri = Uri.parse(savedUri);
            try {
                // Take persistable permission if not already
                int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                getContentResolver().takePersistableUriPermission(selectedStatusFolderUri, takeFlags);
            } catch (SecurityException e) {
                selectedStatusFolderUri = null;
            }
        }
    }

    /**
     * Navigate to appropriate activity based on folder permission
     */
    private void checkPermissionsAndRedirect() {
        if (hasNavigated) return;
        hasNavigated = true;

        boolean folderGranted = selectedStatusFolderUri != null;

        if (folderGranted) {
            startActivity(new Intent(this, MainActivity.class));
        } else {
            startActivity(new Intent(this, PermissionsActivity.class));
        }
        finish();
    }
}
