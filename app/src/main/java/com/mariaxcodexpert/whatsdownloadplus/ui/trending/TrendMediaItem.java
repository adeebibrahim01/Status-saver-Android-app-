package com.mariaxcodexpert.whatsdownloadplus.ui.trending;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;
import java.io.Serializable;

public class TrendMediaItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String title;

    @PropertyName("thumbnailUrl")
    private String thumbnailUrl;

    @PropertyName("mediaUrl")
    private String mediaUrl;

    @PropertyName("mediaType")
    private String mediaType;

    // 🔥 NEW: Download status track karne ke liye (Ye Firebase mein save nahi hoga)
    @Exclude
    private boolean isDownloaded = false;

    // Default constructor required for Firebase
    public TrendMediaItem() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @PropertyName("thumbnailUrl")
    public String getThumbnailUrl() { return thumbnailUrl; }

    @PropertyName("thumbnailUrl")
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    @PropertyName("mediaUrl")
    public String getMediaUrl() { return mediaUrl; }

    @PropertyName("mediaUrl")
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    @PropertyName("mediaType")
    public String getMediaType() { return mediaType; }

    @PropertyName("mediaType")
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    // 🔥 DOWNLOAD LOGIC GETTER/SETTER
    @Exclude
    public boolean isDownloaded() { return isDownloaded; }

    @Exclude
    public void setDownloaded(boolean downloaded) { isDownloaded = downloaded; }

    // Helper method to check type
    @Exclude
    public boolean isVideo() {
        return "video".equalsIgnoreCase(mediaType);
    }
}