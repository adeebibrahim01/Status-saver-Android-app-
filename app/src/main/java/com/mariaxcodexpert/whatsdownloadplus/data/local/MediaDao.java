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

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertDashboardStats(HomeDashboardEntity stats);

    @Transaction
    public void updateDashboardStats(HomeDashboardEntity stats) {
        insertDashboardStats(stats);
    }

    }