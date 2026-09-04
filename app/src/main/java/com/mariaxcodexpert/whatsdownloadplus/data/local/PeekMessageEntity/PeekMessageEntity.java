package com.mariaxcodexpert.whatsdownloadplus.data.local.PeekMessageEntity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "peek_messages")
public class PeekMessageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String senderName;
    public String messageBody;
    public String timestamp;
    public int unreadCount = 0;
    public String userId;
    public long createdAt;
    @Ignore
    public int totalUnreadSum = 0;
}