package com.mariaxcodexpert.whatsdownloadplus.ui.base;

import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.local.MediaEntity;

public class MediaViewUtils {

    /**
     * Common Image Loading for both Adapters
     */
    public static void loadImage(RequestManager glide, String path, ImageView target) {
        glide.load(path)
                .placeholder(R.drawable.shimmer_placeholder)
                .error(R.drawable.shimmer_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(target);
    }

    /**
     * Common Download/Saved Status UI
     */
    public static void updateStatusUI(BaseMediaViewHolder holder, boolean isDownloaded) {
        if (holder.downloadProgress != null) holder.downloadProgress.setVisibility(View.GONE);

        if (isDownloaded) {
            if (holder.downloadIcon != null) holder.downloadIcon.setVisibility(View.GONE);
            if (holder.downloadStatus != null) {
                holder.downloadStatus.setVisibility(View.VISIBLE);
                holder.downloadStatus.setImageResource(R.drawable.ic_double_tick);
            }
        } else {
            if (holder.downloadIcon != null) {
                holder.downloadIcon.setVisibility(View.VISIBLE);
                holder.downloadIcon.setAlpha(1.0f);
            }
            if (holder.downloadStatus != null) holder.downloadStatus.setVisibility(View.GONE);
        }
    }
}