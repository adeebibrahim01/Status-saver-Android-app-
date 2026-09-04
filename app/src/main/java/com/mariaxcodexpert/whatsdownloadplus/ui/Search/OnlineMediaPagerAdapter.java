package com.mariaxcodexpert.whatsdownloadplus.ui.Search;

import android.app.Activity;
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
import java.util.List;

public class OnlineMediaPagerAdapter extends RecyclerView.Adapter<OnlineMediaPagerAdapter.ViewHolder> {

    private final List<MediaItem> items;
    private final Context context;


    private OnItemClickListener clickListener;

    public interface OnItemClickListener {
        void onItemClick();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public OnlineMediaPagerAdapter(Context context, List<MediaItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_online_premium_viewer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        if (items == null || position >= items.size()) return;

        MediaItem item = items.get(position);
        if (item == null) return;

        holder.loader.setVisibility(View.VISIBLE);
        holder.photoView.setVisibility(View.GONE);
        holder.videoView.setVisibility(View.GONE);
        holder.videoView.stopPlayback();

        View.OnClickListener toggleClick = v -> {
            if (clickListener != null) clickListener.onItemClick();
        };

        holder.photoView.setOnClickListener(toggleClick);
        holder.itemView.setOnClickListener(toggleClick);

        if (item.isVideo()) {
            setupSimpleVideo(holder, item);
        } else {
            setupSimpleImage(holder, item);
        }
    }

    private void setupSimpleImage(ViewHolder holder, MediaItem item) {
        if (context == null || item.getUrl() == null) return;

        holder.photoView.setVisibility(View.VISIBLE);

        Glide.with(context)
                .load(item.getUrl())
                .placeholder(android.R.color.transparent)
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
        String vUrl = item.isVideo() ? item.getVideoUrl() : item.getUrl();
        if (vUrl == null || vUrl.isEmpty()) {
            holder.loader.setVisibility(View.GONE);
            return;
        }

        holder.videoView.setVisibility(View.VISIBLE);

        try {
            holder.videoView.setVideoURI(Uri.parse(vUrl));

            holder.videoView.setOnPreparedListener(mp -> {
                if (holder.loader != null) {
                    holder.loader.setVisibility(View.GONE);
                }
                mp.setLooping(true);
                mp.setVideoScalingMode(android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                holder.videoView.start();
            });

            holder.videoView.setOnErrorListener((mp, what, extra) -> {
                if (holder.loader != null) holder.loader.setVisibility(View.GONE);
                android.util.Log.e("VIDEO_ERROR", "Error playing video: " + what);
                return true;
            });

        } catch (Exception e) {
            holder.loader.setVisibility(View.GONE);
            e.printStackTrace();
        }
    }
    @Override
    public int getItemCount() {
        return items.size();
    }
    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);

        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (!activity.isDestroyed() && !activity.isFinishing()) {
                Glide.with(context).clear(holder.photoView);
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        PhotoView photoView;
        VideoView videoView;
        ProgressBar loader;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.premiumPhotoView);
            videoView = itemView.findViewById(R.id.premiumVideoView);
            loader = itemView.findViewById(R.id.premiumItemLoader);
        }
    }
}