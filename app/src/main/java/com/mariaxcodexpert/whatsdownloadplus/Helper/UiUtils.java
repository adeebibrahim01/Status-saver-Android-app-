package com.mariaxcodexpert.whatsdownloadplus.Helper;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.mariaxcodexpert.whatsdownloadplus.R;

public class UiUtils {


    public static void openWhatsApp(Context context) {
        Toast.makeText(context, context.getString(R.string.toast_view_statuses_first), Toast.LENGTH_LONG).show(); // 🔥 Changed

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.whatsapp");
                if (intent != null) {
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, context.getString(R.string.toast_whatsapp_not_found), Toast.LENGTH_SHORT).show(); // 🔥 Changed
                }
            } catch (Exception e) {
                android.util.Log.e("SyncManager", "Error during background sync: " + e.getMessage(), e);
            }
        }, 1000);
    }
}