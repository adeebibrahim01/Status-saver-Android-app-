package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
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

    // Flags
    private boolean introStarted = false;
    private boolean hasNavigated = false;

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

        if (prefs.getBoolean("introCompleted", false) && isStorageGranted() && selectedStatusFolderUri != null) {
            redirectToMain();
        } else {
            introStarted = true;
            viewPager.setCurrentItem(0, false); // Show first page initially
        }
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

    // -----------------------------
    // Storage permission methods
    // -----------------------------
    public boolean isStorageGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            return checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else { // Android 10 - 12
            return checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    public void requestStoragePermission() {
        if (isStorageGranted()) return; // Already granted

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            requestPermissions(new String[]{
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VIDEO
            }, REQUEST_STORAGE);
        } else { // Android 10 - 12
            requestPermissions(new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
            }, REQUEST_STORAGE);
        }
    }

    // -----------------------------
    // SAF folder picker
    // -----------------------------
    public void openStatusFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_FOLDER);
    }

    // -----------------------------
    // Validate all permissions
    // -----------------------------
    public void checkAllPermissionsAndProceed() {
        if (!introStarted || hasNavigated) return; // safe check



        validateFolderPermission();

        boolean storage = isStorageGranted();
        boolean folder = selectedStatusFolderUri != null;

        if (storage && folder) {
            prefs.edit().putBoolean("introCompleted", true).apply();
            redirectToMain();
        }

        if (viewPager.getAdapter() instanceof PermissionsPagerAdapter adapter) {
            adapter.refreshPermissionsPage();
        }

    }

    private void validateFolderPermission() {
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
                selectedStatusFolderUri = null;
                prefs.edit().remove(KEY_STATUS_FOLDER_URI).apply();
                Toast.makeText(this, "Incorrect folder selected. Please select the WhatsApp .Statuses folder.", Toast.LENGTH_LONG).show();
            }
        }
    }


    boolean isValidWhatsAppFolder(Uri treeUri) {
        try {
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
            if (pickedDir == null || !pickedDir.isDirectory()) return false;

            // Folder name check (.Statuses)
            String name = pickedDir.getName();
            if (name == null || !name.equalsIgnoreCase(".Statuses")) {
                return false;
            }

            // Optional: check at least one media file exists
            for (DocumentFile file : pickedDir.listFiles()) {
                if (file.isFile()) {
                    String fileName = file.getName();
                    if (fileName != null &&
                            (fileName.endsWith(".jpg")
                                    || fileName.endsWith(".png")
                                    || fileName.endsWith(".mp4"))) {
                        return true; // VALID WhatsApp Status folder
                    }
                }
            }

            // Folder empty ho sakta hai, phir bhi valid
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    // -----------------------------
    // Handle permission results
    // -----------------------------
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_STORAGE) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            if (!granted) {
                boolean permanentlyDenied = false;
                for (String permission : permissions) {
                    if (!shouldShowRequestPermissionRationale(permission)) {
                        permanentlyDenied = true;
                        break;
                    }
                }

                if (permanentlyDenied) {
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

    // -----------------------------
    // Handle folder picker result
    // -----------------------------
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

    private void redirectToMain() {
        if (hasNavigated) return;
        hasNavigated = true;

        startActivity(mainActivityIntent);
        finish();
    }
}
