package com.mariaxcodexpert.whatsdownloadplus.data.local.Dashboard;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.mariaxcodexpert.whatsdownloadplus.data.local.HomeDashboardEntity;

@Dao
public interface DashboardDao {

    @Query("SELECT (SELECT COUNT(*) FROM images_table WHERE isDownloaded = 1) + " +
            "(SELECT COUNT(*) FROM videos_table WHERE isDownloaded = 1)")
    int getTotalDownloadedCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStats(HomeDashboardEntity stats);

    @Query("SELECT * FROM dashboard_stats WHERE id = 1")
    LiveData<HomeDashboardEntity> getDashboardStatsLive();

    @Query("SELECT * FROM dashboard_stats WHERE id = 1 LIMIT 1")
    HomeDashboardEntity getDashboardStatsSync();
}