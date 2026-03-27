package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.mariaxcodexpert.whatsdownloadplus.MainActivity;
import com.mariaxcodexpert.whatsdownloadplus.R;

import java.io.File;

public class PermissionsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_STATUS_FOLDER_URI = "statusFolderUri";

    private SharedPreferences prefs;
    public Uri selectedStatusFolderUri;
    private ViewPager2 viewPager;
    private final int[] layouts = {R.layout.layout_select_app, R.layout.layout_permissions};
    private boolean hasNavigated = false;

    // 1. SAF Folder Picker Launcher (For Android 10/11+)
    private final ActivityResultLauncher<Intent> folderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    handleFolderSelection(uri);
                } else {
                    Toast.makeText(this, "Folder selection is required!", Toast.LENGTH_SHORT).show();
                }
            }
    );

    // 2. Legacy Permission Launcher (For Android 9 and below)
    private final ActivityResultLauncher<String> legacyPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    checkAndSetLegacyPath();
                } else {
                    Toast.makeText(this, "Storage permission is required for Android 9!", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_activity);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        viewPager = findViewById(R.id.viewPager);
        PermissionsPagerAdapter adapter = new PermissionsPagerAdapter(this, layouts, viewPager);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false);

        findViewById(R.id.btnLeft).setOnClickListener(v -> viewPager.setCurrentItem(0, true));
        findViewById(R.id.btnRight).setOnClickListener(v -> startPermissionFlow());

        restoreFolderUri();

        if (isAlreadyGranted()) {
            redirectToMain();
        }
    }

    private void startPermissionFlow() {
        // 🔥 Android 10 (API 29) aur us se upar ke liye SAF (Folder Picker)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            openStatusFolderPicker();
        } else {
            // 🔥 Android 9 aur us se niche ke liye Legacy Permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                checkAndSetLegacyPath();
            } else {
                legacyPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void checkAndSetLegacyPath() {
        // Android 9 ke default paths
        String[] paths = {
                Environment.getExternalStorageDirectory().getPath() + "/WhatsApp/Media/.Statuses",
                Environment.getExternalStorageDirectory().getPath() + "/Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
        };

        File selectedFolder = null;
        for (String p : paths) {
            File f = new File(p);
            if (f.exists()) {
                selectedFolder = f;
                break;
            }
        }

        if (selectedFolder != null) {
            Uri uri = Uri.fromFile(selectedFolder);
            saveAndRedirect(uri);
        } else {
            // Agar default path na mile toh manual pick karwaein
            Toast.makeText(this, "WhatsApp folder not found. Please select it manually.", Toast.LENGTH_LONG).show();
            openStatusFolderPicker();
        }
    }

    public void openStatusFolderPicker() {
        // Android 11+ exact path hint
        String folderPath = "primary:Android/media/com.whatsapp/WhatsApp/Media/.Statuses";
        Uri pickerInitialUri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", folderPath);

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        try {
            folderPickerLauncher.launch(intent);
        } catch (Exception e) {
            folderPickerLauncher.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE));
        }
    }

    private void handleFolderSelection(Uri uri) {
        if (isValidWhatsAppFolder(uri)) {
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                saveAndRedirect(uri);
            } catch (Exception e) {
                Toast.makeText(this, "Permission error!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Wrong folder! Please select the '.Statuses' folder.", Toast.LENGTH_LONG).show();
            openStatusFolderPicker();
        }
    }

    private boolean isValidWhatsAppFolder(Uri treeUri) {
        if (treeUri == null) return false;
        String path = Uri.decode(treeUri.toString());
        // Ab hum sirf '.Statuses' check kar rahe hain taake custom paths (GB/Business) bhi chal saken
        return path.toLowerCase().contains(".statuses");
    }

    private void saveAndRedirect(Uri uri) {
        selectedStatusFolderUri = uri;
        prefs.edit().putString(KEY_STATUS_FOLDER_URI, uri.toString()).apply();
        prefs.edit().putBoolean("introCompleted", true).apply();
        redirectToMain();
    }

    private boolean isAlreadyGranted() {
        if (selectedStatusFolderUri == null) return false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true;

        return getContentResolver().getPersistedUriPermissions().stream()
                .anyMatch(p -> p.getUri().equals(selectedStatusFolderUri));
    }

    private void restoreFolderUri() {
        String savedUri = prefs.getString(KEY_STATUS_FOLDER_URI, null);
        if (savedUri != null) {
            selectedStatusFolderUri = Uri.parse(savedUri);
        }
    }

    private void redirectToMain() {
        if (hasNavigated) return;
        hasNavigated = true;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}