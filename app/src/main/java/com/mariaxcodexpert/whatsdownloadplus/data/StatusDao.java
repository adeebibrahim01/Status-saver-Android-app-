package com.mariaxcodexpert.whatsdownloadplus.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface StatusDao {

    @Insert
    long insertContact(ContactEntity contact);  // returns generated ID

    @Insert
    void insertStatus(StatusEntity status);

    @Query("SELECT * FROM contacts")
    List<ContactEntity> getAllContacts();

    @Query("SELECT * FROM status_history WHERE contactId = :contactId ORDER BY timestamp DESC")
    List<StatusEntity> getStatusHistory(int contactId);

    @Query("SELECT * FROM contacts WHERE phone = :phone LIMIT 1")
    ContactEntity getContactByPhone(String phone);
}
