package com.mariaxcodexpert.whatsdownloadplus.ui.Search;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.ui.Search.MediaItem;
import java.util.List;

/**
 * Developed by MariaXCodeExpert
 * Optimized for item_online_premium_viewer layout
 */
public class OnlineMediaPagerAdapter extends RecyclerView.Adapter<OnlineMediaPagerAdapter.ViewHolder> {

    private final List<MediaItem> items;
    private final Context context;

    public OnlineMediaPagerAdapter(Context context, List<MediaItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 🔥 FIXED: Naya layout name use kiya hai
        View view = LayoutInflater.from(context).inflate(R.layout.item_online_premium_viewer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MediaItem item = items.get(position);

        // UI Reset
        holder.loader.setVisibility(View.VISIBLE);
        holder.photoView.setVisibility(View.GONE);
        holder.videoView.setVisibility(View.GONE);

        if (item.isVideo()) {
            setupSimpleVideo(holder, item);
        } else {
            setupSimpleImage(holder, item);
        }
    }

    private void setupSimpleImage(ViewHolder holder, MediaItem item) {
        holder.photoView.setVisibility(View.VISIBLE);
        Glide.with(context)
                .load(item.getUrl())
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        holder.loader.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        holder.loader.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(holder.photoView);
    }

    private void setupSimpleVideo(ViewHolder holder, MediaItem item) {
        holder.videoView.setVisibility(View.VISIBLE);
        String vUrl = item.getVideoUrl() != null ? item.getVideoUrl() : item.getUrl();

        holder.videoView.setVideoURI(Uri.parse(vUrl));

        holder.videoView.setOnPreparedListener(mp -> {
            holder.loader.setVisibility(View.GONE);
            mp.setLooping(true); // Loop for status feel
            holder.videoView.start(); // Auto play when ready
        });

        holder.videoView.setOnErrorListener((mp, what, extra) -> {
            holder.loader.setVisibility(View.GONE);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        PhotoView photoView;
        VideoView videoView;
        ProgressBar loader;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // 🔥 FIXED: Updated IDs based on the new premium layout
            photoView = itemView.findViewById(R.id.premiumPhotoView);
            videoView = itemView.findViewById(R.id.premiumVideoView);
            loader = itemView.findViewById(R.id.premiumItemLoader);
        }
    }
}