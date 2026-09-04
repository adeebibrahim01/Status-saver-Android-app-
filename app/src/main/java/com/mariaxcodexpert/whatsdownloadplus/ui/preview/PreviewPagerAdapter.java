package com.mariaxcodexpert.whatsdownloadplus.ui.preview;

import android.content.Context;
import android.net.Uri;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity;
import com.mariaxcodexpert.whatsdownloadplus.ui.utils.media.player.ExoPlayerManager;

import java.util.List;

@OptIn(markerClass = UnstableApi.class)
public class PreviewPagerAdapter extends RecyclerView.Adapter<PreviewPagerAdapter.ViewHolder> {

    private final List<Object> mediaList;
    private final Context context;
    private final SparseArray<ExoPlayer> playerMap = new SparseArray<>();

    public PreviewPagerAdapter(Context context, List<Object> mediaList) {
        this.context = context;
        this.mediaList = mediaList;
    }

    public List<Object> getData() { return mediaList; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_preview_media, p, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int p) {
        if (mediaList == null || p >= mediaList.size()) return;

        Object item = mediaList.get(p);
        if (item == null) return;

        String path = (item instanceof ImageEntity) ? ((ImageEntity) item).getUri() : ((VideoEntity) item).getUri();
        if (path == null) return;

        h.resetUI();
        try {
            Glide.with(context)
                    .load(path)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerInside()
                    .into(h.imgPreview);
        } catch (Exception e) {
            android.util.Log.e("PreviewAdapter", "Glide load error: " + e.getMessage());
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder h) {
        super.onViewAttachedToWindow(h);
        int pos = h.getBindingAdapterPosition();
        if (pos == RecyclerView.NO_POSITION || mediaList == null || pos >= mediaList.size()) return;
        if (!(mediaList.get(pos) instanceof VideoEntity)) return;

        VideoEntity videoItem = (VideoEntity) mediaList.get(pos);
        if (videoItem.getUri() == null) return;

        try {
            ExoPlayer player = ExoPlayerManager.createPlayer(context);
            if (player == null) return;

            player.setRepeatMode(Player.REPEAT_MODE_ONE);
            player.setMediaItem(MediaItem.fromUri(Uri.parse(videoItem.getUri())));
            player.prepare();
            player.setPlayWhenReady(false);
            ExoPlayerManager.setupPlayerWithListeners(player, pos, h, () -> {
                if (h.itemView.isAttachedToWindow()) {
                    h.showPlayerUI();
                }
            });

            h.playerView.setPlayer(player);
            playerMap.put(pos, player);

        } catch (Exception e) {
            android.util.Log.e("PreviewAdapter", "Player Init Error: " + e.getMessage());
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder h) {
        super.onViewDetachedFromWindow(h);
        releasePlayerAt(h.getBindingAdapterPosition(), h);
    }
    private void releasePlayerAt(int pos, ViewHolder h) {
        try {
            ExoPlayer p = playerMap.get(pos);
            if (p != null) {
                ExoPlayerManager.releasePlayer(p);
                playerMap.remove(pos);
            }
            if (h != null && h.playerView != null) {
                h.playerView.setPlayer(null);
            }
        } catch (Exception e) {
            android.util.Log.e("PreviewAdapter", "Release Error: " + e.getMessage());
        }
    }
    public void handlePlayback(int currentPos) {
        if (playerMap == null) return;

        for (int i = 0; i < playerMap.size(); i++) {
            int key = playerMap.keyAt(i);
            ExoPlayer p = playerMap.get(key);
            if (p != null) {
                try {
                    if (key == currentPos) p.play();
                    else p.pause();
                } catch (Exception e) {
                    android.util.Log.e("PreviewAdapter", "Playback Toggle Error");
                }
            }
        }
    }

    public void releaseAll() {
        try {
            for (int i = 0; i < playerMap.size(); i++) {
                ExoPlayer p = playerMap.valueAt(i);
                if (p != null) ExoPlayerManager.releasePlayer(p);
            }
            playerMap.clear();
        } catch (Exception e) {
            android.util.Log.e("PreviewAdapter", "ReleaseAll Error");
        }
    }

    @Override
    public int getItemCount() { return mediaList != null ? mediaList.size() : 0; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView imgPreview;
        public final PlayerView playerView;
        public final View loaderContainer;

        public ViewHolder(View v) {
            super(v);
            imgPreview = v.findViewById(R.id.imgItem);
            playerView = v.findViewById(R.id.pvItem);
            loaderContainer = v.findViewById(R.id.loaderContainer);
        }

        public void resetUI() {
            imgPreview.setVisibility(View.VISIBLE);
            imgPreview.setAlpha(1.0f);
            imgPreview.clearAnimation();

            playerView.setVisibility(View.INVISIBLE);
            playerView.setAlpha(0.0f);
            playerView.setPlayer(null);
            playerView.clearAnimation();

            if (loaderContainer != null) loaderContainer.setVisibility(View.GONE);
        }

        public void showPlayerUI() {
            playerView.setVisibility(View.VISIBLE);
            playerView.animate().alpha(1.0f).setDuration(400).start();
            imgPreview.animate().alpha(0.0f).setDuration(400)
                    .withEndAction(() -> imgPreview.setVisibility(View.INVISIBLE))
                    .start();
        }
    }
}