package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.SparseArray;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.media3.common.*;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.*;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity;
import java.util.List;

@OptIn(markerClass = UnstableApi.class)
public class MediaPagerAdapter extends RecyclerView.Adapter<MediaPagerAdapter.ViewHolder> {

    private final List<Object> mediaList;
    private final Context context;
    private final OnItemClickListener clickListener;
    private final SparseArray<ExoPlayer> playerMap = new SparseArray<>();

    public interface OnItemClickListener { void onMediaClick(); }

    public MediaPagerAdapter(Context context, List<Object> mediaList, OnItemClickListener listener) {
        this.context = context;
        this.mediaList = mediaList;
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_full_screen_media, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object item = mediaList.get(position);
        String path = (item instanceof ImageEntity) ?
                (((ImageEntity) item).isDownloaded ? ((ImageEntity) item).gallery_path : ((ImageEntity) item).getUri()) :
                (((VideoEntity) item).isDownloaded ? ((VideoEntity) item).gallery_path : ((VideoEntity) item).getUri());

        if (path == null) return;
        Uri uri = Uri.parse(path);
        boolean isVid = item instanceof VideoEntity;

        holder.playerView.setVisibility(isVid ? View.VISIBLE : View.GONE);
        holder.fullImage.setVisibility(isVid ? View.GONE : View.VISIBLE);
        holder.loadingLayout.setVisibility(View.VISIBLE);
        holder.tvProgress.setVisibility(View.GONE);

        if (isVid) {
            setupVideo(holder, uri, position);
        } else {
            releasePlayerAtPosition(position);
            holder.playerView.setPlayer(null);
            holder.playerView.setVisibility(View.GONE);
            holder.fullImage.setVisibility(View.VISIBLE);

            Glide.with(context)
                    .load(uri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .dontAnimate()
                    .fitCenter()
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            holder.loadingLayout.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            holder.loadingLayout.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(holder.fullImage);

            holder.fullImage.setOnPhotoTapListener((view, x, y) -> {
                if (clickListener != null) clickListener.onMediaClick();
            });
        }
    }
    public void removeItem(int position) {
        releasePlayerAtPosition(position);
        releaseAll();
    }
    private void setupVideo(ViewHolder holder, Uri uri, int pos) {
        releasePlayerAtPosition(pos);
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(1000, 5000, 1000, 1000).build();
        ExoPlayer player = new ExoPlayer.Builder(context).setLoadControl(loadControl).build();
        player.setMediaItem(MediaItem.fromUri(uri));
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) {
                    holder.loadingLayout.setVisibility(View.VISIBLE);
                    holder.tvProgress.setVisibility(View.VISIBLE);
                    holder.tvProgress.setText(player.getBufferedPercentage() + "%");
                } else if (state == Player.STATE_READY) {
                    holder.loadingLayout.setVisibility(View.GONE);
                }
            }
        });

        player.prepare();
        holder.playerView.setPlayer(player);
        playerMap.put(pos, player);
        setupTouch(holder.playerView);
    }

    private void setupTouch(View v) {
        GestureDetector gd = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                if (clickListener != null) clickListener.onMediaClick();
                return true;
            }
        });
        v.setOnTouchListener((view, event) -> {
            gd.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP) view.performClick();
            return true;
        });
    }

    public void handlePlayback(int pos) {
        for (int i = 0; i < playerMap.size(); i++) {
            int key = playerMap.keyAt(i);
            ExoPlayer p = playerMap.get(key);
            if (p != null) {
                if (key == pos) {
                    p.setPlayWhenReady(true);
                    p.setVolume(1f);
                    p.play();
                } else {
                    p.setPlayWhenReady(false);
                    p.setVolume(0f);
                    p.pause();
                }
            }
        }
    }

    public void pauseAll() {
        for (int i = 0; i < playerMap.size(); i++) {
            ExoPlayer p = playerMap.valueAt(i);
            if (p != null) p.pause();
        }
    }

    private void releasePlayerAtPosition(int pos) {
        ExoPlayer p = playerMap.get(pos);
        if (p != null) { p.release(); playerMap.remove(pos); }
    }

    public void releaseAll() {
        for (int i = 0; i < playerMap.size(); i++) {
            ExoPlayer p = playerMap.valueAt(i);
            if (p != null) p.release();
        }
        playerMap.clear();
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        int pos = holder.getBindingAdapterPosition();
        if (pos != RecyclerView.NO_POSITION) releasePlayerAtPosition(pos);
        holder.playerView.setPlayer(null);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() { return mediaList != null ? mediaList.size() : 0; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        PhotoView fullImage;
        PlayerView playerView;
        LinearLayout loadingLayout;
        TextView tvProgress;

        public ViewHolder(@NonNull View v) {
            super(v);
            fullImage = v.findViewById(R.id.fullImage);
            playerView = v.findViewById(R.id.fullPlayerView);
            loadingLayout = v.findViewById(R.id.loadingLayout);
            tvProgress = v.findViewById(R.id.tvProgress);
        }
    }
}