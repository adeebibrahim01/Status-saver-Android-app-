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
import android.view.View;
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

    // Activity Results
    private final ActivityResultLauncher<Intent> folderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleFolderSelection(result.getData().getData());
                } else {
                    Toast.makeText(this, "Selection required!", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_activity);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        viewPager = findViewById(R.id.viewPager);

        // Adapter setup
        PermissionsPagerAdapter adapter = new PermissionsPagerAdapter(this, layouts, viewPager);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false);

        // Navigation Buttons
        findViewById(R.id.btnLeft).setOnClickListener(v -> viewPager.setCurrentItem(0, true));

        findViewById(R.id.btnRight).setOnClickListener(v -> {
            if (viewPager.getCurrentItem() == 0) {
                viewPager.setCurrentItem(1, true);
            } else {
                startPermissionFlow();
            }
        });

        restoreFolderUri();

        if (isAlreadyGranted()) {
            redirectToMain();
        }
    }

    // 🔥 Made Public for Adapter to access
    public void showGuideBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme);

        View bottomSheetView = getLayoutInflater().inflate(R.layout.layout_guide_bottom_sheet, null);

        bottomSheetView.findViewById(R.id.btnGotIt).setOnClickListener(view -> bottomSheetDialog.dismiss());

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    public void openStatusFolderPicker() {
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

    // ... handleFolderSelection, startPermissionFlow, restoreFolderUri, etc. (Previous Logic) ...
    // Note: Make sure to keep the methods you had before like handleFolderSelection below.

    private void handleFolderSelection(Uri uri) {
        if (isValidWhatsAppFolder(uri)) {
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                saveAndRedirect(uri);
            } catch (Exception e) {
                Toast.makeText(this, "Permission error!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Select '.Statuses' folder!", Toast.LENGTH_LONG).show();
            openStatusFolderPicker();
        }
    }

    private boolean isValidWhatsAppFolder(Uri treeUri) {
        if (treeUri == null) return false;
        return Uri.decode(treeUri.toString()).toLowerCase().contains(".statuses");
    }

    private void saveAndRedirect(Uri uri) {
        selectedStatusFolderUri = uri;
        prefs.edit().putString(KEY_STATUS_FOLDER_URI, uri.toString()).apply();
        prefs.edit().putBoolean("introCompleted", true).apply();
        redirectToMain();
    }

    private void startPermissionFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            openStatusFolderPicker();
        } else {
            // Android 9 logic (already in your code)
            checkAndSetLegacyPath();
        }
    }

    private void checkAndSetLegacyPath() {
        String path = Environment.getExternalStorageDirectory().getPath() + "/WhatsApp/Media/.Statuses";
        File f = new File(path);
        if (f.exists()) {
            saveAndRedirect(Uri.fromFile(f));
        } else {
            openStatusFolderPicker();
        }
    }

    private boolean isAlreadyGranted() {
        if (selectedStatusFolderUri == null) return false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true;
        return getContentResolver().getPersistedUriPermissions().stream()
                .anyMatch(p -> p.getUri().equals(selectedStatusFolderUri));
    }

    private void restoreFolderUri() {
        String savedUri = prefs.getString(KEY_STATUS_FOLDER_URI, null);
        if (savedUri != null) selectedStatusFolderUri = Uri.parse(savedUri);
    }

    private void redirectToMain() {
        if (hasNavigated) return;
        hasNavigated = true;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}