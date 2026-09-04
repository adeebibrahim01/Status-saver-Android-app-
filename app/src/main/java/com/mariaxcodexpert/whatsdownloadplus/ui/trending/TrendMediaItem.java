package com.mariaxcodexpert.whatsdownloadplus.ui.trending;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;
import java.io.Serializable;

@Keep
public class TrendMediaItem implements Serializable {

    public static final long serialVersionUID = 3L;

    public String id = "";
    public String title = "";

    @PropertyName("country")
    public String country = "";

    @PropertyName("thumbnailUrl")
    public String thumbnailUrl = "";

    @PropertyName("mediaUrl")
    public String mediaUrl = "";

    @PropertyName("mediaType")
    public String mediaType = "";

    @Exclude
    public boolean isDownloaded = false;

    @PropertyName("original_keyword")
    public String original_keyword = "";


    public TrendMediaItem() {
        this.id = "";
        this.title = "";
        this.country = "";
        this.thumbnailUrl = "";
        this.mediaUrl = "";
        this.mediaType = "image";
        this.original_keyword = "";
    }

    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }
    public String getTitle() {
        return (title != null) ? title : "";
    }
    public void setTitle(String title) { this.title = title; }

    @PropertyName("country")
    public String getCountry() {
        return country != null ? country : "";
    }

    @PropertyName("country")
    public void setCountry(String country) { this.country = country; }

    @PropertyName("thumbnailUrl")
    public String getThumbnailUrl() { return thumbnailUrl != null ? thumbnailUrl : ""; }

    @PropertyName("thumbnailUrl")
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    @PropertyName("mediaUrl")
    public String getMediaUrl() { return mediaUrl != null ? mediaUrl : ""; }

    @PropertyName("mediaUrl")
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    @PropertyName("mediaType")
    public String getMediaType() { return mediaType != null ? mediaType : "image"; }

    @PropertyName("mediaType")
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    @Exclude
    public boolean isDownloaded() { return isDownloaded; }

    @Exclude
    public void setDownloaded(boolean downloaded) { isDownloaded = downloaded; }

    @Exclude
    public boolean isVideo() {
        return "video".equalsIgnoreCase(getMediaType());
    }

    @PropertyName("original_keyword")
    public String getOriginal_keyword() { return original_keyword != null ? original_keyword : ""; }

    @PropertyName("original_keyword")
    public void setOriginal_keyword(String original_keyword) { this.original_keyword = original_keyword; }

    @NonNull
    @Override
    public String toString() {
        return "TrendItem{id='" + id + "', type='" + mediaType + "'}";
    }
}