package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.content.Context;
import android.net.Uri;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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
        View view = LayoutInflater.from(context).inflate(R.layout.item_full_screen_media, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object item = mediaList.get(position);
        String finalPath = null;
        boolean isVideo = false;

        if (item instanceof ImageEntity) {
            ImageEntity img = (ImageEntity) item;
            finalPath = (img.isDownloaded && img.gallery_path != null && !img.gallery_path.isEmpty())
                    ? img.gallery_path : img.getUri();
            isVideo = false;
        } else if (item instanceof VideoEntity) {
            VideoEntity vid = (VideoEntity) item;
            finalPath = (vid.isDownloaded && vid.gallery_path != null && !vid.gallery_path.isEmpty())
                    ? vid.gallery_path : vid.getUri();
            isVideo = true;
        }

        if (finalPath == null) return;
        Uri uri = Uri.parse(finalPath);

        holder.playerView.setPlayer(null);
        holder.playerView.setVisibility(View.GONE);
        holder.fullImage.setVisibility(View.VISIBLE);

        if (isVideo) {
            holder.fullImage.setVisibility(View.GONE);
            holder.playerView.setVisibility(View.VISIBLE);
            setupVideo(holder, uri, position);
        } else {
            Glide.with(context)
                    .load(uri)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .fitCenter()
                    .into(holder.fullImage);

            holder.fullImage.setOnPhotoTapListener((view, x, y) -> {
                if (clickListener != null) clickListener.onMediaClick();
            });
        }
    }

    private void setupVideo(ViewHolder holder, Uri uri, int position) {
        releasePlayerAtPosition(position);

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(1000, 5000, 1000, 1000).build();

        ExoPlayer player = new ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .build();

        player.setMediaItem(MediaItem.fromUri(uri));
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.prepare();
        player.setPlayWhenReady(false);

        holder.playerView.setPlayer(player);
        playerMap.put(position, player);

        setupTouchListener(holder.playerView);
    }

    private void setupTouchListener(View view) {
        final GestureDetector detector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                if (clickListener != null) clickListener.onMediaClick();
                return true;
            }
        });

        view.setOnTouchListener((v, event) -> {
            detector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return true;
        });
    }

    public void handlePlayback(int position) {
        for (int i = 0; i < playerMap.size(); i++) {
            int key = playerMap.keyAt(i);
            ExoPlayer p = playerMap.get(key);
            if (p != null) {
                if (key == position) {
                    p.play();
                } else {
                    p.pause();
                }
            }
        }
    }

    // 🔥 Added: Pause all active videos (Ad dikhane se pehle sound band karne ke liye)
    public void pauseAll() {
        for (int i = 0; i < playerMap.size(); i++) {
            ExoPlayer p = playerMap.valueAt(i);
            if (p != null && p.isPlaying()) {
                p.pause();
            }
        }
    }

    private void releasePlayerAtPosition(int position) {
        ExoPlayer p = playerMap.get(position);
        if (p != null) {
            p.stop();
            p.release();
            playerMap.remove(position);
        }
    }

    // 🔥 Final Release: Stop and clear everything
    public void releaseAll() {
        for (int i = 0; i < playerMap.size(); i++) {
            ExoPlayer p = playerMap.valueAt(i);
            if (p != null) {
                p.stop();
                p.release();
            }
        }
        playerMap.clear();
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        int pos = holder.getBindingAdapterPosition();
        if (pos != RecyclerView.NO_POSITION) {
            releasePlayerAtPosition(pos);
        }
        holder.playerView.setPlayer(null);
    }

    @Override
    public int getItemCount() {
        return mediaList != null ? mediaList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        PhotoView fullImage;
        PlayerView playerView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fullImage = itemView.findViewById(R.id.fullImage);
            playerView = itemView.findViewById(R.id.fullPlayerView);
        }
    }
}