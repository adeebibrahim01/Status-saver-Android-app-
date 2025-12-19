package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.net.Uri;

public class MediaItem {
    public Uri uri;
    public boolean isVideo;
    public long dateAdded; // 🔥 timestamp

    public MediaItem(Uri uri, boolean isVideo, long dateAdded) {
        this.uri = uri;
        this.isVideo = isVideo;
        this.dateAdded = dateAdded;
    }
}
