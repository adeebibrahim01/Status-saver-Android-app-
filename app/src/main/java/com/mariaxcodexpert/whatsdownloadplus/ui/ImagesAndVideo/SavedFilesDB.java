package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

public class SavedFilesDB {

    private final DBHelper dbHelper;
    private final Set<String> cache = new HashSet<>();
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SavedFilesDB(Context context) {
        this.context = context.getApplicationContext();
        dbHelper = new DBHelper(this.context);
        loadCache(); // safe load, never crashes
        registerContentObservers();
    }

    /** Helper to safely show Toasts on main thread */
    private void showToast(final String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    /** Load cache safely, ignore if table missing */
    private void loadCache() {
        cache.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(DBHelper.TABLE_NAME, new String[]{DBHelper.COLUMN_NAME},
                    null, null, null, null, null);

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String fileName = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_NAME));
                    cache.add(fileName);

                    // 🔹 Toast for fetch
                    showToast("Fetched from DB: " + fileName);
                }
            }
        } catch (android.database.sqlite.SQLiteException e) {
            showToast("Table missing: " + DBHelper.TABLE_NAME);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /** Add file safely */
    public void addFile(String fileName) {
        if (fileName == null || cache.contains(fileName)) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(DBHelper.COLUMN_NAME, fileName);
            db.insertWithOnConflict(DBHelper.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            cache.add(fileName);

            showToast("Added to DB: " + fileName + " | Table: " + DBHelper.TABLE_NAME);

        } catch (android.database.sqlite.SQLiteException e) {
            showToast("Failed to add: " + fileName);
        }
    }

    /** Remove file safely */
    public void removeFile(String fileName) {
        if (fileName == null || !cache.contains(fileName)) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.delete(DBHelper.TABLE_NAME, DBHelper.COLUMN_NAME + "=?", new String[]{fileName});
            cache.remove(fileName);

            showToast("Deleted from DB: " + fileName + " | Table: " + DBHelper.TABLE_NAME);

        } catch (android.database.sqlite.SQLiteException e) {
            showToast("Failed to delete: " + fileName);
        }
    }

    /** Check if file exists in cache */
    public boolean isFileSaved(String fileName) {
        return fileName != null && cache.contains(fileName);
    }

    /** Return all saved files safely */
    public Set<String> getAllSavedFiles() {
        return new HashSet<>(cache);
    }

    private void registerContentObservers() {
        Uri[] uris = {MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.EXTERNAL_CONTENT_URI};

        for (Uri uri : uris) {
            context.getContentResolver().registerContentObserver(uri, true, new android.database.ContentObserver(mainHandler) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    checkDeletedFiles();
                }
            });
        }
    }

    private void checkDeletedFiles() {
        Set<String> toRemove = new HashSet<>();
        for (String fileName : cache) {
            if (!isFileExistsInMediaStore(fileName)) toRemove.add(fileName);
        }
        for (String fileName : toRemove) removeFile(fileName);
    }

    private boolean isFileExistsInMediaStore(String fileName) {
        String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
        Uri[] uris = {MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.EXTERNAL_CONTENT_URI};

        for (Uri contentUri : uris) {
            try (Cursor cursor = context.getContentResolver().query(
                    contentUri,
                    projection,
                    MediaStore.MediaColumns.DISPLAY_NAME + "=?",
                    new String[]{fileName},
                    null
            )) {
                if (cursor != null && cursor.moveToFirst()) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    /** SQLiteOpenHelper */
    private static class DBHelper extends SQLiteOpenHelper {
        private static final String DB_NAME = "downloads.db";
        private static final int DB_VERSION = 1;

        private static final String TABLE_NAME = "saved_files";
        private static final String COLUMN_NAME = "file_name";

        private final Context context;

        public DBHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
            this.context = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
                        " (" + COLUMN_NAME + " TEXT PRIMARY KEY)");
            } catch (android.database.sqlite.SQLiteException ignored) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "DB create failed: " + TABLE_NAME, Toast.LENGTH_SHORT).show()
                );
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            try {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
                onCreate(db);
            } catch (android.database.sqlite.SQLiteException ignored) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "DB upgrade failed: " + TABLE_NAME, Toast.LENGTH_SHORT).show()
                );
            }
        }
    }
}
