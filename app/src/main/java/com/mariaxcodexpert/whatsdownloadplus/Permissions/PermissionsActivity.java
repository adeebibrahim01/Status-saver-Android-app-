package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
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
    private android.app.AlertDialog permissionsDialog; // class level variable

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


        if (prefs.getBoolean("introCompleted", false)) {
            goToMainActivity();
        }

        // Polling for Android 10 below to handle accessibility & folder detection
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            handler.postDelayed(this::checkPermissionsAndroid10Below, 500);
        }
    }

    private void checkPermissionsAndroid10Below() {
        boolean storageReady = isStorageGranted();
        boolean folderReady = selectedStatusFolderUri != null;
        boolean accessibilityReady = isNotificationPermissionGranted();


        // Step 1: Ask for accessibility only after storage is granted
        if (storageReady && !accessibilityReady && !accessibilityDialogShown) {
            if (accessHelper != null) {
                accessibilityDialogShown = true;
                runOnUiThread(() -> accessHelper.showAccessibilityDialog());
            }
        }

        // Step 2: Detect folder if not already set
        if (!folderReady) {
            detectStatusFolder();
            folderReady = selectedStatusFolderUri != null;
        }

        // Step 3: Check if all required permissions are granted
        if (storageReady && folderReady && accessibilityReady) {
            // Show popup if not already shown
            if (!prefs.getBoolean("permissionsPopupShown", false)) {
                prefs.edit().putBoolean("permissionsPopupShown", true).apply();
                runOnUiThread(this::showAllPermissionsGrantedPopup); // UI thread
            }
            return; // Stop further polling once popup triggered
        }

        // Step 4: Continue polling every 500ms
        handler.postDelayed(this::checkPermissionsAndroid10Below, 500);
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

                    // Update buttons in activity
                    refreshPermissionButtons();

                    // Update buttons in ViewPager page (adapter)
                    if (viewPager.getAdapter() instanceof PermissionsPagerAdapter adapter) {
                        adapter.refreshPermissionsPage();
                    }

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

        boolean allGranted;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            allGranted = storage && notification && folder;
        } else {
            allGranted = storage && folder; // notification via accessibility below 10
        }

        if (allGranted) {
            showAllPermissionsGrantedPopup();
        }
    }


    private void showAllPermissionsGrantedPopup() {
        // Inflate custom popup layout
        View popupView = getLayoutInflater().inflate(R.layout.dialog_permissions_granted, null);

        // Create AlertDialog
        permissionsDialog = new android.app.AlertDialog.Builder(this)
                .setView(popupView)
                .setCancelable(false)
                .create();

        // Find views
        Button continueBtn = popupView.findViewById(R.id.btnContinue);
        ProgressBar progressBar = popupView.findViewById(R.id.progressBar);

        // Initially hide progress bar
        progressBar.setVisibility(View.GONE);

        continueBtn.setOnClickListener(v -> {
            // Show progress bar & disable button
            progressBar.setVisibility(View.VISIBLE);
            continueBtn.setEnabled(false);
            continueBtn.setText("Loading...");

            // Wait 2 seconds then go to MainActivity
            new Handler().postDelayed(() -> {
                if (permissionsDialog != null && permissionsDialog.isShowing()) {
                    permissionsDialog.dismiss();
                }
                goToMainActivity();
            }, 2000); // 2000ms = 2 seconds
        });

        permissionsDialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Dismiss popup if activity is going to background or MainActivity is visible
        if (permissionsDialog != null && permissionsDialog.isShowing()) {
            permissionsDialog.dismiss();
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
            if (accessHelper != null) {
                // Exact full service path
                String serviceId = getPackageName() + "/.Permissions.WhatsAccessibilityService";
                return accessHelper.isAccessibilityEnabled(serviceId);
            }
            return false;
        }
    }


    private void goToMainActivity() {
        prefs.edit().putBoolean("introCompleted", true).apply();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh buttons in ViewPager
        if (viewPager.getAdapter() instanceof PermissionsPagerAdapter adapter) {
            adapter.refreshPermissionsPage();
        }

        // Delay thoda do taake system properly update ho jaye
        new Handler().postDelayed(() -> {
            boolean storageReady = isStorageGranted();
            boolean folderReady = selectedStatusFolderUri != null;
            boolean accessibilityReady = isNotificationPermissionGranted();

            // Show toast for debugging/status
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                showEnabledAccessibilityStatus();
            }

            // Trigger popup if all permissions ready and not yet shown
            if (storageReady && folderReady && accessibilityReady &&
                    !prefs.getBoolean("permissionsPopupShown", true)) {
                prefs.edit().putBoolean("permissionsPopupShown", true).apply();
                showAllPermissionsGrantedPopup();
            }
        }, 500); // 0.5 second delay
    }

    private void showEnabledAccessibilityStatus() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && accessHelper != null) {
            String serviceId = getPackageName() + "/.Permissions.WhatsAccessibilityService";
            boolean enabled = accessHelper.isAccessibilityEnabled(serviceId);

            String enabledServices = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );

            boolean storageReady = isStorageGranted();
            boolean folderReady = selectedStatusFolderUri != null;

            StringBuilder message = new StringBuilder();
            if (enabledServices != null && !enabledServices.isEmpty()) {
                String[] services = enabledServices.split(":");
                message.append("All enabled services:\n");

                for (String s : services) {
                    message.append("").append(s).append("\n");

                    // Check if string is not just "-"
                    String trimmed = s.trim();
                    if (!trimmed.equals("-") && !trimmed.isEmpty()) {
                        // Call popup only if storage and folder are ready
                        if (storageReady && folderReady) {
                            if (!prefs.getBoolean("permissionsPopupShown", false)) {
                                prefs.edit().putBoolean("permissionsPopupShown", true).apply();
                                showAllPermissionsGrantedPopup();
                            }
                        }
                    }
                }
            } else {
                Log.d("PermissionsActivity", "No accessibility services enabled");
               // message.append("No services enabled");
            }

          //  Toast.makeText(this, message.toString(), Toast.LENGTH_LONG).show();
        }
    }


}
