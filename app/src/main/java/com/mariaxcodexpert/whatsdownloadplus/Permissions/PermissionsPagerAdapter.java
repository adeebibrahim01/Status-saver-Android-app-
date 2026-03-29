package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.mariaxcodexpert.whatsdownloadplus.R;

public class PermissionsPagerAdapter extends RecyclerView.Adapter<PermissionsPagerAdapter.ViewHolder> {

    private final int[] layouts;
    private final Context context;
    private final ViewPager2 viewPager;
    private final PermissionsActivity activity; // Activity reference

    public PermissionsPagerAdapter(@NonNull Context context, int[] layouts, ViewPager2 viewPager) {
        this.context = context;
        this.layouts = layouts;
        this.viewPager = viewPager;
        this.activity = (PermissionsActivity) context; // Context ko cast kiya
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(viewType, parent, false);
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Layout check karne ka sahi tareeka position ke bajaye 'getItemViewType' hai
        int viewType = getItemViewType(position);

        if (viewType == R.layout.layout_permissions) {
            // Check karein ke button null toh nahi
            if (holder.btnGuide != null) {
                holder.btnGuide.setOnClickListener(v -> {
                    // Debugging ke liye Toast (Agar ye dikhayi de toh matlab click kaam kar raha hai)
                    // Toast.makeText(context, "Guide Clicked", Toast.LENGTH_SHORT).show();
                    activity.showGuideBottomSheet();
                });
            }

            if (holder.statusFolderBtn != null) {
                holder.statusFolderBtn.setOnClickListener(v -> activity.openStatusFolderPicker());
            }
        } else if (viewType == R.layout.layout_select_app) {
            if (holder.whatsappCheckbox != null) {
                holder.whatsappCheckbox.setOnClickListener(v -> {
                    if (holder.whatsappCheckbox.isChecked()) {
                        new Handler(Looper.getMainLooper()).postDelayed(() ->
                                viewPager.setCurrentItem(1, true), 400);
                    }
                });
            }
        }
    }

    // ViewHolder mein IDs ko dhyan se check karein
    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox whatsappCheckbox;
        Button statusFolderBtn;
        Button btnGuide;

        ViewHolder(@NonNull View itemView, int layout) {
            super(itemView);
            if (layout == R.layout.layout_select_app) {
                whatsappCheckbox = itemView.findViewById(R.id.selectWhatsappcheckbox);
            } else if (layout == R.layout.layout_permissions) {
                statusFolderBtn = itemView.findViewById(R.id.allowStatusFolderButton);
                // Zaroori: Check karein layout_permissions.xml mein ID exactly 'statusfolder' hi hai?
                btnGuide = itemView.findViewById(R.id.statusfolder);
            }
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

}