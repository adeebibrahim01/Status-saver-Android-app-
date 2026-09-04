package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.view.*;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity;
import java.util.Objects;

public class RecentDownloadsAdapter extends ListAdapter<Object, RecentDownloadsAdapter.ViewHolder> {

    public interface OnItemClickListener { void onItemClick(Object item); }
    private final OnItemClickListener listener;

    public RecentDownloadsAdapter(OnItemClickListener listener) {
        super(new DiffCallback());
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= getItemCount()) {
            return RecyclerView.NO_ID;
        }

        Object item = getItem(position);
        if (item == null) return RecyclerView.NO_ID;

        if (item instanceof ImageEntity) {
            String name = ((ImageEntity) item).fileName;
            return name != null ? name.hashCode() : position;
        } else if (item instanceof VideoEntity) {
            String name = ((VideoEntity) item).fileName;
            return name != null ? name.hashCode() : position;
        }

        return position;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recent_download_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            Object item = getItem(position);
            if (item == null) return;

            boolean isVid = item instanceof VideoEntity;
            String path = isVid ? ((VideoEntity) item).gallery_path : ((ImageEntity) item).gallery_path;

            if (path == null || path.isEmpty()) {
                holder.itemView.setVisibility(View.GONE);
                holder.itemView.getLayoutParams().height = 0;
                holder.itemView.getLayoutParams().width = 0;
                return;
            } else {
                holder.itemView.setVisibility(View.VISIBLE);
                holder.itemView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                holder.itemView.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
            }

            Glide.with(holder.imgThumb.getContext())
                    .load(path)
                    .dontAnimate()
                    .placeholder(R.drawable.shimmer_placeholder)
                    .error(R.drawable.image_error_placeholder)
                    .thumbnail(
                            Glide.with(holder.imgThumb.getContext())
                                    .load(path)
                                    .sizeMultiplier(0.15f)
                    )
                    .format(DecodeFormat.PREFER_RGB_565)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .into(holder.imgThumb);
            holder.videoIcon.setVisibility(isVid ? View.VISIBLE : View.GONE);
            holder.itemView.setOnClickListener(v -> {
                int currentPos = holder.getBindingAdapterPosition();

                if (listener != null && currentPos != RecyclerView.NO_POSITION) {
                    listener.onItemClick(getItem(currentPos));
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        Glide.with(holder.imgThumb.getContext()).clear(holder.imgThumb);
    }

    public static class DiffCallback extends DiffUtil.ItemCallback<Object> {
        @Override
        public boolean areItemsTheSame(@NonNull Object o, @NonNull Object n) {
            if (o.getClass() != n.getClass()) return false;

            if (o instanceof ImageEntity) {
                return Objects.equals(((ImageEntity) o).fileName, ((ImageEntity) n).fileName);
            }
            if (o instanceof VideoEntity) {
                return Objects.equals(((VideoEntity) o).fileName, ((VideoEntity) n).fileName);
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Object o, @NonNull Object n) {
            return Objects.equals(o, n);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView imgThumb, videoIcon;
        public ViewHolder(@NonNull View v) {
            super(v);
            imgThumb = v.findViewById(R.id.imgThumb);
            videoIcon = v.findViewById(R.id.videoIcon);
            imgThumb.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
    }
}