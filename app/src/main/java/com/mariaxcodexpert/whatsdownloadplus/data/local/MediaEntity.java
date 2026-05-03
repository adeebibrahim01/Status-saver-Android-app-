package com.mariaxcodexpert.whatsdownloadplus.data.local;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(
        tableName = "media_table",
        indices = {
                @Index("timestamp"),
                @Index("isDownloaded"),
                @Index(value = {"isDownloaded", "timestamp"}),
                @Index(value = {"fileName"}, unique = true)
        }
)
public class MediaEntity implements Parcelable {

    @PrimaryKey
    @NonNull
    public String fileName;

    public String whatsapp_path;
    public String gallery_path;
    public boolean isVideo;
    public long timestamp;
    public boolean isDownloaded;
    public long expiryTime;

    // Standard Constructor for Room
    public MediaEntity(@NonNull String fileName, String whatsapp_path, String gallery_path,
                       boolean isVideo, long timestamp, boolean isDownloaded, long expiryTime) {
        this.fileName = fileName;
        this.whatsapp_path = whatsapp_path;
        this.gallery_path = gallery_path;
        this.isVideo = isVideo;
        this.timestamp = timestamp;
        this.isDownloaded = isDownloaded;
        this.expiryTime = expiryTime;
    }

    // --- Getters ---

    public String getGalleryPath() {
        // Safe check: return gallery_path if available, otherwise fallback to whatsapp_path
        return (isDownloaded && gallery_path != null && !gallery_path.isEmpty())
                ? gallery_path
                : whatsapp_path;
    }

    // Is method ka naam same rakhein jo adapter call kar raha hai
    public boolean isVideo() {
        return isVideo;
    }

    @Ignore
    public Uri getUri() {
        String path = getGalleryPath();
        if (path == null || path.isEmpty()) return Uri.EMPTY;
        try {
            return Uri.parse(path);
        } catch (Exception e) {
            return Uri.EMPTY;
        }
    }

    @Ignore
    public void markAsDownloaded(String savedGalleryPath) {
        this.isDownloaded = true;
        this.gallery_path = savedGalleryPath;
        this.timestamp = System.currentTimeMillis();
    }

    // --- Parcelable Implementation ---

    @Ignore // 🔥 Room should ignore this constructor
    protected MediaEntity(Parcel in) {
        fileName = Objects.requireNonNull(in.readString());
        whatsapp_path = in.readString();
        gallery_path = in.readString();
        isVideo = in.readByte() != 0;
        timestamp = in.readLong();
        isDownloaded = in.readByte() != 0;
        expiryTime = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fileName);
        dest.writeString(whatsapp_path);
        dest.writeString(gallery_path);
        dest.writeByte((byte) (isVideo ? 1 : 0));
        dest.writeLong(timestamp);
        dest.writeByte((byte) (isDownloaded ? 1 : 0));
        dest.writeLong(expiryTime);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<MediaEntity> CREATOR = new Creator<MediaEntity>() {
        @Override
        public MediaEntity createFromParcel(Parcel in) { return new MediaEntity(in); }
        @Override
        public MediaEntity[] newArray(int size) { return new MediaEntity[size]; }
    };

    // --- Logic ---

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaEntity that = (MediaEntity) o;
        return isVideo == that.isVideo &&
                timestamp == that.timestamp &&
                isDownloaded == that.isDownloaded &&
                expiryTime == that.expiryTime &&
                fileName.equals(that.fileName) &&
                Objects.equals(whatsapp_path, that.whatsapp_path) &&
                Objects.equals(gallery_path, that.gallery_path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, whatsapp_path, gallery_path, isVideo, timestamp, isDownloaded, expiryTime);
    }
}