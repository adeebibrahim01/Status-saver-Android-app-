package com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

@Dao
public interface VideoDao {

    @Query("SELECT * FROM videos_table WHERE fileName = :name LIMIT 1")
    VideoEntity getVideoByFileName(String name);

    @Query("SELECT * FROM videos_table WHERE whatsapp_path = :uri OR gallery_path = :uri LIMIT 1")
    VideoEntity getVideoByUri(String uri);

    @Query("SELECT * FROM videos_table WHERE isDownloaded = 1 AND gallery_path IS NOT NULL AND gallery_path != '' ORDER BY downloadTime DESC LIMIT 3")
    List<VideoEntity> getOnlyDownloadedVideosSync();

    @Query("SELECT * FROM videos_table WHERE isDownloaded = 1 AND gallery_path IS NOT NULL ORDER BY downloadTime DESC")
    LiveData<List<VideoEntity>> getOnlyDownloadedVideos();
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertVideo(VideoEntity video);

    @Query("DELETE FROM videos_table WHERE isDownloaded = 0")
    void clearAllUnsavedVideos();

    @Query("DELETE FROM videos_table WHERE isDownloaded = 0 AND fileName NOT IN (:currentNames)")
    void deleteGhostVideos(List<String> currentNames);

    @Query("SELECT * FROM videos_table WHERE fileName IN (:activeNames) ORDER BY lastModified DESC")
    List<VideoEntity> getActiveVideosSync(List<String> activeNames);

    @Query("DELETE FROM videos_table WHERE lastModified < :thresholdTime AND isDownloaded = 0")
    void deleteUnsavedRecords(long thresholdTime);

    @Query("DELETE FROM videos_table WHERE lastModified < :thresholdTime")
    void deleteOldRecords(long thresholdTime);

    @Update
    void updateVideo(VideoEntity video);

    @Query("DELETE FROM videos_table WHERE isDownloaded = 0 AND expiryTime < :now")
    void cleanupExpiredVideos(long now);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertVideos(List<VideoEntity> videos);

    @Query("UPDATE videos_table SET isDownloaded = :status, gallery_path = :path, downloadTime = :dTime WHERE fileName = :name")
    void updateVideoDownloadStatus(String name, boolean status, String path, long dTime);

    @Query("SELECT EXISTS(SELECT 1 FROM videos_table WHERE fileName = :name LIMIT 1)")
    boolean isVideoExists(String name);

    @Query("SELECT * FROM videos_table WHERE isDownloaded = 0")
    List<VideoEntity> getAllUnsavedVideosSync();

    @Query("SELECT * FROM videos_table WHERE expiryTime > :now ORDER BY lastModified DESC")
    List<VideoEntity> getActiveVideosSync(long now);

    @Query("SELECT * FROM videos_table WHERE isDownloaded = 0")
    List<VideoEntity> getNonDownloadedVideosSync();

    @Query("SELECT * FROM videos_table WHERE fileName = :name LIMIT 1")
    VideoEntity getVideoByFileNameSync(String name);

    @Query("SELECT * FROM videos_table WHERE isDownloaded = 1")
    LiveData<List<VideoEntity>> getSavedVideos();

    @Query("SELECT * FROM videos_table WHERE expiryTime > :now")
    LiveData<List<VideoEntity>> getActiveVideos(long now);

    @Query("SELECT * FROM videos_table")
    List<VideoEntity> getAllVideosSync();

    @Query("SELECT fileName FROM videos_table")
    List<String> getAllVideoNamesSync();

    @Query("SELECT * FROM videos_table WHERE isDownloaded = 1 ORDER BY downloadTime DESC LIMIT 5")
    LiveData<List<VideoEntity>> getRecentVideosLive();

    @Query("SELECT COUNT(*) FROM videos_table WHERE isDownloaded = 1 AND downloadTime >= :todayStart")
    int getTodayCountSync(long todayStart);

    @Query("SELECT COUNT(*) FROM videos_table WHERE downloadTime >= :startTime AND downloadTime <= :endTime")
    int getCountInRangeSync(long startTime, long endTime);

    @Query("SELECT COUNT(*) FROM videos_table WHERE isDownloaded = 1")
    int getDownloadedCountSync();

    @Query("SELECT COUNT(*) FROM videos_table WHERE expiryTime > :now")
    int getActiveCountSync(long now);


    @Query("SELECT COUNT(*) FROM videos_table WHERE isDownloaded = 1 AND downloadTime > :since")
    int getSavedCountAfter(long since);

    @Query("SELECT * FROM videos_table WHERE isDownloaded = 1 AND fileName IN (:activeDiskList) ORDER BY lastModified DESC LIMIT 3")
    List<VideoEntity> getRecentSavedVideos(List<String> activeDiskList);
}