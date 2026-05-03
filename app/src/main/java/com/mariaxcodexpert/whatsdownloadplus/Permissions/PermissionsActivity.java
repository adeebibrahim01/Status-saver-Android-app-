package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.mariaxcodexpert.whatsdownloadplus.MainActivity;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.SmartNotify;

public class PermissionsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_STATUS_FOLDER_URI = "statusFolderUri";
    private SharedPreferences prefs;
    public Uri selectedStatusFolderUri;
    private ViewPager2 viewPager;
    private final int[] layouts = {R.layout.layout_select_app, R.layout.layout_permissions};
    private boolean hasNavigated = false;
    private android.view.animation.Animation pulseAnim;
    // 1. Notification Permission Launcher (For Android 13+)
    private final ActivityResultLauncher<String> requestNotificationLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                // Notification permission mile ya na mile, hum folder picker par bhej denge
                openStatusFolderPicker();
            }
    );

    // 2. Folder Picker Launcher (SAF)
    private final ActivityResultLauncher<Intent> folderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleFolderSelection(result.getData().getData());
                } else {
                    SmartNotify.warning(findViewById(android.R.id.content), "Selection required! ⚠️");
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

        findViewById(R.id.btnRight).setOnClickListener(v -> {
            if (viewPager.getCurrentItem() == 0) {
                if (isWhatsappSelected()) {
                    viewPager.setCurrentItem(1, true);
                } else {
                    SmartNotify.warning(v, "Please click the checkbox first to proceed. 🚀");
                }
            } else {
                startPermissionFlow();
            }
        });

        restoreFolderUri();

        if (isAlreadyGranted()) {
            redirectToMain();
        }
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                // Agar pehla page (R.layout.layout_select_app) load hua hai
                if (position == 0) {
                    // Thoda sa delay dete hain taake view render ho jaye
                    viewPager.postDelayed(() -> {
                        // ViewPager ke current fragment/view se checkbox dhondty hain
                        View currentView = ((ViewGroup) viewPager.getChildAt(0)).getChildAt(0);
                        if (currentView != null) {
                            CheckBox cb = currentView.findViewById(R.id.selectWhatsappcheckbox);
                            if (cb != null && !cb.isChecked()) {
                                // Animation shuru karein
                                pulseAnim = android.view.animation.AnimationUtils.loadAnimation(PermissionsActivity.this, R.anim.pulse);
                                cb.startAnimation(pulseAnim);
                            }
                        }
                    }, 200); // 200ms ka delay safe hai
                }
            }
        });
    }

    private boolean isWhatsappSelected() {
        View currentView = ((ViewGroup) viewPager.getChildAt(0)).getChildAt(viewPager.getCurrentItem());
        if (currentView != null) {
            CheckBox cb = currentView.findViewById(R.id.selectWhatsappcheckbox);
            return cb != null && cb.isChecked();
        }
        return false;
    }

    public void openStatusFolderPicker() {
        // Direct Android 11+ / Android 10 path setup
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

    private void startPermissionFlow() {
        // 🔥 Android 13+ ke liye Notification mangna zaroori hai agar app reject hone se bachani hai
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                openStatusFolderPicker();
            }
        } else {
            // Android 10, 11, 12 ke liye direct picker
            openStatusFolderPicker();
        }
    }

    private void handleFolderSelection(Uri uri) {
        if (isValidWhatsAppFolder(uri)) {
            try {
                // 🔥 UPDATE: READ aur WRITE dono flags lein taake access permanent ho jaye
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

                getContentResolver().takePersistableUriPermission(uri, takeFlags);

                saveAndRedirect(uri);
            } catch (Exception e) {
                e.printStackTrace();
                SmartNotify.error(findViewById(android.R.id.content), "Permission error! Please grant access. ⚠️");
            }
        } else {
            SmartNotify.warning(findViewById(android.R.id.content), "Select '.Statuses' folder to proceed! 📁");
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

    private boolean isAlreadyGranted() {
        if (selectedStatusFolderUri == null) return false;
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


    public void showGuideBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme);

        // layout_guide_bottom_sheet wahi layout hai jo aapne guide dikhane ke liye banaya hoga
        View bottomSheetView = getLayoutInflater().inflate(R.layout.layout_guide_bottom_sheet, null);
        bottomSheetView.findViewById(R.id.btnGotIt).setOnClickListener(view -> bottomSheetDialog.dismiss());
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }
}