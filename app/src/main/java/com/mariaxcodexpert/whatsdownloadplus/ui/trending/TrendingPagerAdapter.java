package com.mariaxcodexpert.whatsdownloadplus.ui.trending;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.databinding.ItemTrendingPagerBinding;
import java.util.ArrayList;

public class TrendingPagerAdapter extends RecyclerView.Adapter<TrendingPagerAdapter.ViewHolder> {

    private final ArrayList<TrendMediaItem> mediaList;

    public TrendingPagerAdapter(ArrayList<TrendMediaItem> mediaList) {
        this.mediaList = mediaList;
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
        TrendMediaItem item = mediaList.get(position);

        holder.binding.pagerLoader.setVisibility(View.VISIBLE);

        if (item.isVideo()) {
            holder.binding.pagerImageView.setVisibility(View.GONE);
            holder.binding.pagerPlayerView.setVisibility(View.VISIBLE);
            // Video playback logic Activity mein handle hogi
        }



        else {
            holder.binding.pagerPlayerView.setVisibility(View.GONE);
            holder.binding.pagerImageView.setVisibility(View.VISIBLE);

            Glide.with(holder.itemView.getContext())
                    .load(item.getMediaUrl())
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

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemTrendingPagerBinding binding;
        public ViewHolder(ItemTrendingPagerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}