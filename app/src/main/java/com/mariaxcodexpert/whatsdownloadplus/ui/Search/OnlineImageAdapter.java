package com.mariaxcodexpert.whatsdownloadplus.ui.Search;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.mariaxcodexpert.whatsdownloadplus.R;

import java.util.List;

public class OnlineImageAdapter extends RecyclerView.Adapter<OnlineImageAdapter.ViewHolder> {

    private final Context context;
    private final List<MediaItem> mediaList;
    private OnItemClickListener listener;
    private com.mariaxcodexpert.whatsdownloadplus.FeedbackPromptManager feedbackManager;
    public interface OnItemClickListener {
        void onDownloadClick(MediaItem item);
        void onPreviewClick(MediaItem item); // 🔥 Naya method preview k liye
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
        MediaItem item = mediaList.get(position);
// 🔥 High-End "BlurHash" Style Loading
        if (context != null && holder.imageThumb != null) {

            // 1. Instant Thumbnail Request (Bohot choti image jo blur feel degi)
            RequestBuilder<Drawable> thumbRequest = Glide.with(context)
                    .load(item.getUrl())
                    .override(20, 30) // Size mazeed kam kiya taake instant load ho aur blur lage
                    .centerCrop();

            // 2. Main Premium Loading
            Glide.with(context)
                    .load(item.getUrl())
                    .override(400, 600) // Portrait grid size
                    .thumbnail(thumbRequest)

                    // 🔥 Duration 400ms krain, 200ms bohot jaldi khatam ho jata h
                    .transition(DrawableTransitionOptions.withCrossFade(400))

                    .centerCrop()

                    // Placeholder ko halka grey ya off-white krain (Luxury feel)
                    .placeholder(new ColorDrawable(Color.parseColor("#F8F8F8")))

                    .error(android.R.drawable.stat_notify_error)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .into(holder.imageThumb);
        }

        // 2. 🔥 Video Icon Visibility
        if (holder.videoIcon != null) {
            holder.videoIcon.setVisibility(item.isVideo() ? View.VISIBLE : View.GONE);
        }

        // 3. 🔥 Downloaded Status UI
        if (item.isDownloaded()) {
            if (holder.downloadedStatus != null) holder.downloadedStatus.setVisibility(View.VISIBLE);
            if (holder.downloadIcon != null) holder.downloadIcon.setVisibility(View.GONE);
            holder.imageThumb.setAlpha(0.6f);
        } else {
            if (holder.downloadedStatus != null) holder.downloadedStatus.setVisibility(View.GONE);
            if (holder.downloadIcon != null) holder.downloadIcon.setVisibility(View.VISIBLE);
            holder.imageThumb.setAlpha(1.0f);
        }

        // 4. 🔥 Download Button Click (Updated for 100M Growth)
        holder.downloadIcon.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDownloadClick(item);

                // Feedback Logic
                if (feedbackManager == null && context instanceof android.app.Activity) {
                    feedbackManager = new com.mariaxcodexpert.whatsdownloadplus.FeedbackPromptManager((android.app.Activity) context);
                }
                if (feedbackManager != null) {
                    feedbackManager.incrementSuccessAndCheck();
                }
            }
        });

        // 5. 🔥 Card Preview Click (Poore item pr click)
        holder.itemView.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                if (listener != null) {
                    listener.onPreviewClick(item);
                }
            }).start();
        });
    }

    @Override
    public int getItemCount() {
        return (mediaList != null) ? mediaList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageThumb, videoIcon, downloadIcon, downloadedStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageThumb = itemView.findViewById(R.id.imageThumb);
            videoIcon = itemView.findViewById(R.id.videoIcon);
            downloadIcon = itemView.findViewById(R.id.downloadIcon);
            downloadedStatus = itemView.findViewById(R.id.downloadStatus);

            if (imageThumb != null) {
                imageThumb.setClipToOutline(true);
            }
        }
    }
}