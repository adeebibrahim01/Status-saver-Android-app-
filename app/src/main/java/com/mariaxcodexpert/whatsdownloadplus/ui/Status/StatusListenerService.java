package com.mariaxcodexpert.whatsdownloadplus.ui.Status;

import android.app.Service;
import android.content.Intent;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.room.Room;

import com.mariaxcodexpert.whatsdownloadplus.data.AppDatabase;
import com.mariaxcodexpert.whatsdownloadplus.data.ContactEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.StatusDao;
import com.mariaxcodexpert.whatsdownloadplus.data.StatusEntity;

import java.io.File;
import java.util.List;

public class StatusListenerService extends Service {

    private static final String TAG = "StatusListener";
    private static final String STATUS_PATH = "/storage/emulated/0/WhatsApp/Media/.Statuses/";
    private StatusDao statusDao;
    private FileObserver observer;

    @Override
    public void onCreate() {
        super.onCreate();

        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "statusDB")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();
        statusDao = db.statusDao();

        observer = new FileObserver(STATUS_PATH, FileObserver.CREATE | FileObserver.MOVED_TO) {
            @Override
            public void onEvent(int event, @Nullable String fileName) {
                if (fileName != null) {
                    String contactName = extractContactNameFromFile(fileName);
                    long timestamp = System.currentTimeMillis();

                    // Add or get contact
                    ContactEntity contact = statusDao.getContactByPhone(contactName);
                    if (contact == null) {
                        contact = new ContactEntity();
                        contact.name = contactName;
                        contact.phone = contactName;
                        long id = statusDao.insertContact(contact);
                        contact.id = (int) id;
                    }

                    // Add status
                    StatusEntity status = new StatusEntity();
                    status.contactId = contact.id;
                    status.type = fileName.endsWith(".mp4") ? "Video" : "Image";
                    status.timestamp = timestamp;
                    statusDao.insertStatus(status);

                    Log.d(TAG, "Status added for: " + contactName + " -> " + status.type);
                }
            }
        };

        // Delay start to ensure folder exists
        new Handler().postDelayed(observer::startWatching, 1000);
    }

    private String extractContactNameFromFile(String fileName) {
        // Example filename: "IMG-20251115-WA0001.jpg"
        // Look for matching notification timestamp or contact mapping
        // Step 1: Check Room DB for recent statuses within last 10 minutes
        long now = System.currentTimeMillis();
        List<ContactEntity> contacts = statusDao.getAllContacts();

        for (ContactEntity contact : contacts) {
            List<StatusEntity> recentStatuses = statusDao.getStatusHistory(contact.id);
            for (StatusEntity s : recentStatuses) {
                long diff = Math.abs(s.timestamp - now);
                if (diff < 10 * 60 * 1000) { // 10 minutes window
                    return contact.name;      // Use matched contact
                }
            }
        }

        // Step 2: fallback: parse filename
        if (fileName.contains("-WA")) {
            return fileName.split("-")[0]; // naive fallback
        }

        return "Unknown";
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (observer != null) observer.stopWatching();
    }
}
