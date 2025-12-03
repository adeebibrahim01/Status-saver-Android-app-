package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;

import com.mariaxcodexpert.whatsdownloadplus.ui.Notifications10below.NotificationDatabaseHelper10below;

public class WhatsAccessibilityService extends AccessibilityService {

    private NotificationDatabaseHelper10below dbHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new NotificationDatabaseHelper10below(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) return;

        if (event.getParcelableData() instanceof Notification) {
            Notification notif = (Notification) event.getParcelableData();
            if (notif == null || notif.extras == null) return;

            Bundle extras = notif.extras;
            CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);

            if (lines != null && lines.length > 0) {
                // Multiple lines (stacked messages)
                for (CharSequence line : lines) {
                    if (line == null) continue;
                    processLine(extras, line.toString());
                }
            } else {
                // Single message
                CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
                if (text != null) {
                    processLine(extras, text.toString());
                }
            }
        }
    }

    private void processLine(Bundle extras, String message) {
        if (TextUtils.isEmpty(message)) return;

        String sender = extras.getString(Notification.EXTRA_TITLE);
        if (message.contains(": ")) {
            int index = message.indexOf(": ");
            sender = message.substring(0, index);
            message = message.substring(index + 2);
        }

        long timestamp = System.currentTimeMillis();
        String chatId = getPackageName() + "_" + sender;

        // ✅ Prevent duplicate insert
        if (!dbHelper.isMessageAlreadySaved(chatId, message)) {
            dbHelper.insertNotificationWithChatId(sender, chatId, message, timestamp);
        }

        // Send broadcast to update UI
        Intent intent = new Intent("com.mariaxcodexpert.NEW_NOTIFICATION");
        intent.putExtra("sender", sender);
        intent.putExtra("chatId", chatId);
        intent.putExtra("message", message);
        intent.putExtra("timestamp", timestamp);
        sendBroadcast(intent);
    }

    @Override
    public void onInterrupt() { }
}
