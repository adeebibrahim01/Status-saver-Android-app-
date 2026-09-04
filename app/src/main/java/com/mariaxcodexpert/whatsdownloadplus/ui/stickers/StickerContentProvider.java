package com.mariaxcodexpert.whatsdownloadplus.ui.stickers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public class StickerContentProvider extends ContentProvider {

    public static final String AUTHORITY = "com.mariaxcodexpert.whatsdownloadplus.stickercontentprovider";
    private static final String TAG = "PROVIDER_DEBUG";

    private static final int METADATA = 1;
    private static final int METADATA_CODE = 2;
    private static final int STICKERS = 3;
    private static final int STICKERS_ASSET = 4;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, "metadata", METADATA);
        uriMatcher.addURI(AUTHORITY, "metadata/*", METADATA_CODE);
        uriMatcher.addURI(AUTHORITY, "stickers/*", STICKERS);
        uriMatcher.addURI(AUTHORITY, "stickers_asset/*/*", STICKERS_ASSET);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.e(TAG, "!!! PROVIDER QUERY START !!! URI: " + uri);

        int match = uriMatcher.match(uri);
        List<String> segments = uri.getPathSegments();

        if (match == METADATA || match == METADATA_CODE) {
            String identifier = (segments.size() > 1) ? segments.get(1) : "abc";

            // WhatsApp Metadata Column Names (Strict order)
            MatrixCursor cursor = new MatrixCursor(new String[]{
                    "sticker_pack_identifier", "sticker_pack_name", "sticker_pack_publisher",
                    "tray_image_file", "image_data_version", "avoid_cache", "animated_sticker_pack"
            });
            cursor.addRow(new Object[]{identifier, "My Pack", "OmarSamy Creations", "tray.webp", "1", 0, 0});
            return cursor;
        }

        if (match == STICKERS) {
            String packName = segments.get(1);
            File folder = new File(getContext().getFilesDir(), "my_stickers/" + packName);
            MatrixCursor cursor = new MatrixCursor(new String[]{"sticker_file_name", "sticker_emoji"});

            if (folder.exists()) {
                File[] files = folder.listFiles((dir, name) -> name.endsWith(".webp") && !name.equals("tray.webp"));
                if (files != null) {
                    for (File f : files) {
                        cursor.addRow(new Object[]{f.getName(), ""});
                    }
                }
            }
            return cursor;
        }
        return null;
    }

    @Nullable
    @Override
    public AssetFileDescriptor openAssetFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        if (uriMatcher.match(uri) == STICKERS_ASSET) {
            List<String> path = uri.getPathSegments();
            File file = new File(getContext().getFilesDir(), "my_stickers/" + path.get(1) + "/" + path.get(2));
            if (file.exists()) {
                return new AssetFileDescriptor(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY), 0, AssetFileDescriptor.UNKNOWN_LENGTH);
            }
        }
        throw new FileNotFoundException("File not found: " + uri);
    }

    @Override
    public String getType(@NonNull Uri uri) { return "vnd.android.cursor.dir/vnd." + AUTHORITY + ".stickers"; }
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) { return null; }
    @Override
    public int delete(@NonNull Uri uri, String s, String[] args) { return 0; }
    @Override
    public int update(@NonNull Uri uri, ContentValues values, String s, String[] args) { return 0; }
}