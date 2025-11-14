package com.mariaxcodexpert.whatsdownloadplus.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {ContactEntity.class, StatusEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract StatusDao statusDao();
}
