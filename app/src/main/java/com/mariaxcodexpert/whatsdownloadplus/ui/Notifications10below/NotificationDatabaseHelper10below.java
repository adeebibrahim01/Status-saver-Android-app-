package com.mariaxcodexpert.whatsdownloadplus.ui.Notifications10below;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

public class NotificationDatabaseHelper10below extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "notifications.db";
    private static final int DATABASE_VERSION = 3;

    public static final String TABLE_NAME = "whatsapp_notifications";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_SENDER = "sender";
    public static final String COLUMN_CHAT_ID = "chatId";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    private final Context context;

    public NotificationDatabaseHelper10below(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SENDER + " TEXT, " +
                COLUMN_CHAT_ID + " TEXT, " +
                COLUMN_MESSAGE + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER" +
                ");");
        //    Toast.makeText(context, "Database created: " + TABLE_NAME, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_CHAT_ID + " TEXT;");
        }
       // Toast.makeText(context, "Database upgraded: " + oldVersion + " -> " + newVersion, Toast.LENGTH_SHORT).show();
    }

    /**
     * Insert notification (old method, kept for compatibility)
     */
    public void insertNotification(String sender, String message, long timestamp) {
        insertNotificationWithChatId(sender, sender, message, timestamp);
    }

    /**
     * Insert notification with chatId (new method)
     * ✅ Fixed: No grouping, every message is saved individually
     */
    public long insertNotificationWithChatId(String sender, String chatId, String message, long timestamp) {
        if (sender == null || message == null) {
         //   Toast.makeText(context, "Insert failed: sender or message is null", Toast.LENGTH_SHORT).show();
            return -1;
        }

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SENDER, sender);
        values.put(COLUMN_CHAT_ID, chatId != null ? chatId : sender);
        values.put(COLUMN_MESSAGE, message);
        values.put(COLUMN_TIMESTAMP, timestamp);

        long rowId = db.insert(TABLE_NAME, null, values);
        db.close();

        if (rowId != -1) {
            // Optional: Remove Toasts if too spammy
           // Toast.makeText(context, "Inserted: " + sender + " -> " + message, Toast.LENGTH_SHORT).show();
        } else {
           // Toast.makeText(context, "Insert failed: " + sender + " -> " + message, Toast.LENGTH_SHORT).show();
        }

        return rowId;
    }

    /**
     * Retrieve all notifications, newest first
     */
    public Cursor getAllNotifications() {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_TIMESTAMP + " DESC",
                null
        );
    }

    /**
     * Delete notification by ID
     */
    public boolean deleteNotificationById(long id) {
        SQLiteDatabase db = getWritableDatabase();
        int deletedRows = db.delete(TABLE_NAME, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return deletedRows > 0;
    }

    /**
     * Delete all notifications from a specific sender
     */
    public int deleteNotificationBySender(String sender) {
        if (sender == null || sender.trim().isEmpty()) return 0;
        SQLiteDatabase db = getWritableDatabase();
        int deletedRows = db.delete(TABLE_NAME, COLUMN_SENDER + "=?", new String[]{sender});
        db.close();
        return deletedRows;
    }

    /**
     * Clear all notifications
     */
    public void clearAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
        db.close();
    }

    public boolean isMessageAlreadySaved(String chatId, String message) {
        if (chatId == null || message == null) return false;

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        boolean exists = false;

        try {
            cursor = db.query(
                    TABLE_NAME,
                    new String[]{COLUMN_ID},
                    COLUMN_CHAT_ID + "=? AND " + COLUMN_MESSAGE + "=?",
                    new String[]{chatId, message},
                    null,
                    null,
                    null
            );

            exists = cursor != null && cursor.moveToFirst();
        } catch (Exception e) {
            e.printStackTrace();
            exists = false;
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        return exists;
    }

}
