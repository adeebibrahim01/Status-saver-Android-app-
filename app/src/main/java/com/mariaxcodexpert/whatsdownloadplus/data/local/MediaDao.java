package com.mariaxcodexpert.whatsdownloadplus.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public abstract class MediaDao {

    // MediaDao.java mein ye queries check karein
    @Query("SELECT * FROM media_table WHERE isVideo = 0 AND expiryTime > :currentTime")
    public abstract LiveData<List<MediaEntity>> getActiveImages(long currentTime);

    @Query("SELECT * FROM media_table WHERE isVideo = 1 AND expiryTime > :currentTime")
    public abstract LiveData<List<MediaEntity>> getActiveVideos(long currentTime);

    @Query("DELETE FROM media_table WHERE fileName = :name")
    public abstract void deleteMediaByFileName(String name);

    @Query("SELECT fileName FROM media_table") // Check karein aapki table ka naam 'media_table' hi hai na?
    public abstract List<String> getAllFileNamesSync();

    @Query("SELECT * FROM media_table WHERE expiryTime > :currentTime ")
    public abstract LiveData<List<MediaEntity>> getAllActiveMedia(long currentTime);
    @Query("DELETE FROM media_table WHERE " +
            "(isDownloaded = 1 AND timestamp < :thresholdTime) OR " +
            "(isDownloaded = 0 AND expiryTime < :currentTime)")
    public abstract void performDeepCleanup(long thresholdTime, long currentTime);

    @Query("DELETE FROM media_table WHERE isDownloaded = 1 AND timestamp < :thresholdTime")
    public abstract void deleteOldDownloadRecords(long thresholdTime);
    // Is query ko update karein ya count ke liye use karein
    @Query("SELECT COUNT(*) FROM media_table WHERE expiryTime > :currentTime")
    public abstract int getActiveStatusCountSync(long currentTime);

    @Query("SELECT * FROM media_table WHERE isVideo = :isVideo AND expiryTime > :currentTime")
    public abstract LiveData<List<MediaEntity>> getActiveMediaByType(boolean isVideo, long currentTime);

    @Query("SELECT * FROM media_table WHERE gallery_path = :path LIMIT 1")
    public abstract  MediaEntity getMediaByGalleryPathSync(String path);


    @Query("SELECT * FROM media_table WHERE fileName = :name LIMIT 1")
    public abstract LiveData<MediaEntity> getMediaByName(String name);

    /**
     * 🔥 Specific file ki live information fetch karne ke liye.
     * Ye method background thread (Executor) mein call hoga.
     */
    @Query("SELECT * FROM media_table WHERE fileName = :name LIMIT 1")
    public abstract MediaEntity getMediaByNameSync(String name);

    // 🔥 persistence ensure karne ke liye query (Agar upar wala isExists kaam na kare)
    @Query("UPDATE media_table SET whatsapp_path = :newPath WHERE fileName = :name")
    public abstract void updateWhatsAppPath(String name, String newPath);

    // Ye check karega ke kya item DB mein hai?
    @Query("SELECT EXISTS(SELECT 1 FROM media_table WHERE fileName = :name LIMIT 1)")
    public abstract boolean isExists(String name);


    // --- 1. ALL MEDIA (Images/Videos Fragments ke liye) ---
    @Query("SELECT * FROM media_table WHERE isVideo = :isVideo ORDER BY expiryTime DESC")
    public abstract LiveData<List<MediaEntity>> getAllMediaByType(boolean isVideo);


    // 🔥 4 Arguments waali query (Abstract method)
    @Query("UPDATE media_table SET isDownloaded = :status, gallery_path = :path, timestamp = :time WHERE fileName = :name")
    public abstract void updateDownloadStatus(String name, boolean status, String path, long time);

    // Agar aap poora object update karna chahte hain (Alternate fast way)
    @Update
    public abstract void updateMedia(MediaEntity entity);

    // Purana LiveData (Sari 2000 files load karta tha - Isay ab sirf small lists ke liye use karein)
    @Query("SELECT * FROM media_table WHERE isDownloaded = 1 ORDER BY timestamp DESC")
    public abstract LiveData<List<MediaEntity>> getDownloadedMediaLive();
    /**
     * 🔥 200x SPEED ENGINE: Pagination Query
     * Ye 2000 items ko ek sath load karne ke bajaye 'limit' ke hisab se load karega.
     * @param limit Kitne items chahiye (e.g., 20)
     * @param offset Kahan se shuru karna hai (e.g., 0, 20, 40...)
     */
    @Query("SELECT * FROM media_table WHERE isDownloaded = 1 ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    public abstract List<MediaEntity> getDownloadedMediaPaged(int limit, int offset);

    // 🔥 Optimized for Home Fragment (Horizontal Scroll)
    @Query("SELECT * FROM media_table WHERE isDownloaded = 1 ORDER BY timestamp DESC LIMIT 10")
    public abstract LiveData<List<MediaEntity>> getRecentDownloadsLive();


    // --- 3. CORE ACTIONS (Insert/Update/Delete) ---
    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertAll(List<MediaEntity> items);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertMedia(MediaEntity item);

    @Query("DELETE FROM media_table WHERE gallery_path = :uri OR whatsapp_path = :uri")
    public abstract void deleteMediaByUri(String uri);



    @Query("SELECT COUNT(*) FROM media_table WHERE isDownloaded = 1")
    public abstract int getDownloadedCountSync();

    @Query("SELECT COUNT(*) FROM media_table WHERE isDownloaded = 1 AND timestamp >= :todayStart")
    public abstract int getTodayDownloadedCountSync(long todayStart);

    /**
     * 🔥 NEW: Specific Update for Active Statuses
     * Poora object replace karne ke bajaye sirf count update karein (Super Fast)
     */
    @Query("UPDATE dashboard_stats SET activeStatuses = :count WHERE id = 1")
    public abstract void updateActiveStatusCount(int count);

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertDashboardStats(HomeDashboardEntity stats);

    @Transaction
    public void updateDashboardStats(HomeDashboardEntity stats) {
        insertDashboardStats(stats);
    }

    @Query("SELECT * FROM dashboard_stats WHERE id = 1")
    public abstract LiveData<HomeDashboardEntity> getDashboardStatsLive();

    @Query("SELECT * FROM dashboard_stats WHERE id = 1")
    public abstract HomeDashboardEntity getDashboardStatsSync();


    // --- 5. SYNC & FETCH UTILS ---
    @Query("SELECT * FROM media_table WHERE isDownloaded = 1")
    public abstract List<MediaEntity> getAllSavedMediaSync();

    @Query("SELECT * FROM media_table WHERE fileName = :name LIMIT 1")
    public abstract MediaEntity getMediaByFileNameSync(String name);

    @Query("DELETE FROM media_table WHERE isDownloaded = 1")
    public abstract void deleteAllSavedRecords();

    @Query("DELETE FROM media_table WHERE isDownloaded = 0")
    public abstract void clearTempMedia();

    }