package com.mariaxcodexpert.whatsdownloadplus.model;

import android.content.Context;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.HashMap;

public class StatusStorage {

    public static void handleFirebaseSync(Context context, int statusId, long expiryTime, boolean isDownloaded) {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {

            // 🔥 Yahan humne manually URL de dia hai jo json mein missing tha
            String dbUrl = "https://status-saver-92d48-default-rtdb.firebaseio.com/";

            DatabaseReference dbRef = FirebaseDatabase.getInstance(dbUrl)
                    .getReference("StatusAlerts")
                    .child(token)
                    .child(String.valueOf(statusId));

            if (isDownloaded) {
                dbRef.removeValue();
            } else {
                StatusExpiryModel model = new StatusExpiryModel(token, statusId, expiryTime);
                dbRef.setValue(model);
            }
        });
    }

//    public static void handleFirebaseSync(Context context, int statusId, long expiryTime, boolean isDownloaded) {
//        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
//
//            // Database path: StatusAlerts -> [UserToken] -> [StatusId]
//            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("StatusAlerts")
//                    .child(token)
//                    .child(String.valueOf(statusId));
//
//            if (isDownloaded) {
//                // Requirement: Agar download ho gaya toh Firebase se foran delete karein
//                dbRef.removeValue();
//            } else {
//                // Requirement: Agar view kiya hai toh entry banayein
//                // Same package mein hone ki wajah se direct use ho raha hai
//                StatusExpiryModel model = new StatusExpiryModel(token, statusId, expiryTime);
//                dbRef.setValue(model);
//            }
//        });
//    }
}