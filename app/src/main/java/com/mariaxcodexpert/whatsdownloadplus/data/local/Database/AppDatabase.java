package com.mariaxcodexpert.whatsdownloadplus.data.local.Database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

// Entities
import com.mariaxcodexpert.whatsdownloadplus.data.local.HomeDashboardEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.PeekMessageEntity.PeekDao;
import com.mariaxcodexpert.whatsdownloadplus.data.local.PeekMessageEntity.PeekMessageEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ProfileEntity.ProfileDao;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ProfileEntity.ProfileEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.MediaEntity;

// DAOs
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageDao;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoDao;
import com.mariaxcodexpert.whatsdownloadplus.data.local.Dashboard.DashboardDao;
import com.mariaxcodexpert.whatsdownloadplus.data.local.MediaDao;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {
        ImageEntity.class,
        VideoEntity.class,
        HomeDashboardEntity.class,
        MediaEntity.class,
        ProfileEntity.class,
        PeekMessageEntity.class
}, version = 17, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ImageDao imageDao();
    public abstract VideoDao videoDao();
    public abstract DashboardDao dashboardDao();
    public abstract MediaDao mediaDao();
    public abstract ProfileDao profileDao();
    public abstract PeekDao peekDao();



    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;

    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "media_db")
                            .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                            .fallbackToDestructiveMigration()
                            .setQueryExecutor(databaseWriteExecutor)
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            db.enableWriteAheadLogging();
        }
    };
}