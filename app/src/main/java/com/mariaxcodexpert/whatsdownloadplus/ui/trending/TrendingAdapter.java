package com.mariaxcodexpert.whatsdownloadplus.ui.trending;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.mariaxcodexpert.whatsdownloadplus.R;
import java.util.ArrayList;

public class TrendingAdapter extends RecyclerView.Adapter<TrendingAdapter.ViewHolder> {

    private ArrayList<TrendMediaItem> trendList;
    private OnTrendItemClickListener listener;

    public interface OnTrendItemClickListener {
        void onSetStatus(TrendMediaItem item, ViewHolder holder);
        void onPreview(TrendMediaItem item);
    }

    public TrendingAdapter(ArrayList<TrendMediaItem> trendList, OnTrendItemClickListener listener) {
        this.trendList = trendList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trending, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TrendMediaItem currentItem = trendList.get(position);

        // 1. Title handling
        if (holder.tvTrendTitle != null) {
            holder.tvTrendTitle.setText(currentItem.getTitle() != null ? currentItem.getTitle() : "Trending");
        }

        // 2. 🔥 PREMIUM GLIDE LOADING (OnlineSearch wala logic)
        Glide.with(holder.itemView.getContext())
                .load(currentItem.getThumbnailUrl())
                .placeholder(new ColorDrawable(Color.parseColor("#F8F8F8")))
                .transition(DrawableTransitionOptions.withCrossFade(400)) // Smooth fade
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Performance boost
                .centerCrop()
                .into(holder.ivThumbnail);

        // 3. Video Icon Visibility
        if (holder.videoIcon != null) {
            holder.videoIcon.setVisibility(currentItem.isVideo() ? View.VISIBLE : View.GONE);
        }

        // 4. 🔥 DOWNLOADED STATUS HANDLING
        // Fragment mein jo humne syncDownloadStatus lagaya tha, usko yahan UI mein dikhana
        if (currentItem.isDownloaded()) {
            holder.btnSetStatus.setVisibility(View.GONE);
            holder.downloadStatus.setVisibility(View.VISIBLE);
            holder.ivThumbnail.setAlpha(0.7f); // Halka dark taake pata chale saved hai
        } else {
            holder.btnSetStatus.setVisibility(View.VISIBLE);
            holder.downloadStatus.setVisibility(View.GONE);
            holder.ivThumbnail.setAlpha(1.0f);
        }

        // Reset Overlay (Recycler view recycling issue fix)
        holder.downloadOverlay.setVisibility(View.GONE);

        // 5. 🔥 SMOOTH CLICK ANIMATIONS
        holder.itemView.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                if (listener != null) listener.onPreview(currentItem);
            }).start();
        });

        holder.actionContainer.setOnClickListener(v -> {
            if (listener != null && !currentItem.isDownloaded()) {
                listener.onSetStatus(currentItem, holder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return trendList != null ? trendList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView ivThumbnail, videoIcon, btnSetStatus, downloadStatus;
        public TextView tvTrendTitle, progressText;
        public RelativeLayout downloadOverlay;
        public FrameLayout actionContainer;
        // 🔥 Iska naam aur type update kerdain
        public com.google.android.material.progressindicator.CircularProgressIndicator pbCircular;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvTrendTitle = itemView.findViewById(R.id.tvTrendTitle);
            videoIcon = itemView.findViewById(R.id.videoIcon);
            actionContainer = itemView.findViewById(R.id.actionContainer);
            btnSetStatus = itemView.findViewById(R.id.btnSetStatus);
            downloadStatus = itemView.findViewById(R.id.downloadStatus);
            downloadOverlay = itemView.findViewById(R.id.downloadOverlay);
            progressText = itemView.findViewById(R.id.progressText);

            // 🔥 Yahan "neonProgressBar" ID use kero
            pbCircular = itemView.findViewById(R.id.neonProgressBar);

            if (ivThumbnail != null) {
                ivThumbnail.setClipToOutline(true);
            }
        }
    }
}