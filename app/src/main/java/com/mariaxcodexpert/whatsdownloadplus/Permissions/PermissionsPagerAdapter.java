package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

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

        if (layoutRes == R.layout.layout_select_app && holder.selectWhatsapp != null) {
            holder.selectWhatsapp.setOnClickListener(v -> viewPager.setCurrentItem(position + 1, true));
        } else if (layoutRes == R.layout.layout_permissions) {

            // Storage button
            if (holder.storageBtn != null) {
                holder.storageBtn.setOnClickListener(v -> activity.requestStoragePermission());

                if (activity.isStorageGranted()) {
                    holder.storageBtn.setText("Granted ✅");
                    holder.storageBtn.setEnabled(false);
                    holder.storageBtn.setAlpha(0.7f);
                } else {
                    holder.storageBtn.setText("Allow Storage");
                    holder.storageBtn.setEnabled(true);
                    holder.storageBtn.setAlpha(1f);
                }
            }

            // Status folder button
            if (holder.statusFolderBtn != null) {
                if (activity.selectedStatusFolderUri != null) {
                    holder.statusFolderBtn.setText("Granted ✅");
                    holder.statusFolderBtn.setEnabled(false);
                    holder.statusFolderBtn.setAlpha(0.7f);
                } else {
                    holder.statusFolderBtn.setText("Select Folder");
                    holder.statusFolderBtn.setEnabled(true);
                    holder.statusFolderBtn.setAlpha(1f);
                    holder.statusFolderBtn.setOnClickListener(v -> activity.openStatusFolderPicker());
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
        Button selectWhatsapp, storageBtn, statusFolderBtn;

        public ViewHolder(@NonNull View itemView, int layoutRes) {
            super(itemView);
            if (layoutRes == R.layout.layout_select_app)
                selectWhatsapp = itemView.findViewById(R.id.selectWhatsappButton);
            else if (layoutRes == R.layout.layout_permissions) {
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
