package com.mariaxcodexpert.whatsdownloadplus.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.mariaxcodexpert.whatsdownloadplus.MainActivity;
import com.mariaxcodexpert.whatsdownloadplus.R;

public class TutorialHelper {

    public static void show(MainActivity activity) {
        View v = LayoutInflater.from(activity).inflate(R.layout.dialog_how_to_use, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity).setView(v).create();

        ViewPager2 vp = v.findViewById(R.id.viewPagerTutorial);
        TabLayout tabs = v.findViewById(R.id.into_tab_layout);
        Button btn = v.findViewById(R.id.btnDone);

        int[] imgs = {R.drawable.tutorial_step1, R.drawable.tutorial_step2, R.drawable.tutorial_step3};
        String[] info = {
                "Step 1: Watch Status\nView statuses on WhatsApp first.",
                "Step 2: Sync Media\nStatuses will appear here automatically.",
                "Step 3: Save & Share\nTap download to save to your gallery."
        };

        vp.setAdapter(new RecyclerView.Adapter<VH>() {
            @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
                return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_tutorial, p, false));
            }
            @Override public void onBindViewHolder(@NonNull VH h, int p) {
                h.i.setImageResource(imgs[p]);
                h.t.setText(info[p]);
            }
            @Override public int getItemCount() { return imgs.length; }
        });

        new TabLayoutMediator(tabs, vp, (tab, pos) -> {}).attach();
        btn.setOnClickListener(view -> dialog.dismiss());
        dialog.show();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView i; TextView t;
        VH(View v) { super(v); i = v.findViewById(R.id.imgTutorial); t = v.findViewById(R.id.txtTutorial); }
    }
}