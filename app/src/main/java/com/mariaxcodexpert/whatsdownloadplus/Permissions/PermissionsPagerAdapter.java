package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.SmartNotify;

public class PermissionsPagerAdapter extends RecyclerView.Adapter<PermissionsPagerAdapter.ViewHolder> {

    private final int[] layouts;
    private final Context context;
    private final ViewPager2 viewPager;
    private final PermissionsActivity activity; // Activity reference correctly used

    public PermissionsPagerAdapter(@NonNull Context context, int[] layouts, ViewPager2 viewPager) {
        this.context = context;
        this.layouts = layouts;
        this.viewPager = viewPager;
        // Casting context to our specific activity to access methods
        this.activity = (PermissionsActivity) context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // viewType represents the layout ID here because of getItemViewType override
        View view = LayoutInflater.from(context).inflate(viewType, parent, false);
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int currentLayout = layouts[position];

        if (currentLayout == R.layout.layout_select_app) {
            setupSelectAppPage(holder);
        } else if (currentLayout == R.layout.layout_permissions) {
            setupPermissionsPage(holder);
        }
    }

    private void setupSelectAppPage(ViewHolder holder) {
        if (holder.whatsappCheckbox != null) {
            holder.whatsappCheckbox.setOnClickListener(v -> {
                if (holder.whatsappCheckbox.isChecked()) {
                    // Smooth transition to next page after selection
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (viewPager != null) viewPager.setCurrentItem(1, true);
                    }, 400);
                } else {
                    SmartNotify.info(v, "WhatsApp selection is required to proceed! ✅");
                }
            });
        }
    }

    private void setupPermissionsPage(ViewHolder holder) {
        // Guide Button (Show BottomSheet)
        if (holder.btnGuide != null) {
            holder.btnGuide.setOnClickListener(v -> activity.showGuideBottomSheet());
        }

        // Folder Picker Button (Calls the fixed SAF logic in PermissionsActivity)
        if (holder.statusFolderBtn != null) {
            holder.statusFolderBtn.setOnClickListener(v -> activity.openStatusFolderPicker());
        }
    }

    @Override
    public int getItemCount() {
        return layouts.length;
    }

    @Override
    public int getItemViewType(int position) {
        return layouts[position];
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox whatsappCheckbox;
        Button statusFolderBtn;
        Button btnGuide;

        ViewHolder(@NonNull View itemView, int layoutId) {
            super(itemView);
            if (layoutId == R.layout.layout_select_app) {
                whatsappCheckbox = itemView.findViewById(R.id.selectWhatsappcheckbox);
            } else if (layoutId == R.layout.layout_permissions) {
                statusFolderBtn = itemView.findViewById(R.id.allowStatusFolderButton);
                btnGuide = itemView.findViewById(R.id.statusfolder);
            }
        }
    }
}