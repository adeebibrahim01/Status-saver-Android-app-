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
import com.mariaxcodexpert.whatsdownloadplus.Helper.PushNotificationHelper;
import com.mariaxcodexpert.whatsdownloadplus.R;
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

    public static void saveToDatabase(Context ctx, Uri savedUri, String name, boolean isVid) {
        executor.execute(() -> {
            syncToSpecificTable(ctx, savedUri.toString(), savedUri.toString(), name, isVid);
        });
    }
    public static void saveToGallery(Context context, Uri source, Bitmap bmp, String name, boolean isVid, int quality, SaveCallback cb) {
        executor.execute(() -> {
            boolean success = false;
            Uri savedUri = null;
            ContentResolver resolver = context.getContentResolver();

            try {
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                v.put(MediaStore.MediaColumns.MIME_TYPE, isVid ? "video/mp4" : "image/jpeg");
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

                    v.clear();
                    v.put(MediaStore.MediaColumns.IS_PENDING, 0);
                    resolver.update(savedUri, v, null, null);

                    if (success) {
                        String uriString = savedUri.toString();
                        syncToSpecificTable(context, source.toString(), uriString, name, isVid);
                    }
                }
            } catch (Exception e) {
                Log.e("MediaStatusUtils", "Save Error: " + e.getMessage());
            }

            final boolean finalSuccess = success;
            final Uri finalUri = savedUri;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (finalSuccess) {
                    notifyUser(context, name);
                }
                cb.onSaveResult(finalSuccess, finalUri);
            });
        });
    }

    private static void syncToSpecificTable(Context ctx, String src, String savedUriPath, String name, boolean isVid) {
        try {
            AppDatabase db = AppDatabase.getInstance(ctx);
            long now = System.currentTimeMillis();

            long sevenDaysInMs = 7L * 24 * 60 * 60 * 1000;
            long expiryAt = now + sevenDaysInMs;

            if (isVid) {
                VideoEntity video = new VideoEntity(
                        name,
                        src,
                        savedUriPath,
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
                        savedUriPath,
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
        if (ctx == null || uri == null) return;

        if (uri.toString().startsWith("http")) {
            Toast.makeText(ctx, ctx.getString(R.string.toast_preparing_media), Toast.LENGTH_SHORT).show();
            final Context appCtx = ctx.getApplicationContext();
            String downloadUrl = uri.toString();

            new Thread(() -> {
                try {
                    String extension = isVid ? ".mp4" : ".jpg";
                    java.io.File shareFile = new java.io.File(appCtx.getCacheDir(), "share_temp" + extension);

                    okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                    okhttp3.Request request = new okhttp3.Request.Builder().url(downloadUrl).build();

                    try (okhttp3.Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful() || response.body() == null) throw new Exception("Download failed");

                        try (okio.BufferedSink sink = okio.Okio.buffer(okio.Okio.sink(shareFile));
                             okio.BufferedSource source = response.body().source()) {
                            sink.writeAll(source);
                            sink.flush();
                        }
                    }

                    if (shareFile.exists()) {
                        Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                                appCtx, appCtx.getPackageName() + ".fileprovider", shareFile);
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            Intent i = new Intent(Intent.ACTION_SEND)
                                    .setType(isVid ? "video/*" : "image/*")
                                    .putExtra(Intent.EXTRA_STREAM, contentUri)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            ctx.startActivity(Intent.createChooser(i, ctx.getString(R.string.chooser_title_share_via)));
                        });
                    }
                } catch (Exception e) {
                    android.util.Log.e("SHARE_ERROR", "Failed to share: " + e.getMessage());
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            Toast.makeText(appCtx, appCtx.getString(R.string.error_share_failed), Toast.LENGTH_SHORT).show()
                    );
                }
            }).start();
        } else {
            Intent i = new Intent(Intent.ACTION_SEND)
                    .setType(isVid ? "video/*" : "image/*")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(Intent.createChooser(i, ctx.getString(R.string.chooser_title_share_via)));
                 }
    }

    public static void repostMedia(Context ctx, Uri uri, boolean isVid) {
        if (ctx == null || uri == null) return;

        if (uri.toString().startsWith("http")) {
            Toast.makeText(ctx, ctx.getString(R.string.toast_preparing_repost), Toast.LENGTH_SHORT).show();
            final Context appCtx = ctx.getApplicationContext();
            String downloadUrl = uri.toString();

            new Thread(() -> {
                try {
                    String extension = isVid ? ".mp4" : ".jpg";
                    java.io.File repostFile = new java.io.File(appCtx.getCacheDir(), "repost_temp" + extension);

                    okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                    okhttp3.Request request = new okhttp3.Request.Builder().url(downloadUrl).build();

                    try (okhttp3.Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful() || response.body() == null) throw new Exception("Download failed");

                        try (okio.BufferedSink sink = okio.Okio.buffer(okio.Okio.sink(repostFile));
                             okio.BufferedSource source = response.body().source()) {
                            sink.writeAll(source);
                            sink.flush();
                        }
                    }

                    if (repostFile.exists()) {
                        Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                                appCtx, appCtx.getPackageName() + ".fileprovider", repostFile);

                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            executeRepost(ctx, contentUri, isVid);
                        });
                    }
                } catch (Exception e) {
                    android.util.Log.e("REPOST_ERROR", "Failed to prepare repost: " + e.getMessage());
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            Toast.makeText(appCtx, appCtx.getString(R.string.error_repost_failed), Toast.LENGTH_SHORT).show()
                    );
                }
            }).start();
        } else {
            executeRepost(ctx, uri, isVid);
        }
    }

    private static void executeRepost(Context ctx, Uri uri, boolean isVid) {
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType(isVid ? "video/*" : "image/*");
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            i.setPackage("com.whatsapp");

            ctx.startActivity(i);
        } catch (Exception e) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType(isVid ? "video/*" : "image/*");
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(Intent.createChooser(i, ctx.getString(R.string.chooser_title_repost_via)));
        }
    }

    private static void notifyUser(Context ctx, String name) {
        new PushNotificationHelper(ctx).sendNotification(
                ctx.getString(R.string.notification_title_status_saver),
                ctx.getString(R.string.notification_desc_saved_success),
                new Intent(Intent.ACTION_VIEW).setType("image/*"),
                name.hashCode()
        );
    }
}