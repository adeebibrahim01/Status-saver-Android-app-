package com.mariaxcodexpert.whatsdownloadplus.ui.utils.media.player;

import android.content.Context;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import com.mariaxcodexpert.whatsdownloadplus.ui.preview.PreviewPagerAdapter;
import com.mariaxcodexpert.whatsdownloadplus.utils.player.VideoCacheManager;

import java.util.Objects;

@OptIn(markerClass = UnstableApi.class)
public class ExoPlayerManager {
    private static final String TAG = "EXO_DEBUG";

    /**
     * Creates a pre-configured ExoPlayer instance with Optimized Buffer for Status Videos.
     */
    public static ExoPlayer createPlayer(Context context) {
        CacheDataSource.Factory cacheFactory = new CacheDataSource.Factory()
                .setCache(Objects.requireNonNull(VideoCacheManager.getCache(context)))
                .setUpstreamDataSourceFactory(new DefaultDataSource.Factory(context))
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

        // Optimized for small WhatsApp status files to start instantly
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(500, 2000, 500, 500)
                .build();

        return new ExoPlayer.Builder(context)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(cacheFactory))
                .setLoadControl(loadControl)
                .build();
    }

    /**
     * Configures listeners and handles UI state transitions.
     */
    public static void setupPlayerWithListeners(ExoPlayer player, int pos, PreviewPagerAdapter.ViewHolder h, Runnable onReady) {
        if (player == null || h == null) return;

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (h.loaderContainer == null) return;

                if (state == Player.STATE_READY) {
                    h.loaderContainer.setVisibility(View.GONE);
                    if (onReady != null) onReady.run();
                } else if (state == Player.STATE_BUFFERING) {
                    h.loaderContainer.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onRenderedFirstFrame() {
                // Ensure UI transition happens exactly when pixels are visible
                if (onReady != null) onReady.run();
                Log.d(TAG, "🎥 First Frame Rendered at: " + pos);
            }

            @Override
            public void onPlayerError(@NonNull androidx.media3.common.PlaybackException e) {
                Log.e(TAG, "❌ Error at " + pos + ": " + e.getMessage());
                if (h.loaderContainer != null) h.loaderContainer.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Safely releases player resources.
     */
    public static void releasePlayer(ExoPlayer p) {
        if (p != null) {
            p.stop();
            p.release();
        }
    }
}