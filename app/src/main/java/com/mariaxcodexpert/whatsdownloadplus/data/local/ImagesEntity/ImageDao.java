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

    @Query("SELECT * FROM images_table WHERE isDownloaded = 1 AND gallery_path IS NOT NULL ORDER BY downloadTime DESC")
    LiveData<List<ImageEntity>> getOnlyDownloadedImages();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertImage(ImageEntity image); // List ki bajaye single entity

    // ImageDao.java mein
    @Query("DELETE FROM images_table WHERE isDownloaded = 0 AND fileName NOT IN (:currentNames)")
    void deleteGhostImages(List<String> currentNames);

    @Query("DELETE FROM images_table WHERE lastModified < :thresholdTime AND isDownloaded = 0")
    void deleteUnsavedRecords(long thresholdTime);

    @Query("DELETE FROM images_table WHERE lastModified < :thresholdTime")
    void deleteOldRecords(long thresholdTime);
    // --- Cleanup Queries ---
    @Update
    void updateImage(ImageEntity image);

    @Query("DELETE FROM images_table WHERE fileName = :name AND isDownloaded = 0")
    void deleteSpecificUnsaved(String name);

    @Query("DELETE FROM images_table WHERE isDownloaded = 0 AND expiryTime < :currentTime")
    void deleteExpiredUnsaved(long currentTime);

    @Query("DELETE FROM images_table WHERE isDownloaded = 1 AND lastModified < :threshold")
    void deleteOldSavedRecords(long threshold);



    @Query("DELETE FROM images_table WHERE isDownloaded = 0 AND expiryTime < :now")
    void cleanupExpiredImages(long now);

    @Query("DELETE FROM images_table WHERE fileName = :name")
    void deleteImageByName(String name);

    @Delete
    void deleteImage(ImageEntity image);

    // --- Core Sync & Update Actions ---

    /**
     * 🔥 CRITICAL FIX: Strategy ko REPLACE se IGNORE kar diya hai.
     * Is se sync ke waqt purani downloaded rows overwrite/reset nahi hongi.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertImages(List<ImageEntity> images);

    /**
     * 🔥 TARGETED UPDATE: Sirf zaroori columns update karta hai.
     * Naya record banane ke bajaye maujooda row ka status change karta hai.
     */
    @Query("UPDATE images_table SET isDownloaded = :status, gallery_path = :path, downloadTime = :dTime WHERE fileName = :name")
    void updateImageDownloadStatus(String name, boolean status, String path, long dTime);

    @Query("SELECT EXISTS(SELECT 1 FROM images_table WHERE fileName = :name LIMIT 1)")
    boolean isImageExists(String name);


    // --- Retrieval Queries (Sync & Live) ---

    @Query("DELETE FROM images_table WHERE isDownloaded = 0")
    void clearAllUnsavedImages();

    @Query("SELECT * FROM images_table WHERE isDownloaded = 0")
    List<ImageEntity> getAllUnsavedImagesSync();

    // Purani query ko hata kar ye wali laga dein
    @Query("SELECT * FROM images_table WHERE fileName IN (:activeNames) ORDER BY lastModified DESC")
    List<ImageEntity> getActiveImagesSync(List<String> activeNames);


//    @Query("SELECT * FROM images_table WHERE expiryTime > :now ORDER BY lastModified DESC")
//    List<ImageEntity> getActiveImagesSync(long now);

    @Query("SELECT * FROM images_table WHERE isDownloaded = 0")
    List<ImageEntity> getNonDownloadedImagesSync();

    @Query("SELECT * FROM images_table WHERE fileName = :name LIMIT 1")
    ImageEntity getImageByFileNameSync(String name);

    @Query("SELECT * FROM images_table WHERE isDownloaded = 1")
    LiveData<List<ImageEntity>> getSavedImages();

    @Query("SELECT * FROM images_table WHERE expiryTime > :now")
    LiveData<List<ImageEntity>> getActiveImages(long now);

    @Query("SELECT * FROM images_table")
    List<ImageEntity> getAllImagesSync();

    @Query("SELECT fileName FROM images_table")
    List<String> getAllImageNamesSync();

    /**
     * 🔥 Recent items mein latest downloads top par dikhane ke liye logic
     */
    @Query("SELECT * FROM images_table WHERE isDownloaded = 1 ORDER BY downloadTime DESC LIMIT 5")
    LiveData<List<ImageEntity>> getRecentImagesLive();

    // --- Statistics & Counting ---

    @Query("SELECT COUNT(*) FROM images_table WHERE isDownloaded = 1 AND downloadTime >= :todayStart")
    int getTodayCountSync(long todayStart);

    @Query("SELECT COUNT(*) FROM images_table WHERE downloadTime >= :startTime AND downloadTime <= :endTime")
    int getCountInRangeSync(long startTime, long endTime);

    @Query("SELECT COUNT(*) FROM images_table WHERE isDownloaded = 1")
    int getDownloadedCountSync();

    @Query("SELECT COUNT(*) FROM images_table WHERE expiryTime > :now")
    int getActiveCountSync(long now);

    @Query("SELECT COUNT(*) FROM images_table WHERE isDownloaded = 1 AND downloadTime > :since")
    int getSavedCountAfter(long since);

    @Query("SELECT * FROM images_table WHERE isDownloaded = 1 AND fileName IN (:activeDiskList) ORDER BY lastModified DESC LIMIT 3")
    List<ImageEntity> getRecentSavedImages(List<String> activeDiskList);
}