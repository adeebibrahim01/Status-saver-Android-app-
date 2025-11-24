package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import androidx.documentfile.provider.DocumentFile;

public class MediaItem {
    public final DocumentFile file;
    public final long expiryTime; // calculated once

    public MediaItem(DocumentFile file) {
        this.file = file;
        // 24 hours from file lastModified
        this.expiryTime = file.lastModified() + 24 * 60 * 60 * 1000;
    }
}
