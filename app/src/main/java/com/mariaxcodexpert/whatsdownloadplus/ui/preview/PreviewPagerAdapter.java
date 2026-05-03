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

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_preview_media, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int p) {
        Object item = mediaList.get(p);
        String path = (item instanceof ImageEntity) ? ((ImageEntity) item).getUri() : ((VideoEntity) item).getUri();

        h.imgPreview.setVisibility(View.VISIBLE);
        h.imgPreview.setAlpha(1.0f);
        h.playerView.setVisibility(View.INVISIBLE);
        h.playerView.setPlayer(null);
        if (h.loaderContainer != null) h.loaderContainer.setVisibility(View.GONE);

        Glide.with(context).load(path).diskCacheStrategy(DiskCacheStrategy.ALL).centerInside().into(h.imgPreview);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder h) {
        super.onViewAttachedToWindow(h);
        int pos = h.getBindingAdapterPosition();
        if (pos == RecyclerView.NO_POSITION || !(mediaList.get(pos) instanceof VideoEntity)) return;

        ExoPlayer player = ExoPlayerManager.createPlayer(context);
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.setMediaItem(MediaItem.fromUri(Uri.parse(((VideoEntity) mediaList.get(pos)).getUri())));
        player.prepare();
        player.setPlayWhenReady(false);

        ExoPlayerManager.setupPlayerWithListeners(player, pos, h, () -> {
            h.playerView.setVisibility(View.VISIBLE);
            h.playerView.animate().alpha(1.0f).setDuration(400).start();
            h.imgPreview.animate().alpha(0.0f).setDuration(400).withEndAction(() -> h.imgPreview.setVisibility(View.INVISIBLE)).start();
        });

        h.playerView.setPlayer(player);
        playerMap.put(pos, player);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder h) {
        super.onViewDetachedFromWindow(h);
        int pos = h.getBindingAdapterPosition();
        ExoPlayer p = playerMap.get(pos);
        if (p != null) {
            ExoPlayerManager.releasePlayer(p);
            playerMap.remove(pos);
            h.playerView.setPlayer(null);
        }
    }

    public void handlePlayback(int currentPos) {
        for (int i = 0; i < playerMap.size(); i++) {
            ExoPlayer p = playerMap.valueAt(i);
            if (p != null) { if (playerMap.keyAt(i) == currentPos) p.play(); else p.pause(); }
        }
    }

    public void releaseAll() {
        for (int i = 0; i < playerMap.size(); i++) { if (playerMap.valueAt(i) != null) ExoPlayerManager.releasePlayer(playerMap.valueAt(i)); }
        playerMap.clear();
    }

    @Override public int getItemCount() { return mediaList != null ? mediaList.size() : 0; }

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
    }
}