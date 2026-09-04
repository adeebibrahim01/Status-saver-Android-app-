package com.mariaxcodexpert.whatsdownloadplus.ui.trending;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.mariaxcodexpert.whatsdownloadplus.databinding.ItemTrendingPagerBinding;
import java.util.ArrayList;
import java.util.List;

public class TrendingPagerAdapter extends RecyclerView.Adapter<TrendingPagerAdapter.ViewHolder> {

    private final List<TrendMediaItem> mediaList;
    private OnItemClickListener listener;
    public interface OnItemClickListener {
        void onItemClick();
    }
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    public TrendingPagerAdapter(List<TrendMediaItem> mediaList) {
        this.mediaList = mediaList != null ? mediaList : new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTrendingPagerBinding binding = ItemTrendingPagerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= mediaList.size()) return;

        TrendMediaItem item = mediaList.get(position);
        if (item == null) return;

        final Context context = holder.itemView.getContext();
        holder.binding.pagerLoader.setVisibility(View.VISIBLE);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick();
        });

        if (item.isVideo()) {
            holder.binding.pagerImageView.setVisibility(View.GONE);
            holder.binding.pagerPlayerView.setVisibility(View.VISIBLE);
            holder.binding.pagerLoader.setVisibility(View.GONE); // Video loader player control handle karega
        } else {
            holder.binding.pagerPlayerView.setVisibility(View.GONE);
            holder.binding.pagerImageView.setVisibility(View.VISIBLE);
            if (context != null) {
                Glide.with(context)
                        .load(item.getMediaUrl())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .thumbnail(0.1f)
                        .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e,
                                                        Object model,
                                                        com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                        boolean isFirstResource) {
                                holder.binding.pagerLoader.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                                           Object model,
                                                           com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                           com.bumptech.glide.load.DataSource dataSource,
                                                           boolean isFirstResource) {
                                holder.binding.pagerLoader.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(holder.binding.pagerImageView);
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.binding != null && holder.binding.pagerImageView != null) {
            Context context = holder.itemView.getContext();
            if (context != null) {
                Glide.with(context.getApplicationContext()).clear(holder.binding.pagerImageView);
            }
        }
    }
    @Override
    public int getItemCount() {
        return mediaList != null ? mediaList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ItemTrendingPagerBinding binding;
        public ViewHolder(ItemTrendingPagerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}