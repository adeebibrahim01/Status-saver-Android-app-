package com.mariaxcodexpert.whatsdownloadplus.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "dashboard_stats")
public class HomeDashboardEntity {

    @PrimaryKey
    public int id;

    public int todayCount;
    public int totalCount;
    public int activeStatuses;
    public String joinedDate;


    public HomeDashboardEntity(int id, int todayCount, int totalCount, int activeStatuses, String joinedDate) {
        this.id = id;
        this.todayCount = todayCount;
        this.totalCount = totalCount;
        this.activeStatuses = activeStatuses;
        this.joinedDate = joinedDate;
    }
}