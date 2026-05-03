package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity;

import java.util.Objects;

/**
 * 🔥 DASHBOARD ADAPTER: Optimized to show ONLY downloaded gallery media.
 * Logic is now simplified as Room handles the filtering.
 */
public class RecentDownloadsAdapter extends ListAdapter<Object, RecentDownloadsAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Object item);
    }

    private final OnItemClickListener listener;

    public RecentDownloadsAdapter(OnItemClickListener listener) {
        super(new DiffCallback());
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recent_download_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object item = getItem(position);
        if (item == null) return;

        String displayPath = null;
        boolean isVideo = false;

        // 🔥 Room already filtered these, so we just extract the path
        if (item instanceof ImageEntity) {
            ImageEntity image = (ImageEntity) item;
            displayPath = image.gallery_path;
            isVideo = false;
        } else if (item instanceof VideoEntity) {
            VideoEntity video = (VideoEntity) item;
            displayPath = video.gallery_path;
            isVideo = true;
        }

        // Safety check: Agar path null h to view khali rkhain (Room filter ki waja sa ye case aye ga nahi)
        if (displayPath == null || displayPath.isEmpty()) {
            holder.itemView.setVisibility(View.GONE);
            return;
        } else {
            holder.itemView.setVisibility(View.VISIBLE);
        }

        // 🟢 PREMIUM GLIDE LOADING
        Glide.with(holder.imgThumb.getContext())
                .load(displayPath) // Loads content:// uri or file path
                .placeholder(R.drawable.shimmer_placeholder)
                .error(R.drawable.placeholder_image)
                .thumbnail(0.25f)
                .transition(DrawableTransitionOptions.withCrossFade(250))
                .format(DecodeFormat.PREFER_RGB_565) // Saves memory
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.imgThumb);

        // Video Play Icon Overlay
        holder.videoIcon.setVisibility(isVideo ? View.VISIBLE : View.GONE);

        // Click Handling
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        // Clear memory when scrolling
        Glide.with(holder.imgThumb.getContext()).clear(holder.imgThumb);
    }

    public static class DiffCallback extends DiffUtil.ItemCallback<Object> {
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
            return Objects.equals(oldItem, newItem);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView imgThumb, videoIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumb = itemView.findViewById(R.id.imgThumb);
            videoIcon = itemView.findViewById(R.id.videoIcon);

            // Performance enhancement for hardware-accelerated devices
            imgThumb.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
    }
}