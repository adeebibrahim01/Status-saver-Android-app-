package com.mariaxcodexpert.whatsdownloadplus.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class StatusStorage {

    public static void handleFirebaseSync(Context context, int statusId, long expiryTime, boolean isDownloaded) {

        // 1. Persistent Device Identity
        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {

            String dbUrl = "https://status-saver-92d48-default-rtdb.firebaseio.com/";
            DatabaseReference dbRef = FirebaseDatabase.getInstance(dbUrl)
                    .getReference("StatusAlerts")
                    .child(deviceId)
                    .child(String.valueOf(statusId));

            if (isDownloaded) {
                // Download ho gaya toh cleaning
                dbRef.removeValue();
            } else {
                // 2. Global UTC Time Formatting
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm:ss a", Locale.ENGLISH);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String formattedExpiry = sdf.format(new Date(expiryTime));

                // 3. Using Model instead of HashMap for cleaner code
                StatusExpiryModel data = new StatusExpiryModel(
                        token,
                        deviceId,
                        statusId,
                        formattedExpiry,
                        false
                );

                dbRef.setValue(data);
            }
        });
    }
}