package com.mariaxcodexpert.whatsdownloadplus.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "status_history")
public class StatusEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int contactId;
    public String type; // "image", "video", "text", "emoji"
    public long timestamp;
}
