package com.mariaxcodexpert.whatsdownloadplus.ui.trending;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MediaDownloadHelper {

    private static final String TAG = "MediaDownloadHelper";

    /**
     * Callback interface download ke status ke liye
     */
    public interface DownloadCallback {
        void onDownloadCompleted(Uri uri, String mimeType);
        void onDownloadFailed(String error);
    }

    /**
     * Media file download karne aur MediaStore (Gallery) mein save karne ke liye
     */
    public static void downloadToMediaStore(Context context, String fileUrl, String type, DownloadCallback callback) {
        new Thread(() -> {
            Uri uri = null;
            try {
                // 1. File Info setup
                String extension = "video".equalsIgnoreCase(type) ? ".mp4" : ".jpg";
                String fileName = "WA_Status_" + System.currentTimeMillis() + extension;
                String mimeType = "video".equalsIgnoreCase(type) ? "video/mp4" : "image/jpeg";

                Uri collection = "video".equalsIgnoreCase(type) ?
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI :
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    String folder = "video".equalsIgnoreCase(type) ? Environment.DIRECTORY_MOVIES : Environment.DIRECTORY_PICTURES;
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, folder + "/TrendingStatus");
                    values.put(MediaStore.MediaColumns.IS_PENDING, 1);
                }

                uri = context.getContentResolver().insert(collection, values);

                if (uri != null) {
                    URL url = new URL(fileUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(20000);
                    connection.connect();

                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        throw new Exception("Server returned HTTP " + connection.getResponseCode());
                    }

                    InputStream input = connection.getInputStream();
                    OutputStream output = context.getContentResolver().openOutputStream(uri);

                    // 2. High Quality Stream Copy
                    byte[] buffer = new byte[1024 * 16];
                    int len;
                    while ((len = input.read(buffer)) != -1) {
                        output.write(buffer, 0, len);
                    }

                    output.flush();
                    output.close();
                    input.close();

                    // 3. Finalize File
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear();
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                        context.getContentResolver().update(uri, values, null, null);
                    }

                    Uri finalUri = uri;
                    new Handler(Looper.getMainLooper()).post(() -> callback.onDownloadCompleted(finalUri, mimeType));
                }
            } catch (Exception e) {
                Log.e(TAG, "Download Error: " + e.getMessage());
                if (uri != null) {
                    context.getContentResolver().delete(uri, null, null);
                }
                new Handler(Looper.getMainLooper()).post(() -> callback.onDownloadFailed(e.getMessage()));
            }
        }).start();
    }

    /**
     * Downloaded file ko WhatsApp Status par share karne ke liye
     */
    public static void shareToWhatsApp(Context context, Uri fileUri, String mimeType) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            intent.setPackage("com.whatsapp"); // Original WhatsApp ko target kar raha hai
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Chooser taake user ko option mile (WhatsApp ya Business WhatsApp)
            Intent chooser = Intent.createChooser(intent, "Set Status via:");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);

        } catch (Exception e) {
            Log.e(TAG, "Share Error: " + e.getMessage());
            // Agar normal WhatsApp nahi milta to try karein ke generic share open ho jaye
            try {
                Intent genericIntent = new Intent(Intent.ACTION_SEND);
                genericIntent.setType(mimeType);
                genericIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                genericIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(Intent.createChooser(genericIntent, "Share Status"));
            } catch (Exception ex) {
                Toast.makeText(context, "WhatsApp not installed!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}