package com.mariaxcodexpert.whatsdownloadplus.ui.Notifications10below;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;

public class WhatsNotificationService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) return;

        if (event.getParcelableData() instanceof Notification) {
            Notification notif = (Notification) event.getParcelableData();
            if (notif == null || notif.extras == null) return;

            Bundle extras = notif.extras;

            // Check for multiple lines
            CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if (lines != null && lines.length > 0) {
                String sender = extras.getString(Notification.EXTRA_TITLE); // fallback
                for (CharSequence line : lines) {
                    if (line == null) continue;
                    String msg = line.toString();
                    if (msg.contains(": ")) {
                        int index = msg.indexOf(": ");
                        sender = msg.substring(0, index);
                        msg = msg.substring(index + 2);
                    }

                    sendMessageBroadcast(sender, msg);
                }
            } else {
                // Fallback to single text
                CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
                String message = text != null ? text.toString() : "";
                if (TextUtils.isEmpty(message)) return;

                String sender = extras.getString(Notification.EXTRA_TITLE);
                if (message.contains(": ")) {
                    int index = message.indexOf(": ");
                    sender = message.substring(0, index);
                    message = message.substring(index + 2);
                }
                sendMessageBroadcast(sender, message);
            }
        }
    }

    private void sendMessageBroadcast(String sender, String message) {
        if (TextUtils.isEmpty(sender) || TextUtils.isEmpty(message)) return;

        String chatId = getPackageName() + "_" + sender;
        long timestamp = System.currentTimeMillis();

        Intent intent = new Intent("com.mariaxcodexpert.NEW_NOTIFICATION");
        intent.putExtra("sender", sender);
        intent.putExtra("chatId", chatId);
        intent.putExtra("message", message);
        intent.putExtra("timestamp", timestamp);
        sendBroadcast(intent);
    }

    @Override
    public void onInterrupt() {
        // Required
    }
}
