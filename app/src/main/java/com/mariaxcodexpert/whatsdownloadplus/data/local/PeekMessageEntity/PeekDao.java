package com.mariaxcodexpert.whatsdownloadplus.data.local.PeekMessageEntity;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PeekDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(PeekMessageEntity message);

    @Query("SELECT * FROM peek_messages WHERE userId = :uid GROUP BY senderName HAVING createdAt = MAX(createdAt) ORDER BY createdAt DESC")
    List<PeekMessageEntity> getDashboardMessages(String uid);

    @Query("SELECT SUM(unreadCount) FROM peek_messages WHERE senderName = :sender AND userId = :uid")
    int getSumUnreadCount(String sender, String uid);

    @Query("UPDATE peek_messages SET unreadCount = 0 WHERE senderName = :sender AND userId = :uid")
    void resetUnreadCount(String sender, String uid);

    @Query("SELECT * FROM peek_messages WHERE senderName = :sender AND userId = :uid ORDER BY createdAt ASC")
    List<PeekMessageEntity> getChatHistory(String sender, String uid);

    @Query("DELETE FROM peek_messages WHERE senderName = :sender AND userId = :uid")
    void deleteBySender(String sender, String uid);
}