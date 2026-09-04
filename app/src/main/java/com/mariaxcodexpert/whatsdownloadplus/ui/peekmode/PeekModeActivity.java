package com.mariaxcodexpert.whatsdownloadplus.ui.peekmode;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase;
import com.mariaxcodexpert.whatsdownloadplus.data.local.PeekMessageEntity.PeekMessageEntity;
import com.mariaxcodexpert.whatsdownloadplus.Helper.PermissionManager;

import java.util.ArrayList;
import java.util.List;

public class PeekModeActivity extends AppCompatActivity {
    private static final String TAG = "PeekModeActivity";

    private PeekModeViewModel viewModel;
    private SwitchCompat switchPeek;
    private SharedPreferences prefs;
    private RecyclerView recyclerView;
    private PeekModeAdapter adapter;
    private PermissionManager permissionManager;

    private LinearLayout layoutEmptyState;
    private ImageView imgState;
    private TextView txtStateTitle, txtStateDesc;

    // Flag to prevent infinite loop / unwanted triggers when programmatically setting switch state
    private boolean isUpdatingSwitch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peek_mode);

        try {
            permissionManager = new PermissionManager(this, null);
            prefs = getSharedPreferences("peek_settings", MODE_PRIVATE);

            switchPeek = findViewById(R.id.switchPeek);
            recyclerView = findViewById(R.id.recyclerView);
            layoutEmptyState = findViewById(R.id.layoutEmptyState);
            imgState = findViewById(R.id.imgState);
            txtStateTitle = findViewById(R.id.txtStateTitle);
            txtStateDesc = findViewById(R.id.txtStateDesc);

            adapter = new PeekModeAdapter(new ArrayList<>());
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);

            ImageView btnBack = findViewById(R.id.btnBack);
            btnBack.setOnClickListener(v -> finish());

            // Setup listener BEFORE updating state UI
            switchPeek.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isUpdatingSwitch) return;

                boolean hasPermission = isNotificationServiceEnabled();

                if (isChecked && !hasPermission) {
                    // User wants to turn ON, but permission is missing -> Request it
                    prefs.edit().putBoolean("is_peek_on", true).apply();
                    if (permissionManager != null) {
                        permissionManager.checkAndShowNotificationPrompt(this);
                    }
                    startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));

                } else if (!isChecked && hasPermission) {
                    // User wants to turn OFF, but system permission is still active -> Ask them to disable in settings
                    showDisablePermissionDialog();
                } else {
                    // Normal toggle scenario
                    prefs.edit().putBoolean("is_peek_on", isChecked).apply();
                    updateUIState();
                }
            });

            viewModel = new ViewModelProvider(this).get(PeekModeViewModel.class);

            // Observer
            viewModel.getMessagesLiveData().observe(this, messages -> {
                if (adapter != null) {
                    adapter.updateData(messages);
                }
                updateUIState();
            });

            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    int position = viewHolder.getAdapterPosition();
                    List<PeekMessageEntity> currentList = viewModel.getMessagesLiveData().getValue();
                    if (currentList != null && position < currentList.size()) {
                        PeekMessageEntity msgToRemove = currentList.get(position);
                        new Thread(() -> {
                            try {
                                AppDatabase db = AppDatabase.getInstance(PeekModeActivity.this);
                                db.peekDao().deleteBySender(msgToRemove.senderName, msgToRemove.userId);
                                runOnUiThread(() -> viewModel.loadMessages());
                            } catch (Exception e) {
                                Log.e(TAG, "Error deleting peek message: " + e.getMessage(), e);
                            }
                        }).start();
                    }
                }
            });
            itemTouchHelper.attachToRecyclerView(recyclerView);

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
        }
    }

    private void updateSwitchStateUI() {
        isUpdatingSwitch = true;
        boolean hasPermission = isNotificationServiceEnabled();

        // Agar system permission hi off ho chuki hai, toh preference ko bhi automatically false kar dein
        if (!hasPermission) {
            prefs.edit().putBoolean("is_peek_on", false).apply();
        }

        boolean isPrefOn = prefs.getBoolean("is_peek_on", false);
        switchPeek.setChecked(hasPermission && isPrefOn);
        isUpdatingSwitch = false;
    }

    private void showDisablePermissionDialog() {
        try {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Disable Peek Mode Permission")
                    .setMessage("To turn off Peek Mode, please disable notification access for this app in your device settings.")
                    .setPositiveButton("Open Settings", (dialogInterface, which) -> {
                        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    })
                    .setNegativeButton("Cancel", (dialogInterface, which) -> {
                        // Revert switch back to ON since permission is still active
                        updateSwitchStateUI();
                    })
                    .setOnCancelListener(dialogInterface -> updateSwitchStateUI())
                    .create();

            dialog.show();

            if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.WHITE);
            }
            if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.WHITE);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error showing disable permission dialog: " + e.getMessage(), e);
        }
    }

    private void updateUIState() {
        boolean isPermissionGranted = isNotificationServiceEnabled();
        boolean isPrefOn = prefs.getBoolean("is_peek_on", false);

        // Agar permission nahi hai ya preference off hai, toh messages hide karke lock/disabled screen dikhayein
        List<PeekMessageEntity> messages = viewModel.getMessagesLiveData().getValue();
        boolean isEmpty = (messages == null || messages.isEmpty());

        int colorGrey = android.graphics.Color.parseColor("#424242");
        txtStateTitle.setTextColor(colorGrey);
        txtStateDesc.setTextColor(colorGrey);

        if (!isPermissionGranted || !isPrefOn) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);

            imgState.setImageResource(R.drawable.ic_permission_lock);
            txtStateTitle.setText(R.string.peek_title_enable_access);
            txtStateDesc.setText(R.string.peek_desc_enable_access);

        } else if (isEmpty) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);

            imgState.setImageResource(R.drawable.ic_peek_empty);
            txtStateTitle.setText(R.string.peek_title_no_alerts);
            txtStateDesc.setText(R.string.peek_desc_no_alerts);

        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private boolean isNotificationServiceEnabled() {
        try {
            String pkgName = getPackageName();
            String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
            return flat != null && flat.contains(pkgName);
        } catch (Exception e) {
            Log.e(TAG, "Error checking notification service: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            updateSwitchStateUI();
            updateUIState();
            if (recyclerView != null) {
                recyclerView.postDelayed(() -> {
                    if (viewModel != null) {
                        viewModel.loadMessages();
                    }
                }, 300);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume: " + e.getMessage(), e);
        }
    }
}