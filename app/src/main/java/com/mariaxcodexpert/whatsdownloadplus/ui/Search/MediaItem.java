package com.mariaxcodexpert.whatsdownloadplus.ui.Search;

import java.io.Serializable; // 🔥 1. Import zaroori hai

/**
 * Developed by MariaXCodeExpert
 * Updated for Intent Transfer & Premium Discovery
 */
public class MediaItem implements Serializable { // 🔥 2. Interface implement kerna h

    // 🔥 3. Serial ID: Taake transfer k waqt versioning error na aye
    private static final long serialVersionUID = 1L;

    private String url;
    private String videoUrl;
    private boolean isVideo;
    private boolean isDownloaded;

    public MediaItem(String url, String videoUrl, boolean isVideo) {
        this.url = url;
        this.videoUrl = videoUrl;
        this.isVideo = isVideo;
        this.isDownloaded = false;
    }

    // Getters and Setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public boolean isVideo() { return isVideo; }
    public void setVideo(boolean video) { isVideo = video; }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }
}