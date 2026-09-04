package com.mariaxcodexpert.whatsdownloadplus.Helper;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mariaxcodexpert.whatsdownloadplus.MainActivity;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.databinding.ActivityMainBinding;

public class PermissionManager {
    private final Activity activity;
    private final ActivityMainBinding binding;
    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_LAST_PROMPT = "last_notif_prompt";
    private static final long COOLDOWN_PERIOD = 172800000;


    public PermissionManager(MainActivity activity, ActivityMainBinding binding) {
        this.activity = activity;
        this.binding = binding;
        this.requestNotificationPermissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    updateNotificationDot();
                    showBatteryOptimizationDialog();
                });
    }
    public PermissionManager(Activity activity, ActivityMainBinding binding) {
        this.activity = activity;
        this.binding = binding;
        this.requestNotificationPermissionLauncher = ((AppCompatActivity) activity).registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    updateNotificationDot();
                    showBatteryOptimizationDialog();
                });
    }
    public boolean isNotificationPermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public void checkAndShowNotificationPrompt(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            long lastPromptTime = prefs.getLong(KEY_LAST_PROMPT, 0);
            boolean isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

            if (!isGranted && (System.currentTimeMillis() - lastPromptTime > COOLDOWN_PERIOD)) {
                updateLastPromptTime();
                askNotificationPermission();
            }
        }
    }

    private void updateLastPromptTime() {
        activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putLong(KEY_LAST_PROMPT, System.currentTimeMillis()).apply();
    }

    public boolean isBatteryOptimized() {
        PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        return pm != null && !pm.isIgnoringBatteryOptimizations(activity.getPackageName());
    }

    public void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                new MaterialAlertDialogBuilder(activity)
                        .setTitle(activity.getString(R.string.perm_dialog_title_notif))
                        .setMessage(activity.getString(R.string.perm_dialog_desc_notif))
                        .setPositiveButton(activity.getString(R.string.perm_action_allow), (d, i) -> launchScanner())
                        .setNegativeButton(activity.getString(R.string.perm_action_later), (d, i) -> showBatteryOptimizationDialog())
                        .show();
            } else {
                launchScanner();
            }
        }
    }

    private void launchScanner() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    public void showBatteryOptimizationDialog() {
        if (isBatteryOptimized()) {
            View customView = activity.getLayoutInflater().inflate(R.layout.layout_battery_dialog, null);
            androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                    .setView(customView)
                    .setCancelable(false)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            customView.findViewById(R.id.btnLater).setOnClickListener(v -> dialog.dismiss());
            customView.findViewById(R.id.btnSettings).setOnClickListener(v -> {
                dialog.dismiss();
                try {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
                    activity.startActivity(intent);
                } catch (Exception e) {
                    activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
                }
            });
            dialog.show();
        }
    }

    public void updateNotificationDot() {
        if (binding == null || binding.appBarMain == null || binding.appBarMain.toolbar == null) return;

        View dot = binding.appBarMain.toolbar.findViewById(R.id.custom_notif_dot);
        if (dot == null) return;

        boolean isNotifGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isNotifGranted = ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }

        boolean needsAction = !isNotifGranted || isBatteryOptimized();
        dot.setVisibility(needsAction ? View.VISIBLE : View.GONE);
    }
}