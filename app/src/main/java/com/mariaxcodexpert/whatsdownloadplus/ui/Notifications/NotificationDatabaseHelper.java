package com.mariaxcodexpert.whatsdownloadplus.ui.Notifications;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.widget.Toast;

public class NotificationDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "notifications.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_NAME = "whatsapp_notifications";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_SENDER = "sender";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    private final Context context;

    public NotificationDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SENDER + " TEXT, " +
                COLUMN_MESSAGE + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER" +
                ");");
        Toast.makeText(context, "Database created: " + TABLE_NAME, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
        Toast.makeText(context, "Database upgraded: " + oldVersion + " -> " + newVersion, Toast.LENGTH_SHORT).show();
    }

    /**
     * Save a new WhatsApp notification
     * Ignores duplicates and system/call messages
     */
    public void insertNotification(String sender, String message, long timestamp) {
        if (sender == null || message == null) {
            Toast.makeText(context, "Insert failed: sender or message is null", Toast.LENGTH_SHORT).show();
            return;
        }

        // Temporarily disable ignore filter for Android 10 below
        boolean skipIgnore = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q;

//        if (!skipIgnore && NotificationsFragment.shouldIgnoreNotification(message)) {
//            Toast.makeText(context, "Ignored system/call message: " + message, Toast.LENGTH_SHORT).show();
//            return;
//        }

        SQLiteDatabase db = getWritableDatabase();

        // Check duplicates
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_NAME +
                        " WHERE " + COLUMN_SENDER + "=? AND " + COLUMN_MESSAGE + "=? AND ABS(" + COLUMN_TIMESTAMP + "-?) < 1000",
                new String[]{sender, message, String.valueOf(timestamp)}
        );

        boolean exists = false;
        if (cursor.moveToFirst()) exists = cursor.getInt(0) > 0;
        cursor.close();

        if (!exists) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_SENDER, sender);
            values.put(COLUMN_MESSAGE, message);
            values.put(COLUMN_TIMESTAMP, timestamp);
            long rowId = db.insert(TABLE_NAME, null, values);

            if (rowId != -1) {
                Toast.makeText(context, "Inserted: " + sender + " -> " + message, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Insert failed for: " + sender + " -> " + message, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "Duplicate ignored: " + sender + " -> " + message, Toast.LENGTH_SHORT).show();
        }

        db.close();
    }

    /**
     * Retrieve all notifications, newest first
     */
    public Cursor getAllNotifications() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_TIMESTAMP + " DESC", null);
        Toast.makeText(context, "Loaded notifications: " + (cursor != null ? cursor.getCount() : 0), Toast.LENGTH_SHORT).show();
        return cursor;
    }

    /**
     * Clear all notifications
     */
    public void clearAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
        db.close();
        Toast.makeText(context, "All notifications cleared", Toast.LENGTH_SHORT).show();
    }

    /**
     * Delete all notifications from a specific sender
     *
     * @param sender The sender whose notifications should be removed
     * @return number of rows deleted
     */
    public int deleteNotificationBySender(String sender) {
        if (sender == null || sender.trim().isEmpty()) {
            Toast.makeText(context, "Delete failed: sender is null or empty", Toast.LENGTH_SHORT).show();
            return 0;
        }

        SQLiteDatabase db = getWritableDatabase();
        int deletedRows = db.delete(
                TABLE_NAME,
                COLUMN_SENDER + " = ?",
                new String[]{sender}
        );
        db.close();
        Toast.makeText(context, "Deleted " + deletedRows + " notifications for sender: " + sender, Toast.LENGTH_SHORT).show();
        return deletedRows;
    }

    /**
     * Delete notification by its ID
     *
     * @param id Notification ID
     * @return true if deleted successfully
     */
    public boolean deleteNotificationById(long id) {
        SQLiteDatabase db = getWritableDatabase();
        int deletedRows = db.delete(TABLE_NAME, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        Toast.makeText(context, "Deleted notification ID " + id + ": " + (deletedRows > 0 ? "success" : "fail"), Toast.LENGTH_SHORT).show();
        return deletedRows > 0;
    }
}
