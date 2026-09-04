package com.mariaxcodexpert.whatsdownloadplus.ui.base;

import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.mariaxcodexpert.whatsdownloadplus.R;

public class MediaViewUtils {

    public static void loadImage(RequestManager glide, String path, ImageView target) {
        if (path == null || target == null || glide == null) return;

        com.bumptech.glide.RequestBuilder<android.graphics.drawable.Drawable> thumbnailRequest = glide
                .load(path)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop();

        glide.load(path)
                .placeholder(R.drawable.shimmer_placeholder)
                .error(R.drawable.shimmer_placeholder)
                .thumbnail(thumbnailRequest)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .dontAnimate()
                .into(target);
    }
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