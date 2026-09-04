package com.mariaxcodexpert.whatsdownloadplus.ui.Search;

import java.io.Serializable;

public class MediaItem implements Serializable {

    private static final long serialVersionUID = 2L;

    private String url;
    private String videoUrl;
    private boolean isVideo;
    private boolean isDownloaded;
    private String title;

    public MediaItem(String url, String videoUrl, boolean isVideo, String title) {
        this.url = (url != null) ? url : "";
        this.videoUrl = (videoUrl != null) ? videoUrl : "";
        this.isVideo = isVideo;
        this.title = (title != null) ? title : "";
        this.isDownloaded = false;
    }

    public String getUrl() {
        return (url != null) ? url : "";
    }

    public String getVideoUrl() {
        if (videoUrl == null || videoUrl.isEmpty()) {
            return getUrl();
        }
        return videoUrl;
    }

    public String getTitle() {
        if (title == null || title.trim().isEmpty()) {
            return "";
        }
        if (title.length() > 0) {
            return title.substring(0, 1).toUpperCase() + title.substring(1).toLowerCase();
        }
        return title;
         }


    public void setUrl(String url) {
        this.url = (url != null) ? url : "";
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = (videoUrl != null) ? videoUrl : "";
    }

    public void setTitle(String title) {
        this.title = (title != null) ? title : "";
    }

    public boolean isVideo() {
        return isVideo;
    }

    public void setVideo(boolean video) {
        isVideo = video;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }

    public boolean isValid() {
        return url != null && !url.isEmpty();
    }
}