package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

public class RecentDownloadsAdapter extends RecyclerView.Adapter<RecentDownloadsAdapter.ViewHolder> {

    private final List<MediaItem> items = new ArrayList<>();
    private final LinearLayout emptyMessage;

    public RecentDownloadsAdapter(List<MediaItem> initialItems, @Nullable LinearLayout emptyMessage) {
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
        // Stable IDs using URI hash for better performance
        return items.get(position).uri.toString().hashCode();
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

        // Thumbnail Loading (Glide handles content URIs perfectly)
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
            Context ctx = v.getContext();

            // 🔥 CLEANED: Direct URI handling (No FileProvider needed)
            Intent intent = new Intent(ctx, FullScreenMediaActivity.class);
            intent.putExtra(FullScreenMediaActivity.EXTRA_URI, item.uri.toString());
            intent.putExtra(FullScreenMediaActivity.EXTRA_IS_VIDEO, item.isVideo);

            // Important for MediaStore URIs
            intent.setDataAndType(item.uri, item.isVideo ? "video/*" : "image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (!(ctx instanceof android.app.Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }

            ctx.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateData(List<MediaItem> newItems) {
        if (newItems == null) {
            items.clear();
            notifyDataSetChanged();
            updateEmptyState();
            return;
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return items.size(); }
            @Override
            public int getNewListSize() { return newItems.size(); }
            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return items.get(oldPos).uri.toString().equals(newItems.get(newPos).uri.toString());
            }
            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                return items.get(oldPos).uri.equals(newItems.get(newPos).uri);
            }
        });

        items.clear();
        items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (emptyMessage != null) {
            emptyMessage.post(() -> {
                emptyMessage.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            });
        }
    }
}