package com.mariaxcodexpert.whatsdownloadplus.ui.Search;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.Helper.FeedbackPromptManager;

import java.util.List;

public class OnlineImageAdapter extends RecyclerView.Adapter<OnlineImageAdapter.ViewHolder> {

    private final Context context;
    private final List<MediaItem> mediaList;
    private OnItemClickListener listener;
    private FeedbackPromptManager feedbackManager;

    public interface OnItemClickListener {
        void onDownloadClick(MediaItem item);
        void onPreviewClick(MediaItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public OnlineImageAdapter(Context context, List<MediaItem> mediaList) {
        this.context = context;
        this.mediaList = mediaList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_online_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (mediaList == null || position >= mediaList.size()) return;

        MediaItem item = mediaList.get(position);
        if (item == null) return;

        setupTextAndIcons(holder, item);
        setupImageLoading(holder, item);
        setupStatusUI(holder, item);
        setupClickListeners(holder, item);
    }


    private void setupTextAndIcons(ViewHolder holder, MediaItem item) {
        if (holder.tvTrendTitle != null) {
            holder.tvTrendTitle.setText(item.getTitle() != null && !item.getTitle().isEmpty()
                    ? item.getTitle()
                    : context.getString(R.string.default_wallpaper_title));
        }
        if (holder.videoIcon != null) {
            holder.videoIcon.setVisibility(item.isVideo() ? View.VISIBLE : View.GONE);
        }
    }

    private void setupImageLoading(ViewHolder holder, MediaItem item) {
        if (context == null || holder.imageThumb == null || item.getUrl() == null) return;

        try {
            RequestBuilder<Drawable> thumbRequest = Glide.with(context)
                    .load(item.getUrl())
                    .override(20, 30)
                    .centerCrop();

            Glide.with(context)
                    .load(item.getUrl())
                    .override(400, 600)
                    .thumbnail(thumbRequest)
                    .transition(DrawableTransitionOptions.withCrossFade(400))
                    .centerCrop()
                    .placeholder(new ColorDrawable(Color.parseColor("#1A1A1A"))) // Darker placeholder for Premium look
                    .error(android.R.drawable.stat_notify_error)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .into(holder.imageThumb);
        } catch (Exception e) {
            Log.e("GLIDE_ERROR", "Image load failed: " + e.getMessage());
        }
    }

    private void setupStatusUI(ViewHolder holder, MediaItem item) {
        boolean isDownloaded = item.isDownloaded();

        if (holder.downloadedStatus != null)
            holder.downloadedStatus.setVisibility(isDownloaded ? View.VISIBLE : View.GONE);

        if (holder.downloadIcon != null)
            holder.downloadIcon.setVisibility(isDownloaded ? View.GONE : View.VISIBLE);

        if (holder.imageThumb != null) {
            holder.imageThumb.setAlpha(isDownloaded ? 0.6f : 1.0f);
        }
    }

    private void setupClickListeners(ViewHolder holder, MediaItem item) {
        // Download Click
        if (holder.downloadIcon != null) {
            holder.downloadIcon.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDownloadClick(item);
                    triggerFeedback();
                }
            });
        }

        holder.itemView.setOnClickListener(v -> {
            if (v == null) return;
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                if (listener != null) listener.onPreviewClick(item);
            }).start();
        });
    }

    private void triggerFeedback() {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                if (feedbackManager == null) {
                    feedbackManager = new FeedbackPromptManager(activity);
                }
                feedbackManager.incrementSuccessAndCheck();
            }
        }
    }

    @Override
    public int getItemCount() {
        return (mediaList != null) ? mediaList.size() : 0;
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        if (context != null && holder.imageThumb != null) {
            Glide.with(context.getApplicationContext()).clear(holder.imageThumb);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageThumb, videoIcon, downloadIcon, downloadedStatus;
        TextView tvTrendTitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageThumb = itemView.findViewById(R.id.imageThumb);
            videoIcon = itemView.findViewById(R.id.videoIcon);
            downloadIcon = itemView.findViewById(R.id.downloadIcon);
            downloadedStatus = itemView.findViewById(R.id.downloadStatus);
            tvTrendTitle = itemView.findViewById(R.id.tvTrendTitle);

            if (imageThumb != null) {
                imageThumb.setClipToOutline(true);
            }
        }
    }
}