package com.mariaxcodexpert.whatsdownloadplus.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "contacts")
public class ContactEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String phone;
    public String profilePicPath;
}
