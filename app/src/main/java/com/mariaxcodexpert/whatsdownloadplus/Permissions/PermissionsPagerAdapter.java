package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

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
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(viewType, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int layoutRes = layouts[position];

        // Page 1: Select App
        if (layoutRes == R.layout.layout_select_app) {
            Button selectWhatsapp = holder.itemView.findViewById(R.id.selectWhatsappButton);
            if (selectWhatsapp != null) {
                selectWhatsapp.setOnClickListener(v -> {
                    Toast.makeText(context, "WhatsApp selected ✅", Toast.LENGTH_SHORT).show();
                    viewPager.setCurrentItem(position + 1, true);
                });
            }
        }

        // Page 2: Permissions
        else if (layoutRes == R.layout.layout_permissions) {
            // Get buttons from the page layout
            Button storageBtn = holder.itemView.findViewById(R.id.allowStorageButton);
            Button notificationBtn = holder.itemView.findViewById(R.id.allowNotificationButton);
            Button statusFolderBtn = holder.itemView.findViewById(R.id.allowStatusFolderButton);

            // Hide notification button for Android 10 below
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && notificationBtn != null) {
                notificationBtn.setVisibility(View.GONE);
            }

            // Setup Storage button
            if (storageBtn != null) {
                activity.updatePermissionButtonUI(storageBtn, activity.isStorageGranted());
                storageBtn.setOnClickListener(v -> {
                    activity.requestStoragePermission();
                    storageBtn.postDelayed(() -> {
                        activity.updatePermissionButtonUI(storageBtn, activity.isStorageGranted());
                        activity.checkAllPermissionsAndProceed();
                    }, 500);
                });
            }

            // Setup Notification button (Android 10+ only)
            if (notificationBtn != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                activity.updatePermissionButtonUI(notificationBtn, activity.isNotificationPermissionGranted());
                notificationBtn.setOnClickListener(v -> {
                    if (!activity.isNotificationPermissionGranted()) {
                        activity.showNotificationAccessDialog();
                    } else {
                        activity.updatePermissionButtonUI(notificationBtn, true);
                        activity.checkAllPermissionsAndProceed();
                    }
                });
            }

            // Setup Status Folder button
            if (statusFolderBtn != null) {
                activity.updatePermissionButtonUI(statusFolderBtn, activity.selectedStatusFolderUri != null);

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    // For Android 9 and below, auto detect folder
                    statusFolderBtn.setEnabled(false);
                    statusFolderBtn.setText("✅");
                    statusFolderBtn.setAlpha(0.7f);
                    activity.detectStatusFolder();
                } else {
                    // For Android 10+, allow user to pick folder
                    statusFolderBtn.setOnClickListener(v -> {
                        activity.openStatusFolderPicker();
                        statusFolderBtn.postDelayed(() -> {
                            activity.updatePermissionButtonUI(statusFolderBtn, activity.selectedStatusFolderUri != null);
                            activity.checkAllPermissionsAndProceed();
                        }, 500);
                    });
                }
            }
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    // Call this to refresh the buttons dynamically
    public void refreshPermissionsPage() {
        for (int i = 0; i < getItemCount(); i++) {
            if (layouts[i] == R.layout.layout_permissions) {
                notifyItemChanged(i);
                break;
            }
        }
    }

}
