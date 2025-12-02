package com.mariaxcodexpert.whatsdownloadplus.Permissions;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import com.mariaxcodexpert.whatsdownloadplus.ui.Notifications.NotificationDatabaseHelper;

import java.util.List;

public class WhatsAccessibilityService extends AccessibilityService {

    private static final String TAG = "WhatsAccessibilityService";

    // Log + single toast per notification
    private void logAndToast(String msg) {
        Log.d(TAG, msg);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        // Only for Android 10 below
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            logAndToast("Android 10+ detected, ignoring event");
            return;
        }

        if (event.getEventType() != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            logAndToast("Event type not notification: " + event.getEventType());
            return;
        }

        String packageName = String.valueOf(event.getPackageName());

        // Accept only WhatsApp packages
        if (!packageName.toLowerCase().contains("whatsapp")) {
            logAndToast("Not WhatsApp, ignoring.");
            return;
        }

        String sender = null;
        String message = null;

        // Try extracting from notification extras
        if (event.getParcelableData() instanceof Notification) {
            Notification notification = (Notification) event.getParcelableData();
            Bundle extras = notification.extras;
            if (extras != null) {
                sender = extras.getString("android.title");

                CharSequence text = extras.getCharSequence("android.text");
                if (text == null) text = extras.getCharSequence("android.bigText");

                // Check android.textLines
                if (text == null) {
                    CharSequence[] lines = extras.getCharSequenceArray("android.textLines");
                    if (lines != null && lines.length > 0)
                        text = lines[lines.length - 1];
                }

                if (text != null) message = text.toString();
            }
        }

        // Fallback: event.getText()
        if (message == null || message.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            List<CharSequence> texts = event.getText();
            for (CharSequence s : texts) sb.append(s).append(" ");
            message = sb.toString().trim();
        }

        if (sender == null || sender.isEmpty()) sender = "WhatsApp";

        long timestamp = System.currentTimeMillis();

        // Prepare single debug message
        String debugMsg = "Notif received: \nPackage: " + packageName +
                "\nSender: " + sender +
                "\nMessage: " + message +
                "\nTimestamp: " + timestamp;

        logAndToast(debugMsg);  // Log only; show single toast
        Toast.makeText(getApplicationContext(), "WhatsApp notif: " + message, Toast.LENGTH_SHORT).show();

        // Insert into database
        NotificationDatabaseHelper dbHelper = new NotificationDatabaseHelper(getApplicationContext());
        dbHelper.insertNotification(sender, message, timestamp);

        // Send broadcast
        Intent intent = new Intent("com.mariaxcodexpert.NEW_NOTIFICATION");
        intent.putExtra("sender", sender);
        intent.putExtra("message", message);
        intent.putExtra("timestamp", timestamp);
        sendBroadcast(intent);
    }

    @Override
    public void onInterrupt() {
        logAndToast("Accessibility interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        logAndToast("Accessibility service connected and active for Android 10 below");
    }
}
