package com.mariaxcodexpert.whatsdownloadplus.ui.trending;

public class TrendingModel {
    private String title;
    private String thumbnailUrl;
    private String videoUrl;
    private String mediaType; // "image" ya "video" identify karne ke liye

    // Empty constructor Firebase ke liye zaroori hai
    public TrendingModel() {}

    public TrendingModel(String title, String thumbnailUrl, String videoUrl, String mediaType) {
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.videoUrl = videoUrl;
        this.mediaType = mediaType;
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getVideoUrl() { return videoUrl; }
    public String getMediaType() { return mediaType; }
}