package com.mariaxcodexpert.whatsdownloadplus.ui.Notifications;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NotificationDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "notifications.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_NAME = "whatsapp_notifications";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_SENDER = "sender";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    public NotificationDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SENDER + " TEXT, " +
                COLUMN_MESSAGE + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER" +
                ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    /**
     * Save a new WhatsApp notification
     * Ignores duplicates and system/call messages
     */
    public void insertNotification(String sender, String message, long timestamp) {
        if (sender == null || message == null) return;

        // Ignore system/call messages
        if (NotificationsFragment.shouldIgnoreNotification(message)) return;

        SQLiteDatabase db = getWritableDatabase();

        // Check for duplicates: same sender & message within 1 second
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
            db.insert(TABLE_NAME, null, values);
        }

        db.close();
    }


    /**
     * Retrieve all notifications, newest first
     */
    public Cursor getAllNotifications() {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_TIMESTAMP + " DESC", null);
    }

    /**
     * Clear all notifications
     */
    public void clearAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
        db.close();
    }

    /**
     * Delete all notifications from a specific sender
     *
     * @param sender The sender whose notifications should be removed
     * @return number of rows deleted
     */
    public int deleteNotificationBySender(String sender) {
        if (sender == null || sender.trim().isEmpty()) return 0;

        SQLiteDatabase db = getWritableDatabase();
        int deletedRows = db.delete(
                TABLE_NAME,
                COLUMN_SENDER + " = ?",
                new String[]{sender}
        );
        db.close();
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
        return deletedRows > 0;
    }
}
