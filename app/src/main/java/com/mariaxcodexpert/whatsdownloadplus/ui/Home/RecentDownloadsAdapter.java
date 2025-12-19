package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.ui.Download.FullScreenMediaActivity;

import java.util.List;

public class RecentDownloadsAdapter
        extends RecyclerView.Adapter<RecentDownloadsAdapter.ViewHolder> {

    private final Context context;
    private final List<MediaItem> items;
    private final TextView emptyMessage;

    public RecentDownloadsAdapter(Context context,
                                  List<MediaItem> items,
                                  TextView emptyMessage) {
        this.context = context;
        this.items = items;
        this.emptyMessage = emptyMessage;

        setHasStableIds(true); // 🚀 PERFORMANCE BOOST
        updateEmptyState();
    }

    // =========================
    // ViewHolder
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
    // Stable ID
    // =========================
    @Override
    public long getItemId(int position) {
        return items.get(position).uri.hashCode();
    }

    // =========================
    // Create View
    // =========================
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recent_download_item, parent, false);
        return new ViewHolder(view);
    }

    // =========================
    // Bind View
    // =========================
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MediaItem item = items.get(position);

        Glide.with(holder.imgThumb)
                .load(item.uri)
                .thumbnail(0.25f)      // 🚀 FAST PREVIEW
                .centerCrop()
                .dontAnimate()         // 🚀 NO JANK
                .placeholder(R.drawable.image_bg)
                .error(R.drawable.ic_download)
                .into(holder.imgThumb);

        holder.videoIcon.setVisibility(item.isVideo ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, FullScreenMediaActivity.class);
            intent.putExtra(FullScreenMediaActivity.EXTRA_URI, item.uri);
            intent.putExtra(FullScreenMediaActivity.EXTRA_IS_VIDEO, item.isVideo);
            context.startActivity(intent);
        });
    }

    // =========================
    // Count
    // =========================
    @Override
    public int getItemCount() {
        return items.size(); // ⚡ ZERO LOGIC
    }

    // =========================
    // Update Data (call from Fragment)
    // =========================
    public void updateData(List<MediaItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged(); // list <= 10 → safe
        updateEmptyState();
    }

    // =========================
    // Empty State
    // =========================
    private void updateEmptyState() {
        if (emptyMessage == null) return;
        emptyMessage.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
