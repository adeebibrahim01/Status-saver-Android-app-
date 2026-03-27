package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.ui.Download.FullScreenMediaActivity;

import java.util.ArrayList;
import java.util.List;

public class RecentDownloadsAdapter
        extends RecyclerView.Adapter<RecentDownloadsAdapter.ViewHolder> {

    // 1. Context ko private final nahi rakha taake memory leak na ho
    private final List<MediaItem> items;
    private final TextView emptyMessage;

    public RecentDownloadsAdapter(List<MediaItem> items, @Nullable TextView emptyMessage) {
        // Hamesha new list banayein taake original list clear hone par adapter crash na ho
        this.items = new ArrayList<>(items);
        this.emptyMessage = emptyMessage;

        setHasStableIds(true); // 🚀 PERFORMANCE BOOST
        updateEmptyState();
    }

    // =========================
    // ViewHolder (Click Listener optimized)
    // =========================
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumb, videoIcon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumb = itemView.findViewById(R.id.imgThumb);
            videoIcon = itemView.findViewById(R.id.videoIcon);
        }
    }

    // =========================
    // Safe Unique ID (Prevents Blinking)
    // =========================
    @Override
    public long getItemId(int position) {
        if (position < items.size()) {
            Uri uri = items.get(position).uri;
            return uri != null ? uri.hashCode() : position;
        }
        return position;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recent_download_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MediaItem item = items.get(position);
        if (item == null || item.uri == null) return;

        // Glide Loading (Same as before)
        Glide.with(holder.imgThumb.getContext())
                .load(item.uri)
                .thumbnail(0.15f)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.image_bg)
                .error(R.drawable.ic_download)
                .into(holder.imgThumb);

        holder.videoIcon.setVisibility(item.isVideo ? View.VISIBLE : View.GONE);

        // 🔥 CLICK FIXED HERE
        holder.itemView.setOnClickListener(v -> {
            try {
                Context ctx = v.getContext();
                Intent intent = new Intent(ctx, FullScreenMediaActivity.class);

                // 1. Keys ko check karein (Static variables use karna behtar hai)
                intent.putExtra(FullScreenMediaActivity.EXTRA_URI, item.uri.toString());
                intent.putExtra(FullScreenMediaActivity.EXTRA_IS_VIDEO, item.isVideo);

                // 2. Data aur Type set karna professional tareeka hai
                intent.setData(item.uri);
                if (item.isVideo) {
                    intent.setType("video/*");
                } else {
                    intent.setType("image/*");
                }

                // 3. Permission grant (Agar file scoped storage mein hai)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                ctx.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // =========================
    // 4. SMART DATA UPDATE (No more notifyDataSetChanged)
    // =========================
    public void updateData(List<MediaItem> newItems) {
        // DiffUtil use karne se sirf wo items change honge jo naye hain
        // Isse scrolling ke waqt jhatka (jank) nahi lagta
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return items.size(); }
            @Override
            public int getNewListSize() { return newItems.size(); }
            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return items.get(oldPos).uri.equals(newItems.get(newPos).uri);
            }
            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                return items.get(oldPos).equals(newItems.get(newPos));
            }
        });

        items.clear();
        items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (emptyMessage != null) {
            emptyMessage.post(() -> emptyMessage.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE));
        }
    }
}