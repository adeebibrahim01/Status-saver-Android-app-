package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.net.Uri;

// --- Helper class to store URI + type ---
public class MediaItem {
    public Uri uri;
    public boolean isVideo;

    public MediaItem(Uri uri, boolean isVideo) {
        this.uri = uri;
        this.isVideo = isVideo;
    }
}
