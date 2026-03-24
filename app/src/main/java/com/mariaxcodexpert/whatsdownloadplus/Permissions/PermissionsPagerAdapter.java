package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.content.Context;
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

        if (layouts[position] == R.layout.layout_select_app && holder.whatsappCheckbox != null) {
            holder.whatsappCheckbox.setOnClickListener(v -> {
                if (holder.whatsappCheckbox.isChecked()) {
                    Toast.makeText(context, "WhatsApp Selected", Toast.LENGTH_SHORT).show();
                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                            viewPager.setCurrentItem(1, true), 400);
                }
            });
        }

        if (layouts[position] == R.layout.layout_permissions && holder.statusFolderBtn != null) {
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

        ViewHolder(@NonNull View itemView, int layout) {
            super(itemView);

            if (layout == R.layout.layout_select_app) {
                whatsappCheckbox = itemView.findViewById(R.id.selectWhatsappcheckbox);
            } else {
                statusFolderBtn = itemView.findViewById(R.id.allowStatusFolderButton);
            }
        }
    }
}