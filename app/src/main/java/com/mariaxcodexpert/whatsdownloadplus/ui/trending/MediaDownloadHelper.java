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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mariaxcodexpert.whatsdownloadplus.R;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class MediaDownloadHelper {

    private static final String TAG = "MediaDownloadHelper";

    public interface DownloadCallback {
        void onDownloadCompleted(Uri uri, String mimeType);
        void onDownloadFailed(String error);
    }

    public static void downloadToMediaStore(@NonNull Context context, @Nullable String fileUrl, @Nullable String type, @NonNull DownloadCallback callback) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            callback.onDownloadFailed("Invalid URL");
            return;
        }
        WeakReference<Context> contextRef = new WeakReference<>(context);

        new Thread(() -> {
            Uri uri = null;
            HttpURLConnection connection = null;
            InputStream input = null;
            OutputStream output = null;

            try {
                Context innerContext = contextRef.get();
                if (innerContext == null) return;
                boolean isVideo = "video".equalsIgnoreCase(type);
                String extension = isVideo ? ".mp4" : ".jpg";
                String fileName = "WA_Status_" + System.currentTimeMillis() + extension;
                String mimeType = isVideo ? "video/mp4" : "image/jpeg";

                Uri collection = isVideo ?
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI :
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    String folder = isVideo ? Environment.DIRECTORY_MOVIES : Environment.DIRECTORY_PICTURES;
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, folder + "/TrendingStatus");
                    values.put(MediaStore.MediaColumns.IS_PENDING, 1);
                }

                uri = innerContext.getContentResolver().insert(collection, values);

                if (uri == null) throw new Exception(innerContext.getString(R.string.error_mediastore_entry_failed));

                URL url = new URL(fileUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000); // 15 seconds timeout
                connection.setReadTimeout(20000);
                connection.setInstanceFollowRedirects(true);
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new Exception(innerContext.getString(R.string.error_server_response_code, connection.getResponseCode()));
                  }

                input = connection.getInputStream();
                output = innerContext.getContentResolver().openOutputStream(uri);

                if (output == null) {
                    throw new Exception(innerContext.getString(R.string.error_output_stream_failed));
                }
                byte[] buffer = new byte[8192];
                int len;
                while ((len = input.read(buffer)) != -1) {
                    output.write(buffer, 0, len);
                }

                output.flush();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                    innerContext.getContentResolver().update(uri, values, null, null);
                }
                Uri finalUri = uri;
                new Handler(Looper.getMainLooper()).post(() -> callback.onDownloadCompleted(finalUri, mimeType));

            } catch (Exception e) {
                Log.e(TAG, "Download Error: " + e.getMessage());
                if (uri != null && contextRef.get() != null) {
                    contextRef.get().getContentResolver().delete(uri, null, null);
                }
                new Handler(Looper.getMainLooper()).post(() -> callback.onDownloadFailed(e.getMessage()));
            } finally {
                try {
                    if (input != null) input.close();
                    if (output != null) output.close();
                    if (connection != null) connection.disconnect();
                } catch (Exception ignored) {}
            }
        }).start();
    }

    public static void shareToWhatsApp(Context context, Uri fileUri, String mimeType) {
        if (context == null || fileUri == null) return;

        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            intent.setPackage("com.whatsapp");
            Intent chooser = Intent.createChooser(intent, context.getString(R.string.chooser_title_whatsapp_status));
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);

        } catch (Exception e) {
            try {
                Intent genericIntent = new Intent(Intent.ACTION_SEND);
                genericIntent.setType(mimeType);
                genericIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                genericIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Intent chooser = Intent.createChooser(genericIntent, context.getString(R.string.chooser_title_share_status));
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(chooser);
            } catch (Exception ex) {
                Toast.makeText(context, context.getString(R.string.toast_no_sharing_app_found), Toast.LENGTH_SHORT).show();
            }
        }
    }
}