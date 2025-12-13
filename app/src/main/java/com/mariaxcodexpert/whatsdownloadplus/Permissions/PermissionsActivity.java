package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.mariaxcodexpert.whatsdownloadplus.MainActivity;
import com.mariaxcodexpert.whatsdownloadplus.R;

public class PermissionsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_STATUS_FOLDER_URI = "statusFolderUri";
    private static final int REQUEST_STORAGE = 2001;
    private static final int REQUEST_FOLDER = 1001;

    private SharedPreferences prefs;
    public Uri selectedStatusFolderUri;

    private ViewPager2 viewPager;
    private int[] layouts = {R.layout.layout_select_app, R.layout.layout_permissions};
    private Intent mainActivityIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_activity);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        mainActivityIntent = new Intent(this, MainActivity.class);

        viewPager = findViewById(R.id.viewPager);
        PermissionsPagerAdapter adapter = new PermissionsPagerAdapter(this, layouts, viewPager);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(layouts.length);

        viewPager.setUserInputEnabled(false);

        findViewById(R.id.btnLeft).setOnClickListener(v -> goToPage1());
        findViewById(R.id.btnRight).setOnClickListener(v -> goToPage2());

        restoreFolderUri();

        if (isStorageGranted() && selectedStatusFolderUri != null) {
            redirectToMain();
            return;
        }

        checkIntroCompleted();
    }

    private void goToPage1() {
        viewPager.setCurrentItem(0, true);
    }

    private void goToPage2() {
        PermissionsPagerAdapter.ViewHolder holder = getWhatsappViewHolder();
        if (holder != null && holder.whatsappCheckbox.isChecked()) {
            viewPager.setCurrentItem(1, true);
        } else {
            Toast.makeText(this, "Please select WhatsApp to continue", Toast.LENGTH_SHORT).show();
        }
    }

    private PermissionsPagerAdapter.ViewHolder getWhatsappViewHolder() {
        RecyclerView recyclerView = (RecyclerView) viewPager.getChildAt(0);
        if (recyclerView != null && recyclerView.getChildCount() > 0) {
            View child = recyclerView.getChildAt(0);
            RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(child);
            if (vh instanceof PermissionsPagerAdapter.ViewHolder) {
                return (PermissionsPagerAdapter.ViewHolder) vh;
            }
        }
        return null;
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

    private void checkIntroCompleted() {
        if (prefs.getBoolean("introCompleted", false)) {
            redirectToMain();
        }
    }

    void redirectToMain() {
        startActivity(mainActivityIntent);
        finish();
    }

    // -------------------------
    // Storage Permission (Android 10+)
    // -------------------------
    public boolean isStorageGranted() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    public void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(new String[]{
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VIDEO
            }, REQUEST_STORAGE);
        }
    }

    // -------------------------
    // Status Folder Permission
    // -------------------------
    public void openStatusFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_FOLDER);
    }

    // -------------------------
    // Check All Permissions & Proceed
    // -------------------------
    public void checkAllPermissionsAndProceed() {
        boolean storage = isStorageGranted();
        boolean folder = selectedStatusFolderUri != null;

        // Validate folder permission
        if (selectedStatusFolderUri != null) {
            boolean validPermission = false;
            for (UriPermission perm : getContentResolver().getPersistedUriPermissions()) {
                if (perm.getUri().equals(selectedStatusFolderUri) && perm.isReadPermission()) {
                    validPermission = true;
                    break;
                }
            }

            if (!validPermission) {
                selectedStatusFolderUri = null;
                prefs.edit().remove(KEY_STATUS_FOLDER_URI).apply();
                Toast.makeText(this, "Folder permission lost. Please select again.", Toast.LENGTH_SHORT).show();
            } else if (!isValidWhatsAppFolder(selectedStatusFolderUri)) {
                // Wrong folder selected
                selectedStatusFolderUri = null;
                prefs.edit().remove(KEY_STATUS_FOLDER_URI).apply();
                Toast.makeText(this, "Incorrect folder selected. Please select the WhatsApp .Statuses folder.", Toast.LENGTH_LONG).show();
            }
        }

        if (viewPager.getAdapter() instanceof PermissionsPagerAdapter adapter) {
            adapter.refreshPermissionsPage();
        }

        if (storage && selectedStatusFolderUri != null) {
            redirectToMain();
        }
    }

    // -------------------------
    // Verify WhatsApp .Statuses folder
    // -------------------------
    boolean isValidWhatsAppFolder(Uri uri) {
        String path = uri.getPath();
        // Check if path contains WhatsApp/Media/.Statuses (case-insensitive)
        return path != null && path.toLowerCase().contains("whatsapp") && path.toLowerCase().contains(".statuses");
    }

    // -------------------------
    // Permission Results
    // -------------------------
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_STORAGE) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            if (!granted) {
                if (!shouldShowRequestPermissionRationale(android.Manifest.permission.READ_MEDIA_IMAGES)) {
                    Toast.makeText(this, "Storage permission denied permanently. Enable in settings.", Toast.LENGTH_LONG).show();
                    openAppSettings();
                } else {
                    Toast.makeText(this, "Storage permission is required to access media.", Toast.LENGTH_SHORT).show();
                }
            }

            checkAllPermissionsAndProceed();
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FOLDER && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    );
                    selectedStatusFolderUri = uri;
                    prefs.edit().putString(KEY_STATUS_FOLDER_URI, uri.toString()).apply();
                    checkAllPermissionsAndProceed();
                } catch (Exception ignored) {
                    Toast.makeText(this, "Cannot access selected folder. Please select again.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAllPermissionsAndProceed();
    }
}
