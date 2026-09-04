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
import com.mariaxcodexpert.whatsdownloadplus.Helper.SmartNotify;

public class PermissionsPagerAdapter extends RecyclerView.Adapter<PermissionsPagerAdapter.ViewHolder> {

    private final int[] layouts;
    private final Context context;
    private final ViewPager2 viewPager;
    private final PermissionsActivity activity;

    public PermissionsPagerAdapter(@NonNull Context context, int[] layouts, ViewPager2 viewPager) {
        this.context = context;
        this.layouts = layouts;
        this.viewPager = viewPager;
        this.activity = (PermissionsActivity) context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
                    v.clearAnimation();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (viewPager != null) viewPager.setCurrentItem(1, true);
                    }, 400);
                } else {
                    SmartNotify.warning(v, context.getString(R.string.perm_warning_checkbox_first)); // 🔥 Changed
                }
            });
        }
    }

    private void setupPermissionsPage(ViewHolder holder) {
        if (holder.btnGuide != null) {
            holder.btnGuide.setOnClickListener(v -> activity.showGuideBottomSheet());
        }

        if (holder.statusFolderBtn != null) {

            android.view.animation.Animation fadeAnim = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.text_fade);
            holder.statusFolderBtn.startAnimation(fadeAnim);

            holder.statusFolderBtn.setOnClickListener(v -> {
                v.clearAnimation();
                activity.openStatusFolderPicker();
            });
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