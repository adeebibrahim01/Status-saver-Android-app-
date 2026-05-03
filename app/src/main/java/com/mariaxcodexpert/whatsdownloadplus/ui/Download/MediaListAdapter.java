package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity;
import com.mariaxcodexpert.whatsdownloadplus.ui.base.BaseMediaViewHolder;
import com.mariaxcodexpert.whatsdownloadplus.ui.base.MediaViewUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MediaListAdapter extends RecyclerView.Adapter<MediaListAdapter.VH> {

    private final OnAction action;
    private OnItemClickListener itemClickListener;

    public static final String EXTRA_MEDIA_LIST = "extra_media_list";
    public static final String EXTRA_POSITION = "extra_position";

    // 🔥 Single Source of Truth ke liye optimized Differ
    private final AsyncListDiffer<Object> differ = new AsyncListDiffer<>(this, new DiffUtil.ItemCallback<Object>() {
        @Override
        public boolean areItemsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
            if (oldItem instanceof ImageEntity && newItem instanceof ImageEntity) {
                return Objects.equals(((ImageEntity) oldItem).fileName, ((ImageEntity) newItem).fileName);
            }
            if (oldItem instanceof VideoEntity && newItem instanceof VideoEntity) {
                return Objects.equals(((VideoEntity) oldItem).fileName, ((VideoEntity) newItem).fileName);
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
            // Full content comparison to detect gallery_path changes
            return Objects.equals(oldItem, newItem);
        }
    });

    public List<Object> getCurrentList() {
        return differ.getCurrentList();
    }

    public interface OnAction { void onDelete(Object item); }
    public interface OnItemClickListener { void onItemClick(Object item); }

    public MediaListAdapter(OnAction action) {
        this.action = action;
        setHasStableIds(true); // IDs stable rakhein taake list jump na kare
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void submit(List<Object> list, View emptyView, RecyclerView recyclerView) {
        differ.submitList(list, () -> {
            boolean isEmpty = (list == null || list.isEmpty());
            if (emptyView != null) emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            if (recyclerView != null) recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public long getItemId(int position) {
        Object item = differ.getCurrentList().get(position);
        if (item instanceof ImageEntity) return (long) ((ImageEntity) item).fileName.hashCode();
        else if (item instanceof VideoEntity) return (long) ((VideoEntity) item).fileName.hashCode();
        return position;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_downloaded_files, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        final Object item = differ.getCurrentList().get(position);
        if (item == null) return;

        String displayPath;
        boolean isVideoType;

        // 🔥 Logic: Agar isDownloaded true hai to hamesha gallery_path use karein
        if (item instanceof ImageEntity) {
            ImageEntity img = (ImageEntity) item;
            displayPath = (img.isDownloaded && img.gallery_path != null && !img.gallery_path.isEmpty())
                    ? img.gallery_path : img.getUri();
            isVideoType = false;
        } else if (item instanceof VideoEntity) {
            VideoEntity vid = (VideoEntity) item;
            displayPath = (vid.isDownloaded && vid.gallery_path != null && !vid.gallery_path.isEmpty())
                    ? vid.gallery_path : vid.getUri();
            isVideoType = true;
        } else return;

        MediaViewUtils.loadImage(Glide.with(holder.imageThumb.getContext()), displayPath, holder.imageThumb);
        holder.videoIcon.setVisibility(isVideoType ? View.VISIBLE : View.GONE);

        // Reset Overlay (Recycling safety)
        if (holder.deleteOverlay != null) {
            holder.deleteOverlay.setVisibility(View.GONE);
            holder.deleteOverlay.setAlpha(0f);
        }

        // Delete Animation Logic
        if (holder.deleteIcon != null) {
            holder.deleteIcon.setOnClickListener(v -> {
                if (holder.deleteOverlay != null) {
                    holder.deleteOverlay.setVisibility(View.VISIBLE);
                    holder.deleteOverlay.animate().alpha(1.0f).setDuration(200).start();

                    ValueAnimator progressAnim = ValueAnimator.ofInt(0, 100);
                    progressAnim.setDuration(800);
                    progressAnim.addUpdateListener(animation -> {
                        int val = (int) animation.getAnimatedValue();
                        if (holder.deleteProgress != null) holder.deleteProgress.setProgress(val);
                        if (holder.tvPercent != null) holder.tvPercent.setText(val + "%");
                    });

                    progressAnim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (action != null) action.onDelete(item);
                        }
                    });
                    progressAnim.start();
                } else {
                    if (action != null) action.onDelete(item);
                }
            });
        }

        // Click Listener
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() { return differ.getCurrentList().size(); }

    public static class VH extends BaseMediaViewHolder {
        public View deleteOverlay;
        public CircularProgressIndicator deleteProgress;
        public TextView tvPercent;
        public ImageView imageThumb, videoIcon;
        public View deleteIcon;

        public VH(@NonNull View v) {
            super(v);
            imageThumb = v.findViewById(R.id.imageThumb);
            videoIcon = v.findViewById(R.id.videoIcon);
            deleteIcon = v.findViewById(R.id.deleteIcon);
            deleteOverlay = v.findViewById(R.id.deleteOverlay);
            deleteProgress = v.findViewById(R.id.deleteProgress);
            tvPercent = v.findViewById(R.id.tvPercent);
        }
    }
}