package com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

@Dao
public interface ImageDao {


    @Query("SELECT * FROM images_table WHERE fileName = :name LIMIT 1")
    ImageEntity getImageByFileName(String name);

    @Query("SELECT * FROM images_table WHERE whatsapp_path = :uri OR gallery_path = :uri LIMIT 1")
    ImageEntity getImageByUri(String uri);

    @Query("SELECT * FROM images_table WHERE isDownloaded = 1 AND gallery_path IS NOT NULL AND gallery_path != '' ORDER BY downloadTime DESC LIMIT 3")
    List<ImageEntity> getOnlyDownloadedImagesSync();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertImage(ImageEntity image);

    @Query("DELETE FROM images_table WHERE isDownloaded = 0 AND fileName NOT IN (:currentNames)")
    void deleteGhostImages(List<String> currentNames);

    @Query("DELETE FROM images_table WHERE lastModified < :thresholdTime")
    void deleteOldRecords(long thresholdTime);

    @Update
    void updateImage(ImageEntity image);

    @Query("DELETE FROM images_table WHERE isDownloaded = 0 AND expiryTime < :now")
    void cleanupExpiredImages(long now);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertImages(List<ImageEntity> images);

    @Query("UPDATE images_table SET isDownloaded = :status, gallery_path = :path, downloadTime = :dTime WHERE fileName = :name")
    void updateImageDownloadStatus(String name, boolean status, String path, long dTime);

    @Query("SELECT EXISTS(SELECT 1 FROM images_table WHERE fileName = :name LIMIT 1)")
    boolean isImageExists(String name);

    @Query("DELETE FROM images_table WHERE isDownloaded = 0")
    void clearAllUnsavedImages();

    @Query("SELECT * FROM images_table WHERE fileName IN (:activeNames) ORDER BY lastModified DESC")
    List<ImageEntity> getActiveImagesSync(List<String> activeNames);

    @Query("SELECT * FROM images_table WHERE isDownloaded = 1")
    LiveData<List<ImageEntity>> getSavedImages();

    @Query("SELECT * FROM images_table WHERE expiryTime > :now")
    LiveData<List<ImageEntity>> getActiveImages(long now);

    @Query("SELECT * FROM images_table")
    List<ImageEntity> getAllImagesSync();

    @Query("SELECT COUNT(*) FROM images_table WHERE isDownloaded = 1 AND downloadTime >= :todayStart")
    int getTodayCountSync(long todayStart);
}