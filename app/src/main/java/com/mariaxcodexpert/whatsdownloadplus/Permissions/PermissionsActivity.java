package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
    private Android10AboveActivity android10Above;
    private Intent mainActivityIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_activity);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        mainActivityIntent = new Intent(this, MainActivity.class);

        android10Above = new Android10AboveActivity(this, prefs);

        viewPager = findViewById(R.id.viewPager);
        PermissionsPagerAdapter adapter = new PermissionsPagerAdapter(this, layouts, viewPager);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(layouts.length);

        restoreFolderUri();

        // Instant redirect if storage permission + folder URI already granted
        if (isStorageGranted() && selectedStatusFolderUri != null) {
            redirectToMain();
            return;
        }

        checkIntroCompleted();
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

    private void redirectToMain() {
        startActivity(mainActivityIntent);
        finish();
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
            }, REQUEST_STORAGE);
        } else {
            requestPermissions(new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_STORAGE);
        }
    }

    public void openStatusFolderPicker() {
        if (android10Above != null) {
            android10Above.openStatusFolderPicker();
        }
    }

    public void checkAllPermissionsAndProceed() {
        boolean storage = isStorageGranted();
        boolean folder = selectedStatusFolderUri != null;

        // Refresh buttons immediately
        if (viewPager.getAdapter() instanceof PermissionsPagerAdapter adapter) {
            adapter.refreshPermissionsPage();
        }

        // Instant redirect if permissions + folder granted
        if (storage && folder) {
            redirectToMain();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE) {
            checkAllPermissionsAndProceed();
        }
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
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAllPermissionsAndProceed();
    }
}
