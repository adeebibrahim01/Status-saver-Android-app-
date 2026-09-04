package com.mariaxcodexpert.whatsdownloadplus.ui.utils.media.player;

import android.content.Context;
import android.util.Log;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import java.io.File;

@OptIn(markerClass = UnstableApi.class)
public class VideoCacheManager {
    private static final String TAG = "VideoCacheManager";
    private static SimpleCache sDownloadCache;
    private static final long CACHE_SIZE = 250 * 1024 * 1024; // 250MB
    private static final String CACHE_DIR = "status_video_cache_v2";

    public static synchronized SimpleCache getCache(Context context) {
        if (sDownloadCache == null) {
            try {
                File cacheDir = new File(context.getApplicationContext().getCacheDir(), CACHE_DIR);
                if (!cacheDir.exists()) cacheDir.mkdirs();

                if (!SimpleCache.isCacheFolderLocked(cacheDir)) {
                    sDownloadCache = new SimpleCache(cacheDir,
                            new LeastRecentlyUsedCacheEvictor(CACHE_SIZE),
                            new StandaloneDatabaseProvider(context.getApplicationContext()));
                    Log.d(TAG, "Video Cache Initialized: 250MB");
                }
            } catch (Exception e) {
                Log.e(TAG, "Cache Error: " + e.getMessage());
            }
        }
        return sDownloadCache;
    }

    public static String getReadableCacheSize() {
        return (sDownloadCache == null) ? "0 MB" : (sDownloadCache.getCacheSpace() / (1024 * 1024)) + " MB";
    }

    public static synchronized void clearAllCache() {
        if (sDownloadCache == null) return;
        try {
            for (String key : sDownloadCache.getKeys()) sDownloadCache.removeResource(key);
            Log.d(TAG, "Cache cleared.");
        } catch (Exception e) {
            Log.e(TAG, "Clear Error: " + e.getMessage());
        }
    }

    public static synchronized void releaseCache() {
        if (sDownloadCache != null) {
            sDownloadCache.release();
            sDownloadCache = null;
            Log.d(TAG, "Cache released.");
        }
    }
}