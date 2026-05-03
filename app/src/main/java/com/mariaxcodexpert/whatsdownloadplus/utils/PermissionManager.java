package com.mariaxcodexpert.whatsdownloadplus.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mariaxcodexpert.whatsdownloadplus.MainActivity;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.databinding.ActivityMainBinding;

public class PermissionManager {
    private final MainActivity activity;
    private final ActivityMainBinding binding;
    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher;

    public PermissionManager(MainActivity activity, ActivityMainBinding binding) {
        this.activity = activity;
        this.binding = binding;

        // Launcher initialization
        this.requestNotificationPermissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    updateNotificationDot();
                    // 🔥 Notification ke decision (Allow ya Deny) ke baad Battery check trigger hoga
                    showBatteryOptimizationDialog();
                });
    }

    // 🔥 MainActivity ke toolbar click par ye call karein
    public void handleNotificationButtonClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Step 1: Agar permission nahi hai to mangein
                askNotificationPermission();
            } else {
                // Step 2: Agar Notification granted hai, to check karein battery status
                checkOnlyBattery();
            }
        } else {
            // Lower versions par seedha battery check
            checkOnlyBattery();
        }
    }

    private void checkOnlyBattery() {
        if (isBatteryOptimized()) {
            showBatteryOptimizationDialog();
        } else {
            Toast.makeText(activity, "All alerts are active! ✅", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper to check battery status
    public boolean isBatteryOptimized() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
            return pm != null && !pm.isIgnoringBatteryOptimizations(activity.getPackageName());
        }
        return false;
    }

    // App Start par flow check karne ke liye (Automated)
    public void checkNotificationAndBatteryFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                askNotificationPermission();
            } else {
                showBatteryOptimizationDialog();
            }
        } else {
            showBatteryOptimizationDialog();
        }
    }

    public void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                new MaterialAlertDialogBuilder(activity)
                        .setTitle("Permission Required")
                        .setMessage("Enable notifications to get alerts before statuses expire.")
                        .setPositiveButton("Allow", (d, i) -> launchScanner())
                        .setNegativeButton("Later", (d, i) -> showBatteryOptimizationDialog()) // Deny par battery dialog
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isBatteryOptimized()) {
            // Layout Inflate karein
            View customView = activity.getLayoutInflater().inflate(R.layout.layout_battery_dialog, null);

            androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
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
                    // Seedha App Info page khulega
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                    intent.setData(uri);
                    activity.startActivity(intent);
                } catch (Exception e) {
                    activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
                }
            });

            dialog.show();
        }
    }

    public void updateNotificationDot() {
        View dot = binding.appBarMain.toolbar.findViewById(R.id.custom_notif_dot);
        if (dot == null) return;

        boolean isNotifGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isNotifGranted = ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }

        // 🔥 Dot tab dikhega jab Notification missing ho YA Battery optimization on ho
        boolean needsAction = !isNotifGranted || isBatteryOptimized();
        dot.setVisibility(needsAction ? View.VISIBLE : View.GONE);
    }
}