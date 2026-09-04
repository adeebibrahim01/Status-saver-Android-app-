package com.mariaxcodexpert.whatsdownloadplus.ui.trending;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.mariaxcodexpert.whatsdownloadplus.R;

import java.util.ArrayList;
import java.util.List;

public class TrendingAdapter extends RecyclerView.Adapter<TrendingAdapter.ViewHolder> {

    private List<TrendMediaItem> trendList;
    private final OnTrendItemClickListener listener;
    private static final ColorDrawable PLACEHOLDER = new ColorDrawable(Color.parseColor("#1A1A1A"));

    public interface OnTrendItemClickListener {
        void onSetStatus(TrendMediaItem item, ViewHolder holder);
        void onPreview(TrendMediaItem item);
    }

    public TrendingAdapter(List<TrendMediaItem> trendList, OnTrendItemClickListener listener) {
        this.trendList = trendList != null ? trendList : new ArrayList<>();
        this.listener = listener;
    }

    public void updateList(ArrayList<TrendMediaItem> newList) {
        if (newList == null) return;

        try {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new TrendDiffCallback(this.trendList, newList));
            this.trendList = new ArrayList<>(newList);
            diffResult.dispatchUpdatesTo(this);
        } catch (Exception e) {
            this.trendList = new ArrayList<>(newList);
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trending, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= trendList.size()) return;

        final TrendMediaItem currentItem = trendList.get(position);
        if (currentItem == null) return;

        final Context context = holder.itemView.getContext();
        if (context != null) {
            try {
                com.bumptech.glide.RequestBuilder<android.graphics.drawable.Drawable> thumbnailRequest = Glide.with(context)
                        .load(currentItem.getThumbnailUrl())
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .centerCrop();

                Glide.with(context)
                        .load(currentItem.getThumbnailUrl())
                        .placeholder(PLACEHOLDER)
                        .error(PLACEHOLDER)
                        .thumbnail(thumbnailRequest)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .centerCrop()
                        .into(holder.ivThumbnail);

            } catch (Exception e) {
                holder.ivThumbnail.setImageDrawable(PLACEHOLDER);
            }
        }

        String location = currentItem.getCountry();
        holder.tvTrendTitle.setText((location != null && !location.isEmpty()) ? location : context.getString(R.string.trend_fallback_global));
        holder.videoIcon.setVisibility(currentItem.isVideo() ? View.VISIBLE : View.GONE);

        boolean isDownloaded = currentItem.isDownloaded();
        holder.btnSetStatus.setVisibility(isDownloaded ? View.GONE : View.VISIBLE);
        holder.downloadStatus.setVisibility(isDownloaded ? View.VISIBLE : View.GONE);
        holder.ivThumbnail.setAlpha(isDownloaded ? 0.6f : 1.0f);
        holder.downloadOverlay.setVisibility(View.GONE);
        holder.itemView.setOnClickListener(v -> {
            if (isNetworkAvailable(context)) {
                v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).withEndAction(() -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                    if (listener != null) listener.onPreview(currentItem);
                }).start();
            } else {
                showToast(context, context.getString(R.string.error_no_internet_toast));
            }
        });

        holder.actionContainer.setOnClickListener(v -> {
            if (isNetworkAvailable(context)) {
                if (listener != null && !currentItem.isDownloaded()) {
                    listener.onSetStatus(currentItem, holder);
                }
            } else {
                showToast(context, context.getString(R.string.error_no_internet_toast));
            }
        });
    }

    @Override
    public int getItemCount() {
        return trendList != null ? trendList.size() : 0;
    }

    private boolean isNetworkAvailable(Context context) {
        if (context == null) return false;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return true;
            NetworkCapabilities cap = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return cap != null && (cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } catch (Exception e) {
            return true;
        }
    }

    private void showToast(Context context, String msg) {
        if (context != null) {
            Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView ivThumbnail, videoIcon, btnSetStatus, downloadStatus;
        public final TextView tvTrendTitle, progressText;
        public final RelativeLayout downloadOverlay;
        public final FrameLayout actionContainer;
        public final com.google.android.material.progressindicator.CircularProgressIndicator pbCircular;

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
            pbCircular = itemView.findViewById(R.id.neonProgressBar);

            if (ivThumbnail != null) ivThumbnail.setClipToOutline(true);
        }
    }

    private static class TrendDiffCallback extends DiffUtil.Callback {
        private final List<TrendMediaItem> oldList, newList;
        public TrendDiffCallback(List<TrendMediaItem> oldList, List<TrendMediaItem> newList) {
            this.oldList = oldList; this.newList = newList;
        }
        @Override public int getOldListSize() { return oldList != null ? oldList.size() : 0; }
        @Override public int getNewListSize() { return newList != null ? newList.size() : 0; }

        @Override public boolean areItemsTheSame(int oldPos, int newPos) {
            try {
                return oldList.get(oldPos).getThumbnailUrl().equals(newList.get(newPos).getThumbnailUrl());
            } catch (Exception e) { return false; }
        }

        @Override public boolean areContentsTheSame(int oldPos, int newPos) {
            try {
                return oldList.get(oldPos).isDownloaded() == newList.get(newPos).isDownloaded();
            } catch (Exception e) { return false; }
        }
    }
}