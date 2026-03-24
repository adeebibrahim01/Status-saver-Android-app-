package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.mariaxcodexpert.whatsdownloadplus.MainActivity;
import com.mariaxcodexpert.whatsdownloadplus.R;

public class PermissionsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_STATUS_FOLDER_URI = "statusFolderUri";
    private static final int REQUEST_FOLDER = 1001;

    private SharedPreferences prefs;
    public Uri selectedStatusFolderUri;
    private ViewPager2 viewPager;
    private int[] layouts = {R.layout.layout_select_app, R.layout.layout_permissions};
    private boolean hasNavigated = false;

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
        if (selectedStatusFolderUri != null && isValidWhatsAppFolder(selectedStatusFolderUri)) {
            redirectToMain();
        }
    }

    private void restoreFolderUri() {
        String savedUri = prefs.getString(KEY_STATUS_FOLDER_URI, null);
        if (savedUri != null) {
            selectedStatusFolderUri = Uri.parse(savedUri);
            // Check if permission is still valid
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
        // Android 11+ WhatsApp path
        String folderPath = "primary:Android/media/com.whatsapp/WhatsApp/Media/.Statuses";

        // Correct Tree URI build
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

        // System ko force karna ke wahi folder dikhaye
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        try {
            startActivityForResult(intent, REQUEST_FOLDER);
            Toast.makeText(this, "Click the USE THIS FOLDER button below.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_FOLDER);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FOLDER) {
            if (resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();

                if (uri != null && isValidWhatsAppFolder(uri)) {
                    // ✅ SUCCESS: Sahi folder select kiya
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
                    // ❌ WRONG FOLDER: User ne kahin aur click kiya
                    Toast.makeText(this, "Select only the WhatsApp .Statuses folder.", Toast.LENGTH_LONG).show();

                    // Dobara picker kholna taake user majboor ho sahi select karne pe
                    openStatusFolderPicker();
                }
            } else {
                // User ne cancel kar diya
                Toast.makeText(this, "Folder selection is required!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isValidWhatsAppFolder(Uri treeUri) {
        // Path check: Kya isme WhatsApp aur .Statuses ka naam hai?
        String path = treeUri.toString();
        return path.contains("com.whatsapp") && path.contains(".Statuses");
    }

    private void redirectToMain() {
        if (hasNavigated) return;
        hasNavigated = true;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}