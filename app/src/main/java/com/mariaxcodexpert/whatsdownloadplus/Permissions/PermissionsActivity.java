package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.mariaxcodexpert.whatsdownloadplus.MainActivity;
import com.mariaxcodexpert.whatsdownloadplus.R;

import java.io.File;

public class PermissionsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_STATUS_FOLDER_URI = "statusFolderUri";

    public Uri selectedStatusFolderUri;
    private SharedPreferences prefs;

    private ViewPager2 viewPager;
    private int[] layouts = {R.layout.layout_select_app, R.layout.layout_permissions};

    private Android10AboveActivity android10Above;
    AccessibilityBelow10Helper accessHelper;

    private Button storageBtn, notificationBtn, statusFolderBtn;

    private Handler handler = new Handler();
    private boolean accessibilityDialogShown = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_activity);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new PermissionsPagerAdapter(this, layouts, viewPager));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            android10Above = new Android10AboveActivity(this, prefs);
        } else {
            accessHelper = new AccessibilityBelow10Helper(this);
        }

        // Restore saved folder URI
        String savedUri = prefs.getString(KEY_STATUS_FOLDER_URI, null);
        if (savedUri != null) {
            selectedStatusFolderUri = Uri.parse(savedUri);
            try {
                getContentResolver().takePersistableUriPermission(selectedStatusFolderUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception ignored) {
                selectedStatusFolderUri = null;
            }
        }

        storageBtn = findViewById(R.id.allowStorageButton);
        notificationBtn = findViewById(R.id.allowNotificationButton);
        statusFolderBtn = findViewById(R.id.allowStatusFolderButton);

        // Hide notification button for Android 10 below
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && notificationBtn != null) {
            notificationBtn.setVisibility(View.GONE);
        }

        setupButtons();
        refreshPermissionButtons();

        if (prefs.getBoolean("introCompleted", false)) {
            goToMainActivity();
        }

        // Polling for Android 10 below to handle accessibility & folder detection
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            handler.postDelayed(this::checkPermissionsAndroid10Below, 500);
        }
    }

    private void checkPermissionsAndroid10Below() {
        if (isStorageGranted()) {
            // Show accessibility dialog only once
            if (!isNotificationPermissionGranted() && !accessibilityDialogShown) {
                if (accessHelper != null) {
                    accessHelper.showAccessibilityDialog();
                    accessibilityDialogShown = true;
                }
            }

            // Folder detection even if accessibility denied
            if (selectedStatusFolderUri == null) {
                detectStatusFolder();
            }

            // Navigate to MainActivity if storage + folder ready
            if (isStorageGranted() && selectedStatusFolderUri != null) {
                goToMainActivity();
                return; // stop further polling
            }
        }

        // Continue polling
        handler.postDelayed(this::checkPermissionsAndroid10Below, 1000);
    }

    private void setupButtons() {
        if (storageBtn != null) storageBtn.setOnClickListener(v -> {
            requestStoragePermission();
            storageBtn.postDelayed(() -> updatePermissionButtonUI(storageBtn, isStorageGranted()), 500);
        });

        if (notificationBtn != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            notificationBtn.setOnClickListener(v -> {
                if (!isNotificationPermissionGranted()) showNotificationAccessDialog();
            });
        }

        if (statusFolderBtn != null) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                statusFolderBtn.setEnabled(false);
            } else {
                statusFolderBtn.setOnClickListener(v -> openStatusFolderPicker());
            }
        }
    }

    private void refreshPermissionButtons() {
        if (storageBtn != null) updatePermissionButtonUI(storageBtn, isStorageGranted());
        if (notificationBtn != null) updatePermissionButtonUI(notificationBtn, isNotificationPermissionGranted());
        if (statusFolderBtn != null) updatePermissionButtonUI(statusFolderBtn, selectedStatusFolderUri != null);
    }

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

    public void showNotificationAccessDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) android10Above.showNotificationAccessDialog();
        else if (accessHelper != null) accessHelper.showAccessibilityDialog();
    }

    public void openStatusFolderPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) android10Above.openStatusFolderPicker();
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
                    Toast.makeText(this, "Failed to save folder permission", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    void checkAllPermissionsAndProceed() {
        boolean storage = isStorageGranted();
        boolean notification = isNotificationPermissionGranted();
        boolean folder = selectedStatusFolderUri != null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (storage && notification && folder) {
                goToMainActivity();
            }
        }
    }

    public void detectStatusFolder() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            File whatsapp = new File("/storage/emulated/0/WhatsApp/Media/.Statuses");
            File business = new File("/storage/emulated/0/WhatsApp Business/Media/.Statuses");

            if (whatsapp.exists()) selectedStatusFolderUri = Uri.fromFile(whatsapp);
            else if (business.exists()) selectedStatusFolderUri = Uri.fromFile(business);
        } else {
            if (selectedStatusFolderUri == null) openStatusFolderPicker();
        }

        refreshPermissionButtons();
    }

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

    public boolean isNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return android10Above != null && android10Above.isNotificationPermissionGranted();
        } else {
            return accessHelper != null &&
                    accessHelper.isAccessibilityEnabled(getPackageName() + "/.WhatsAccessibilityService");
        }
    }

    private void goToMainActivity() {
        prefs.edit().putBoolean("introCompleted", true).apply();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
