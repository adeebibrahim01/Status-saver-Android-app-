package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.mariaxcodexpert.whatsdownloadplus.MainActivity;
import com.mariaxcodexpert.whatsdownloadplus.R;

import java.io.File;
import java.util.Set;

public class PermissionsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_STATUS_FOLDER_URI = "statusFolderUri";

    public Uri selectedStatusFolderUri;
    private SharedPreferences prefs;

    private ViewPager2 viewPager;
    private int[] layouts = {
            R.layout.layout_select_app,
            R.layout.layout_permissions
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_activity); // <-- setContentView first!
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new PermissionsPagerAdapter(this, layouts, viewPager));

        // Restore previously selected folder URI
        String savedUri = prefs.getString(KEY_STATUS_FOLDER_URI, null);
        if (savedUri != null) {
            selectedStatusFolderUri = Uri.parse(savedUri);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && selectedStatusFolderUri != null) {
                try {
                    getContentResolver().takePersistableUriPermission(selectedStatusFolderUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } catch (Exception e) {
                    e.printStackTrace();
                    selectedStatusFolderUri = null;
                }
            }
        }

        // Update buttons initially
        refreshPermissionButtons();

        if (prefs.getBoolean("introCompleted", false)) {
            goToMainActivity();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh UI dynamically for notification permission or folder permission changes
        refreshPermissionButtons();
        checkAllPermissionsAndProceed();
    }

    private void refreshPermissionButtons() {
        updatePermissionButtonUI(findViewById(R.id.allowStorageButton), isStorageGranted());
        updatePermissionButtonUI(findViewById(R.id.allowNotificationButton), isNotificationPermissionGranted());
        updatePermissionButtonUI(findViewById(R.id.allowStatusFolderButton), selectedStatusFolderUri != null);
    }

    /** ------------------ Permission Checks ------------------ */
    public boolean isStorageGranted() {
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

    public void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VIDEO
            }, 2001);
        } else {
            requestPermissions(new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 2001);
        }
    }

    /** Notification permission check only for Android 10+ */
    public boolean isNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) return true;
        Set<String> enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(this);
        return enabledPackages != null && enabledPackages.contains(getPackageName());
    }

    public void showNotificationAccessDialog() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) return;
        new AlertDialog.Builder(this)
                .setTitle("Enable Notification Access")
                .setMessage("To recover deleted messages, please allow notification access.")
                .setCancelable(false)
                .setPositiveButton("Allow", (dialog, which) -> {
                    try {
                        startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    } catch (Exception e) {
                        Toast.makeText(this, "Please enable manually", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** ------------------ Open Folder Picker ------------------ */
    public void openStatusFolderPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, 1001);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Select folder manually", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    selectedStatusFolderUri = uri;
                    prefs.edit().putString(KEY_STATUS_FOLDER_URI, uri.toString()).apply();

                    refreshPermissionButtons();
                    checkAllPermissionsAndProceed();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to save folder permission ❌", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /** ------------------ WhatsApp Folder Detection ------------------ */
    public void detectStatusFolder() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            try {
                File whatsappFolder = new File("/storage/emulated/0/WhatsApp/Media/.Statuses");
                File businessFolder = new File("/storage/emulated/0/WhatsApp Business/Media/.Statuses");
                File folderToUse = null;

                if (whatsappFolder.exists() && whatsappFolder.isDirectory()) folderToUse = whatsappFolder;
                else if (businessFolder.exists() && businessFolder.isDirectory()) folderToUse = businessFolder;

                if (folderToUse != null) selectedStatusFolderUri = Uri.fromFile(folderToUse);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (selectedStatusFolderUri == null) openStatusFolderPicker();
        }

        refreshPermissionButtons();
        checkAllPermissionsAndProceed();
    }

    /** ------------------ Button UI ------------------ */
    public void updatePermissionButtonUI(Button button, boolean granted) {
        if (button == null) return;
        if (granted) {
            button.setText("✅");
            button.setEnabled(false);
            button.setAlpha(0.7f);
        } else {
            button.setText("Allow");
            button.setEnabled(true);
            button.setAlpha(1f);
        }
    }

    /** ------------------ Check All Permissions & Redirect ------------------ */
    public void checkAllPermissionsAndProceed() {
        boolean storage = isStorageGranted();
        boolean notification = isNotificationPermissionGranted();
        boolean folder = selectedStatusFolderUri != null;

        if (storage && notification && folder) {
            goToMainActivity();
        }
    }

    private void goToMainActivity() {
        prefs.edit().putBoolean("introCompleted", true).apply();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshPermissionButtons();
        checkAllPermissionsAndProceed();
    }
}