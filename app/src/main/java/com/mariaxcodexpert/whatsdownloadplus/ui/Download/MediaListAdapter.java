package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.animation.*;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity;
import com.mariaxcodexpert.whatsdownloadplus.ui.base.BaseMediaViewHolder;
import com.mariaxcodexpert.whatsdownloadplus.ui.base.MediaViewUtils;
import java.util.*;

public class MediaListAdapter extends RecyclerView.Adapter<MediaListAdapter.VH> {

    private static final String TAG = "MediaListAdapter";

    private final OnAction action;
    private OnItemClickListener itemClickListener;
    public static final String EXTRA_MEDIA_LIST = "extra_media_list", EXTRA_POSITION = "extra_position";

    private final AsyncListDiffer<Object> differ = new AsyncListDiffer<>(this, new DiffUtil.ItemCallback<Object>() {
        @Override
        public boolean areItemsTheSame(@NonNull Object o, @NonNull Object n) {
            if (o instanceof ImageEntity && n instanceof ImageEntity) return Objects.equals(((ImageEntity) o).fileName, ((ImageEntity) n).fileName);
            if (o instanceof VideoEntity && n instanceof VideoEntity) return Objects.equals(((VideoEntity) o).fileName, ((VideoEntity) n).fileName);
            return false;
        }
        @Override
        public boolean areContentsTheSame(@NonNull Object o, @NonNull Object n) { return Objects.equals(o, n); }
    });

    public interface OnAction { void onDelete(Object item); }
    public interface OnItemClickListener { void onItemClick(Object item); }

    public MediaListAdapter(OnAction action) {
        this.action = action;
        setHasStableIds(true);
    }

    public void setOnItemClickListener(OnItemClickListener l) { this.itemClickListener = l; }

    public void submit(List<Object> list, View emptyView, RecyclerView rv) {
        differ.submitList(list, () -> {
            boolean empty = (list == null || list.isEmpty());
            if (emptyView != null) emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            if (rv != null) rv.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
    }
    @Override
    public int getItemCount() {
        return differ != null ? differ.getCurrentList().size() : 0;
    }

    @Override
    public long getItemId(int pos) {
        try {
            List<Object> list = differ.getCurrentList();
            if (pos < 0 || pos >= list.size()) return RecyclerView.NO_ID;

            Object item = list.get(pos);
            if (item instanceof ImageEntity) return ((ImageEntity) item).fileName.hashCode();
            if (item instanceof VideoEntity) return ((VideoEntity) item).fileName.hashCode();
        } catch (Exception e) {
            Log.e(TAG, "Error calculating getItemId at position: " + pos, e);
            return RecyclerView.NO_ID;
        }
        return RecyclerView.NO_ID;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        try {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_downloaded_files, p, false));
        } catch (Exception e) {
            Log.e(TAG, "Error inflating layout R.layout.item_downloaded_files", e);
            throw e;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        final List<Object> currentList = differ.getCurrentList();
        if (position < 0 || position >= currentList.size()) return;

        final Object item = currentList.get(position);
        if (item == null) return;
        if (holder.deleteOverlay != null) {
            holder.deleteOverlay.setVisibility(View.GONE);
            holder.deleteOverlay.setAlpha(0f);
            if (holder.deleteProgress != null) holder.deleteProgress.setProgress(0);
        }

        try {
            boolean isVid = item instanceof VideoEntity;
            String path = isVid ?
                    (((VideoEntity) item).isDownloaded ? ((VideoEntity) item).gallery_path : ((VideoEntity) item).getUri()) :
                    (((ImageEntity) item).isDownloaded ? ((ImageEntity) item).gallery_path : ((ImageEntity) item).getUri());

            if (path != null && !path.isEmpty()) {
                MediaViewUtils.loadImage(Glide.with(holder.imageThumb.getContext()), path, holder.imageThumb);
            } else {
                setSafePlaceholder(holder.imageThumb);
            }
            holder.videoIcon.setVisibility(isVid ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            Log.e(TAG, "Error binding view holder at position: " + position, e);
            setSafePlaceholder(holder.imageThumb);
        }

        holder.deleteIcon.setOnClickListener(v -> handleDeletion(holder, item));
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null && holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                itemClickListener.onItemClick(item);
            }
        });
    }

    private void setSafePlaceholder(ImageView imageView) {
        try {
            if (imageView != null) {
                imageView.setImageResource(R.drawable.placeholder_media);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting safe placeholder image", e);
            if (imageView != null) {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }
    }

    private void handleDeletion(VH holder, Object item) {
        if (holder.deleteOverlay == null) {
            if (action != null) action.onDelete(item);
            return;
        }

        holder.deleteOverlay.setVisibility(View.VISIBLE);
        holder.deleteOverlay.setAlpha(1.0f);

        ValueAnimator anim = ValueAnimator.ofInt(0, 100).setDuration(600);
        anim.addUpdateListener(a -> {
            try {
                int val = (int) a.getAnimatedValue();
                if (holder.deleteProgress != null) holder.deleteProgress.setProgress(val);
                if (holder.tvPercent != null) holder.tvPercent.setText(val + "%");
            } catch (Exception e) {
                Log.e(TAG, "Error during delete animation update", e);
            }
        });

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                if (action != null && holder.getAdapterPosition() != RecyclerView.NO_POSITION){
                    action.onDelete(item);
                }
            }
        });
        anim.start();
    }

    public List<Object> getCurrentList() { return differ.getCurrentList(); }

    public static class VH extends BaseMediaViewHolder {
        public View deleteOverlay, deleteIcon;
        public CircularProgressIndicator deleteProgress;
        public TextView tvPercent;
        public ImageView imageThumb, videoIcon;

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