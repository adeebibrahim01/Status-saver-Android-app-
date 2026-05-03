package com.mariaxcodexpert.whatsdownloadplus;

import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCM_Service";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        // Jab Firebase Console se message aaye
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();

            PushNotificationHelper helper = new PushNotificationHelper(this);
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // Time-based ID taake multiple notifications mix na hon
            int id = (int) System.currentTimeMillis();
            helper.sendNotification(title, body, intent, id);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New Token Generated: " + token);
    }
}