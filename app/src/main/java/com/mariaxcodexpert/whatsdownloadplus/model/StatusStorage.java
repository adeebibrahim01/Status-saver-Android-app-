package com.mariaxcodexpert.whatsdownloadplus.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import androidx.annotation.Keep;
import com.google.firebase.auth.FirebaseAuth; // Firebase Auth import
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

@Keep
public class StatusStorage {

    public static void handleFirebaseSync(Context context, int statusId, long expiryTime, boolean isDownloaded) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String identity = (user != null) ? user.getUid() :
                Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {

            String dbUrl = "https://status-saver-92d48-default-rtdb.firebaseio.com/";
            DatabaseReference dbRef = FirebaseDatabase.getInstance(dbUrl)
                    .getReference("StatusAlerts")
                    .child(identity)
                    .child(String.valueOf(statusId));

            if (isDownloaded) {
                dbRef.removeValue();
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm:ss a", Locale.ENGLISH);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String formattedExpiry = sdf.format(new Date(expiryTime));

                StatusExpiryModel data = new StatusExpiryModel(
                        token,
                        identity,
                        statusId,
                        formattedExpiry,
                        false
                );

                dbRef.setValue(data);
            }
        });
    }
}