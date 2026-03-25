package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.mariaxcodexpert.whatsdownloadplus.MainActivity;
import com.mariaxcodexpert.whatsdownloadplus.R;

public class PermissionsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_STATUS_FOLDER_URI = "statusFolderUri";

    private SharedPreferences prefs;
    public Uri selectedStatusFolderUri;
    private ViewPager2 viewPager;
    private final int[] layouts = {R.layout.layout_select_app, R.layout.layout_permissions};
    private boolean hasNavigated = false;

    // 1. Modern Launcher implementation (startActivityForResult ka replacement)
    private final ActivityResultLauncher<Intent> folderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();

                    if (isValidWhatsAppFolder(uri)) {
                        // ✅ SAHI FOLDER: Permission save karein aur aage barhein
                        try {
                            getContentResolver().takePersistableUriPermission(uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            selectedStatusFolderUri = uri;
                            prefs.edit().putString(KEY_STATUS_FOLDER_URI, uri.toString()).apply();
                            prefs.edit().putBoolean("introCompleted", true).apply();

                            redirectToMain();
                        } catch (Exception e) {
                            Toast.makeText(this, "Permission error!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // ❌ GHALAT FOLDER: User ko wapas bhejein
                        Toast.makeText(this, "Please select only the WhatsApp .Statuses folder.", Toast.LENGTH_LONG).show();
                        openStatusFolderPicker(); // Loop back to picker
                    }
                } else {
                    // User ne cancel kiya
                    Toast.makeText(this, "Folder selection is required!", Toast.LENGTH_SHORT).show();
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
        findViewById(R.id.btnRight).setOnClickListener(v -> openStatusFolderPicker());

        restoreFolderUri();

        // Check if already granted
        if (isValidWhatsAppFolder(selectedStatusFolderUri)) {
            redirectToMain();
        }
    }

    private void restoreFolderUri() {
        String savedUri = prefs.getString(KEY_STATUS_FOLDER_URI, null);
        if (savedUri != null) {
            selectedStatusFolderUri = Uri.parse(savedUri);
            try {
                getContentResolver().takePersistableUriPermission(
                        selectedStatusFolderUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } catch (Exception e) {
                selectedStatusFolderUri = null;
            }
        }
    }

    public void openStatusFolderPicker() {
        // Android 11+ ke liye exact path
        String folderPath = "primary:Android/media/com.whatsapp/WhatsApp/Media/.Statuses";

        Uri pickerInitialUri = DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents",
                folderPath
        );

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        );

        // System ko hint dena k kahan khulna hai
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        try {
            folderPickerLauncher.launch(intent);
            Toast.makeText(this, "Click the USE THIS FOLDER button below.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            // Fallback agar direct path kaam na kare
            folderPickerLauncher.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE));
        }
    }

    private boolean isValidWhatsAppFolder(Uri treeUri) {
        if (treeUri == null) return false;
        String path = treeUri.toString();
        // Decode path check for WhatsApp Status
        return path.contains("com.whatsapp") && path.contains(".Statuses");
    }

    private void redirectToMain() {
        if (hasNavigated) return;
        hasNavigated = true;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}