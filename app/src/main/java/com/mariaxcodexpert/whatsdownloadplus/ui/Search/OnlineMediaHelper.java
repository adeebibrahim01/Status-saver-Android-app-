package com.mariaxcodexpert.whatsdownloadplus.ui.Search;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import com.mariaxcodexpert.whatsdownloadplus.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase;

public class OnlineMediaHelper {

    public static void showAdBeforeAction(Activity activity, Runnable action) {
        if (activity != null && AdManager.isInterstitialLoaded()) {
            AdManager.showInterstitial(activity, new AdManager.AdCallback() {
                @Override public void onAdClosed() { action.run(); }
                @Override public void onAdFailed() { action.run(); }
            });
        } else {
            action.run();
        }
    }

    public static String generateFileName(String url, boolean isVideo) {
        return "Pexels_" + Math.abs(url.hashCode()) + (isVideo ? ".mp4" : ".jpg");
    }

    // 🔥 Centralized Database Check
    public static boolean checkIfDownloaded(Context context, MediaItem item) {
        AppDatabase db = AppDatabase.getInstance(context);
        String url = item.isVideo() ? item.getVideoUrl() : item.getUrl();
        String fileName = generateFileName(url, item.isVideo());
        return db.imageDao().isImageExists(fileName) || db.videoDao().isVideoExists(fileName);
    }

    // 🔥 Centralized Sharing Logic
    public static void shareMedia(Context context, MediaItem item) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(item.isVideo() ? "video/*" : "image/*");
            String shareUrl = item.isVideo() ? item.getVideoUrl() : item.getUrl();
            intent.putExtra(Intent.EXTRA_TEXT, "Shared via WhatsDownload+\n" + shareUrl);
            context.startActivity(Intent.createChooser(intent, "Share via"));
        } catch (Exception e) {
            showPremiumToast(context, "Share failed");
        }
    }

    public static void showPremiumToast(Context context, String message) {
        if (context != null) {
            Toast.makeText(context, "✧ " + message + " ✧", Toast.LENGTH_SHORT).show();
        }
    }
}