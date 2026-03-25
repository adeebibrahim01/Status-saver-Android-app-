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

public class Splash_screen extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_STATUS_FOLDER_URI = "statusFolderUri";
    private static final long SPLASH_DELAY_MS = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        new Handler(Looper.getMainLooper()).postDelayed(this::checkPermissionsAndRedirect, SPLASH_DELAY_MS);
    }

    private void checkPermissionsAndRedirect() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedUriStr = prefs.getString(KEY_STATUS_FOLDER_URI, null);

        boolean isPermissionValid = false;

        if (savedUriStr != null) {
            Uri uri = Uri.parse(savedUriStr);
            isPermissionValid = hasPersistedPermission(uri) && isFolderAvailable(uri);
        }

        Intent intent;
        if (isPermissionValid) {
            intent = new Intent(this, MainActivity.class);
        } else {
            // Agar permission nahi hai to purana data clean karein
            prefs.edit().remove(KEY_STATUS_FOLDER_URI).apply();
            intent = new Intent(this, PermissionsActivity.class);
        }

        startActivity(intent);
        finish();
    }

    // Check karein ke system ke paas is URI ki permanent permission hai ya nahi
    private boolean hasPersistedPermission(Uri uri) {
        List<UriPermission> persistedPermissions = getContentResolver().getPersistedUriPermissions();
        for (UriPermission permission : persistedPermissions) {
            if (permission.getUri().equals(uri) && permission.isReadPermission()) {
                return true;
            }
        }
        return false;
    }

    // Check karein ke folder waqai memory mein mojood hai
    private boolean isFolderAvailable(Uri uri) {
        try {
            DocumentFile root = DocumentFile.fromTreeUri(this, uri);
            return root != null && root.exists() && root.isDirectory();
        } catch (Exception e) {
            return false;
        }
    }
}