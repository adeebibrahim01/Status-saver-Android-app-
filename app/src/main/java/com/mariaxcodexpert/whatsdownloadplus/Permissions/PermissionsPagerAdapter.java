package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.mariaxcodexpert.whatsdownloadplus.R;

public class PermissionsPagerAdapter extends RecyclerView.Adapter<PermissionsPagerAdapter.ViewHolder> {

    private final int[] layouts;
    private final Context context;
    private final ViewPager2 viewPager;
    private final PermissionsActivity activity;

    public PermissionsPagerAdapter(@NonNull Context context, @NonNull int[] layouts, @NonNull ViewPager2 viewPager) {
        this.context = context;
        this.layouts = layouts;
        this.viewPager = viewPager;
        this.activity = (PermissionsActivity) context;
        viewPager.setOffscreenPageLimit(layouts.length);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(viewType, parent, false);
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int layoutRes = layouts[position];

        // -------------------------
        // Page 1: Select WhatsApp
        // -------------------------
        if (layoutRes == R.layout.layout_select_app && holder.whatsappCheckbox != null) {
            holder.whatsappCheckbox.setOnClickListener(v -> {
                if (holder.whatsappCheckbox.isChecked()) {
                    Toast.makeText(context, "WhatsApp Selected", Toast.LENGTH_SHORT).show();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> viewPager.setCurrentItem(position + 1, true), 400);
                } else {
                    Toast.makeText(context, "You must select WhatsApp to continue", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // -------------------------
        // Page 2: Storage & Status Folder
        // -------------------------
        else if (layoutRes == R.layout.layout_permissions) {

            // Storage button
            if (holder.storageBtn != null) {
                boolean storageGranted = activity.isStorageGranted();

                // Update button based on current API level
                updateButtonState(holder.storageBtn, storageGranted, "Allow Storage");

                holder.storageBtn.setOnClickListener(v -> activity.requestStoragePermission());
            }

            // Status folder button
            if (holder.statusFolderBtn != null) {
                boolean folderGranted = activity.selectedStatusFolderUri != null;

                // Validate folder
                if (folderGranted && !activity.isValidWhatsAppFolder(activity.selectedStatusFolderUri)) {
                    folderGranted = false;
                    activity.selectedStatusFolderUri = null;
                    Toast.makeText(context, "Incorrect folder selected. Please select WhatsApp .Statuses folder.", Toast.LENGTH_LONG).show();
                }

                updateButtonState(holder.statusFolderBtn, folderGranted, "Select Folder");

                if (!folderGranted) {
                    holder.statusFolderBtn.setOnClickListener(v -> activity.openStatusFolderPicker());
                } else {
                    holder.statusFolderBtn.setEnabled(false);
                    holder.statusFolderBtn.setAlpha(0.7f);
                }
            }
        }
    }

    /** Helper to update button UI based on granted state */
    private void updateButtonState(Button btn, boolean granted, String defaultLabel) {
        if (granted) {
            btn.setText("✅");
            btn.setEnabled(false);
            btn.setAlpha(0.7f);
        } else {
            btn.setText(defaultLabel);
            btn.setEnabled(true);
            btn.setAlpha(1f);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return layouts[position];
    }

    @Override
    public int getItemCount() {
        return layouts.length;
    }

    // -------------------------
    // ViewHolder
    // -------------------------
    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox whatsappCheckbox;
        Button storageBtn, statusFolderBtn;

        public ViewHolder(@NonNull View itemView, int layoutRes) {
            super(itemView);
            if (layoutRes == R.layout.layout_select_app) {
                whatsappCheckbox = itemView.findViewById(R.id.selectWhatsappcheckbox);
            } else if (layoutRes == R.layout.layout_permissions) {
                storageBtn = itemView.findViewById(R.id.allowStorageButton);
                statusFolderBtn = itemView.findViewById(R.id.allowStatusFolderButton);
            }
        }
    }

    /** Refresh only the permissions page */
    public void refreshPermissionsPage() {
        for (int i = 0; i < getItemCount(); i++) {
            if (layouts[i] == R.layout.layout_permissions) {
                notifyItemChanged(i);
                break;
            }
        }
    }
}
