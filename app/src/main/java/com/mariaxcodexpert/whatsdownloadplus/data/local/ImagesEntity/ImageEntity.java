package com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "images_table", indices = {@Index(value = {"fileName"}, unique = true)})
public class ImageEntity implements Serializable {

    @PrimaryKey
    @NonNull
    public String fileName;

    public String whatsapp_path;
    public String gallery_path;

    public long lastModified;

    public boolean isDownloaded;
    public long expiryTime;
    public long downloadTime;

    @ColumnInfo(name = "uri")
    private String uri;

    public ImageEntity(@NonNull String fileName, String uri, String gallery_path, long lastModified, boolean isDownloaded, long expiryTime) {
        this.fileName = fileName;
        this.uri = uri;
        this.gallery_path = gallery_path;
        this.lastModified = lastModified;
        this.isDownloaded = isDownloaded;
        this.expiryTime = expiryTime;
        this.downloadTime = 0;
    }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
    public String getGalleryPath() { return gallery_path; }
    public long getDownloadTime() { return downloadTime; }
    public void setDownloadTime(long downloadTime) { this.downloadTime = downloadTime; }
}