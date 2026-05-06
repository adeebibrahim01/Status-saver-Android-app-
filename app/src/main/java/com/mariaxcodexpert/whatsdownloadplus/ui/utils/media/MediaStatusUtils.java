package com.mariaxcodexpert.whatsdownloadplus.ui.utils.media;

import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import com.mariaxcodexpert.whatsdownloadplus.PushNotificationHelper;
import com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity;

import java.io.*;
import java.util.concurrent.*;

@OptIn(markerClass = UnstableApi.class)
public class MediaStatusUtils {

    private static final String SAVE_FOLDER = "Status Saver";
    public static final ExecutorService executor = Executors.newFixedThreadPool(4);

    public interface SaveCallback {
        void onSaveResult(Boolean success, Uri savedUri);
    }

    /**
     * 🔥 NEW PUBLIC METHOD: Direct database mein record save karne ke liye (Trending Fragment ke liye)
     */
    public static void saveToDatabase(Context ctx, Uri savedUri, String name, boolean isVid) {
        executor.execute(() -> {
            // Trending items ke liye hum savedUri ko hi source aur path dono consider karte hain
            syncToSpecificTable(ctx, savedUri.toString(), savedUri.toString(), name, isVid);
        });
    }

    /**
     * Professional Save Logic: Gallery mein save karta hai aur Content Uri ko DB mein store karta hai.
     */
    public static void saveToGallery(Context context, Uri source, Bitmap bmp, String name, boolean isVid, int quality, SaveCallback cb) {
        executor.execute(() -> {
            boolean success = false;
            Uri savedUri = null;
            ContentResolver resolver = context.getContentResolver();

            try {
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                v.put(MediaStore.MediaColumns.MIME_TYPE, isVid ? "video/mp4" : "image/jpeg");

                // Android 10+ scoped storage path configuration
                String path = (isVid ? Environment.DIRECTORY_MOVIES : Environment.DIRECTORY_PICTURES) + "/" + SAVE_FOLDER;
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, path);
                v.put(MediaStore.MediaColumns.IS_PENDING, 1);

                Uri collection = isVid ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                savedUri = resolver.insert(collection, v);

                if (savedUri != null) {
                    try (OutputStream os = resolver.openOutputStream(savedUri)) {
                        if (!isVid && bmp != null) {
                            success = bmp.compress(Bitmap.CompressFormat.JPEG, quality, os);
                        } else {
                            try (InputStream is = resolver.openInputStream(source)) {
                                if (is != null) {
                                    byte[] buf = new byte[8192];
                                    int len;
                                    while ((len = is.read(buf)) != -1) os.write(buf, 0, len);
                                    success = true;
                                }
                            }
                        }
                    }

                    // IS_PENDING ko 0 kerna zaroori hai taake file gallery mein visible ho
                    v.clear();
                    v.put(MediaStore.MediaColumns.IS_PENDING, 0);
                    resolver.update(savedUri, v, null, null);

                    if (success) {
                        // 🔥 FIX: Physical path ke bajaye Content Uri String save kar rahe hain (Best for Android 11+)
                        String uriString = savedUri.toString();
                        syncToSpecificTable(context, source.toString(), uriString, name, isVid);
                    }
                }
            } catch (Exception e) {
                Log.e("MediaStatusUtils", "Save Error: " + e.getMessage());
            }

            final boolean finalSuccess = success;
            final Uri finalUri = savedUri;

            // UI Thread par result return karna
            new Handler(Looper.getMainLooper()).post(() -> {
                if (finalSuccess) {
                    notifyUser(context, name); // User ko "Saved to Gallery" ka toast/notification dena
                }
                cb.onSaveResult(finalSuccess, finalUri);
            });
        });
    }

    /**
     * Database sync logic: 7 din ki expiry aur isDownloaded status ke sath record save karta hai.
     */
    private static void syncToSpecificTable(Context ctx, String src, String savedUriPath, String name, boolean isVid) {
        try {
            AppDatabase db = AppDatabase.getInstance(ctx);
            long now = System.currentTimeMillis();

            // 7 din ki expiry calculation
            long sevenDaysInMs = 7L * 24 * 60 * 60 * 1000;
            long expiryAt = now + sevenDaysInMs;

            if (isVid) {
                VideoEntity video = new VideoEntity(
                        name,
                        src,
                        savedUriPath, // Database mein content://... save hoga
                        now,
                        true,
                        expiryAt
                );
                video.setDownloadTime(now);
                db.videoDao().insertVideo(video);
                Log.d("MediaStatusUtils", "Video saved with Content Uri: " + savedUriPath);
            } else {
                ImageEntity image = new ImageEntity(
                        name,
                        src,
                        savedUriPath, // Database mein content://... save hoga
                        now,
                        true,
                        expiryAt
                );
                image.setDownloadTime(now);
                db.imageDao().insertImage(image);
                Log.d("MediaStatusUtils", "Image saved with Content Uri: " + savedUriPath);
            }
        } catch (Exception e) {
            Log.e("MediaStatusUtils", "Table Sync Error: " + e.getMessage());
        }
    }

    public static void shareMedia(Context ctx, Uri uri, boolean isVid) {
        if (uri.toString().startsWith("http")) {
            Toast.makeText(ctx, "Preparing media... ✧", Toast.LENGTH_SHORT).show();

            new Thread(() -> {
                try {
                    // 1. Glide se asFile fetch karein
                    java.io.File originalFile = com.bumptech.glide.Glide.with(ctx)
                            .asFile()
                            .load(uri.toString())
                            .submit()
                            .get();

                    if (originalFile != null && originalFile.exists()) {
                        // 2. 🔥 Extension Fix: Bin file ko JPG/MP4 mein convert (copy) karein
                        String extension = isVid ? ".mp4" : ".jpg";
                        java.io.File shareFile = new java.io.File(ctx.getCacheDir(), "share_temp" + extension);

                        // File copy logic
                        try (java.io.FileInputStream in = new java.io.FileInputStream(originalFile);
                             java.io.FileOutputStream out = new java.io.FileOutputStream(shareFile)) {
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = in.read(buffer)) > 0) {
                                out.write(buffer, 0, length);
                            }
                        }

                        // 3. Nayi extension wali file ka URI banayein
                        Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                                ctx, ctx.getPackageName() + ".fileprovider", shareFile);

                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            Intent i = new Intent(Intent.ACTION_SEND)
                                    .setType(isVid ? "video/*" : "image/*")
                                    .putExtra(Intent.EXTRA_STREAM, contentUri)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            ctx.startActivity(Intent.createChooser(i, "Share via"));
                        });
                    }
                } catch (Exception e) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            Toast.makeText(ctx, "Share failed", Toast.LENGTH_SHORT).show());
                }
            }).start();
        } else {
            // Local files (already have extensions)
            Intent i = new Intent(Intent.ACTION_SEND)
                    .setType(isVid ? "video/*" : "image/*")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(Intent.createChooser(i, "Share via"));
        }
    }

    public static void repostMedia(Context ctx, Uri uri, boolean isVid) {
        if (uri.toString().startsWith("http")) {
            Toast.makeText(ctx, "Preparing for Repost... ✧", Toast.LENGTH_SHORT).show();

            new Thread(() -> {
                try {
                    // 1. Glide se asFile fetch karein
                    java.io.File originalFile = com.bumptech.glide.Glide.with(ctx)
                            .asFile()
                            .load(uri.toString())
                            .submit()
                            .get();

                    if (originalFile != null && originalFile.exists()) {
                        // 2. Extension Fix (Same as share)
                        String extension = isVid ? ".mp4" : ".jpg";
                        java.io.File repostFile = new java.io.File(ctx.getCacheDir(), "repost_temp" + extension);

                        try (java.io.FileInputStream in = new java.io.FileInputStream(originalFile);
                             java.io.FileOutputStream out = new java.io.FileOutputStream(repostFile)) {
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = in.read(buffer)) > 0) {
                                out.write(buffer, 0, length);
                            }
                        }

                        Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                                ctx, ctx.getPackageName() + ".fileprovider", repostFile);

                        // 3. Direct WhatsApp Repost Intent
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            executeRepost(ctx, contentUri, isVid);
                        });
                    }
                } catch (Exception e) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            Toast.makeText(ctx, "Repost failed", Toast.LENGTH_SHORT).show());
                }
            }).start();
        } else {
            // Local files ke liye direct call
            executeRepost(ctx, uri, isVid);
        }
    }

    // 🔥 Helper function jo direct WhatsApp target karta h
    private static void executeRepost(Context ctx, Uri uri, boolean isVid) {
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType(isVid ? "video/*" : "image/*");
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // 🔥 Direct WhatsApp target (Primary focus)
            i.setPackage("com.whatsapp");

            ctx.startActivity(i);
        } catch (Exception e) {
            // Agar WhatsApp install nahi h to normal share dikha do
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType(isVid ? "video/*" : "image/*");
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(Intent.createChooser(i, "Repost via"));
        }
    }

    private static void notifyUser(Context ctx, String name) {
        new PushNotificationHelper(ctx).sendNotification("Status Saver", "Status successfully saved!",
                new Intent(Intent.ACTION_VIEW).setType("image/*"), name.hashCode());
    }
}