package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class StatusSaverUtils {

    // -----------------------------------------------------------------------------------------
    // CHECK IF FILE ALREADY SAVED (Android 10+ and Android 10 below both)
    // -----------------------------------------------------------------------------------------
    public static boolean isAlreadySaved(Context context, String fileName) {

        if (fileName == null) return false;

        String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=?";
        String[] args = new String[]{fileName};

        // Check IMAGES
        try (Cursor c = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.MediaColumns._ID},
                selection, args, null
        )) {
            if (c != null && c.getCount() > 0) return true;
        } catch (Exception ignored) {}

        // Check VIDEOS
        try (Cursor c = context.getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.MediaColumns._ID},
                selection, args, null
        )) {
            if (c != null && c.getCount() > 0) return true;
        } catch (Exception ignored) {}

        return false;
    }

    // -----------------------------------------------------------------------------------------
    // DETECT MIME TYPE BY FILE NAME
    // -----------------------------------------------------------------------------------------
    public static String getMimeType(String fileName) {
        if (fileName.toLowerCase().endsWith(".mp4")
                || fileName.endsWith(".mkv")
                || fileName.endsWith(".3gp")) {
            return "video/mp4";
        }
        return "image/jpeg";
    }

    // -----------------------------------------------------------------------------------------
    // CREATE OUTPUT URI FOR BOTH ANDROID 10+ AND 10 BELOW
    // -----------------------------------------------------------------------------------------
    public static Uri getOutputUri(Context context, String fileName, String mimeType) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/Status Saver");

            return context.getContentResolver().insert(
                    mimeType.startsWith("video")
                            ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            : MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
            );
        }

        // Android 9 & below → normal file
        File folder = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Status Saver"
        );
        if (!folder.exists()) folder.mkdirs();

        return Uri.fromFile(new File(folder, fileName));
    }

    // -----------------------------------------------------------------------------------------
    // SAVE FILE (GENERIC FOR ANDROID 10+ and 10 BELOW)
    // -----------------------------------------------------------------------------------------
    public static boolean saveStatus(Context context, DocumentFile inputFile) {

        try {
            String fileName = inputFile.getName();
            if (fileName == null) fileName = "status_" + System.currentTimeMillis();

            String mimeType = getMimeType(fileName);

            // Fix extension for images
            if (mimeType.startsWith("image") &&
                    !fileName.endsWith(".jpg") &&
                    !fileName.endsWith(".jpeg") &&
                    !fileName.endsWith(".png")) {
                fileName += ".jpg";
            }

            Uri outputUri = getOutputUri(context, fileName, mimeType);
            if (outputUri == null) return false;

            InputStream in = context.getContentResolver().openInputStream(inputFile.getUri());
            OutputStream out = context.getContentResolver().openOutputStream(outputUri);

            if (in == null || out == null) return false;

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            in.close();
            out.close();

            // Refresh gallery
            context.sendBroadcast(
                    new android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, outputUri)
            );

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // -----------------------------------------------------------------------------------------
    // REUSABLE: ADD FILES FROM FOLDER TO IMAGE/VIDEO LISTS
    // -----------------------------------------------------------------------------------------
    public static void addFilesFromFolder(File folder, List<DocumentFile> imageList, List<DocumentFile> videoList) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) return;

        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isFile()) {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")) {
                    imageList.add(DocumentFile.fromFile(file));
                } else if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".3gp")) {
                    videoList.add(DocumentFile.fromFile(file));
                }
            } else if (file.isDirectory()) {
                addFilesFromFolder(file, imageList, videoList);
            }
        }
    }

    public static void addFilesFromFolder(DocumentFile folder, List<DocumentFile> imageList, List<DocumentFile> videoList) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) return;

        for (DocumentFile file : folder.listFiles()) {
            if (file.isFile()) {
                String name = file.getName() != null ? file.getName().toLowerCase() : "";
                if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")) {
                    imageList.add(file);
                } else if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".3gp")) {
                    videoList.add(file);
                }
            } else if (file.isDirectory()) {
                addFilesFromFolder(file, imageList, videoList);
            }
        }
    }
}
