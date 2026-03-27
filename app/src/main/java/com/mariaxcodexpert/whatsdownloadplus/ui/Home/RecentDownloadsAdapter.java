package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.ui.Download.FullScreenMediaActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecentDownloadsAdapter extends RecyclerView.Adapter<RecentDownloadsAdapter.ViewHolder> {

    private final List<MediaItem> items = new ArrayList<>(); // Initialize directly
    private final TextView emptyMessage;

    public RecentDownloadsAdapter(List<MediaItem> initialItems, @Nullable TextView emptyMessage) {
        if (initialItems != null) {
            this.items.addAll(initialItems);
        }
        this.emptyMessage = emptyMessage;
        setHasStableIds(true);
        updateEmptyState();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumb, videoIcon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumb = itemView.findViewById(R.id.imgThumb);
            videoIcon = itemView.findViewById(R.id.videoIcon);
        }
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).uri.hashCode();
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

        // Thumbnail Loading
        Glide.with(holder.imgThumb.getContext())
                .load(item.uri)
                .thumbnail(0.15f)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.image_bg)
                .error(R.drawable.ic_download)
                .into(holder.imgThumb);

        holder.videoIcon.setVisibility(item.isVideo ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            try {
                Context ctx = v.getContext();
                Uri finalUri = item.uri;

                // 🔥 FIX: Android 9 (File Scheme) handle karne ke liye
                if ("file".equals(item.uri.getScheme())) {
                    String path = item.uri.getPath();
                    if (path != null) {
                        File file = new File(path);
                        if (file.exists()) {
                            // Authority must match Manifest: com.mariaxcodexpert.whatsdownloadplus.fileprovider
                            finalUri = FileProvider.getUriForFile(ctx,
                                    ctx.getPackageName() + ".fileprovider", file);
                        } else {
                            Toast.makeText(ctx, "File path not found!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                }

                // 🔥 Intent Setup
                Intent intent = new Intent(ctx, FullScreenMediaActivity.class);

                // 1. Send as String (Safety for Large Bundles)
                intent.putExtra(FullScreenMediaActivity.EXTRA_URI, finalUri.toString());
                intent.putExtra(FullScreenMediaActivity.EXTRA_IS_VIDEO, item.isVideo);

                // 2. Data and Type (Critical for some Players/Galleries)
                intent.setDataAndType(finalUri, item.isVideo ? "video/*" : "image/*");

                // 3. Security Flags
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // 4. Activity Context Check
                if (!(ctx instanceof android.app.Activity)) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }

                ctx.startActivity(intent);

            } catch (Exception e) {
                e.printStackTrace();
                android.util.Log.e("MediaOpenError", "Error: " + e.getMessage());
                Toast.makeText(v.getContext(), "Error: Make sure file exists", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public int getItemCount() {
        return items.size();
    }

    // 🔥 FIX: Data Update logic ko synchronize kiya hai
    public void updateData(List<MediaItem> newItems) {
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
                return items.get(oldPos).uri.toString().equals(newItems.get(newPos).uri.toString());
            }
        });

        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        diffResult.dispatchUpdatesTo(this);
        updateEmptyState(); // Check again after update
    }

    private void updateEmptyState() {
        if (emptyMessage != null) {
            // Android 9 compatibility: post on UI thread to ensure view is ready
            emptyMessage.post(() -> {
                if (items.isEmpty()) {
                    emptyMessage.setVisibility(View.VISIBLE);
                } else {
                    emptyMessage.setVisibility(View.GONE);
                }
            });
        }
    }
}