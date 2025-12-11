package com.mariaxcodexpert.whatsdownloadplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.mariaxcodexpert.whatsdownloadplus.Permissions.PermissionsActivity;

public class Splash_screen extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_STATUS_FOLDER_URI = "statusFolderUri";

    private SharedPreferences prefs;
    private Uri selectedStatusFolderUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Handle edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        restoreFolderUri();

        // Delay for splash effect (1.5s)
        new Handler().postDelayed(this::checkPermissionsAndRedirect, 1500);
    }

    private void restoreFolderUri() {
        String savedUri = prefs.getString(KEY_STATUS_FOLDER_URI, null);
        if (savedUri != null) {
            selectedStatusFolderUri = Uri.parse(savedUri);
            try {
                getContentResolver().takePersistableUriPermission(
                        selectedStatusFolderUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                );
            } catch (Exception ignored) {
                selectedStatusFolderUri = null;
            }
        }
    }

    private void checkPermissionsAndRedirect() {
        boolean storageGranted = isStorageGranted();
        boolean folderGranted = selectedStatusFolderUri != null;

        if (storageGranted && folderGranted) {
            startActivity(new Intent(this, MainActivity.class));
        } else {
            startActivity(new Intent(this, PermissionsActivity.class));
        }
        finish();
    }

    private boolean isStorageGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED;
        } else {
            return checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
    }
}
