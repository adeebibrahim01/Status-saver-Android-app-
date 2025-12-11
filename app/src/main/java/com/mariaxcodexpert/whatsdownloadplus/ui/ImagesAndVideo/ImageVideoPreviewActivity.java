package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageVideoPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_URI = "extra_uri";
    public static final String EXTRA_IS_VIDEO = "extra_is_video";
    private static final int UCROP_REQUEST_CODE = 69;

    private ImageView imagePreview, btnClose, btnShare, btnDownload, btnCrop, btnForward, btnInfo, playOverlay;
    private VideoView videoPreview;
    private LinearLayout cardCrop;
    private boolean isVideo;
    private Uri mediaUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_video_preview_activity);

        bindViews();
        initMedia();
        setupActions();
    }

    private void bindViews() {
        imagePreview = findViewById(R.id.imagePreview);
        videoPreview = findViewById(R.id.videoPreview);
        btnClose = findViewById(R.id.btnClosePreview);
        btnShare = findViewById(R.id.btnShare);
        btnDownload = findViewById(R.id.btnDownload);
        btnCrop = findViewById(R.id.btnCrop);
        cardCrop = findViewById(R.id.cardCrop);
        btnForward = findViewById(R.id.btnForward);
        btnInfo = findViewById(R.id.btnInfo);
        playOverlay = findViewById(R.id.playOverlay);
    }

    private void initMedia() {
        mediaUri = getIntent().getParcelableExtra(EXTRA_URI);
        isVideo = getIntent().getBooleanExtra(EXTRA_IS_VIDEO, false);
        cardCrop.setVisibility(isVideo ? View.GONE : View.VISIBLE);

        if (mediaUri != null) {
            if (isVideo) showVideo(mediaUri);
            else showImage(mediaUri);
        }
    }

    private void setupActions() {
        btnClose.setOnClickListener(v -> finish());
        btnShare.setOnClickListener(v -> shareMedia());
        btnForward.setOnClickListener(v -> shareMedia());
        btnDownload.setOnClickListener(v -> downloadMedia());
        btnCrop.setOnClickListener(v -> cropImage());
        btnInfo.setOnClickListener(v -> showMediaInfo());
    }

    private void showImage(Uri uri) {
        imagePreview.setVisibility(View.VISIBLE);
        videoPreview.setVisibility(View.GONE);
        playOverlay.setVisibility(View.GONE);

        Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.image_bg)
                .error(R.drawable.ic_download_white)
                .into(imagePreview);
    }

    private void showVideo(Uri uri) {
        imagePreview.setVisibility(View.GONE);
        videoPreview.setVisibility(View.VISIBLE);
        playOverlay.setVisibility(View.VISIBLE);
        videoPreview.setVideoURI(uri);
        playOverlay.setImageResource(R.drawable.ic_play_circle);

        videoPreview.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            videoPreview.start();
            playOverlay.setVisibility(View.GONE);
        });

        View.OnClickListener togglePlay = v -> {
            if (videoPreview.isPlaying()) {
                videoPreview.pause();
                playOverlay.setImageResource(R.drawable.ic_pause_circle);
                playOverlay.setVisibility(View.VISIBLE);
            } else {
                videoPreview.start();
                playOverlay.setVisibility(View.GONE);
            }
        };

        playOverlay.setOnClickListener(togglePlay);
        videoPreview.setOnClickListener(togglePlay);
    }

    private void cropImage() {
        if (isVideo || mediaUri == null) return;
        Uri destUri = Uri.fromFile(new File(getCacheDir(), "cropped_" + System.currentTimeMillis() + ".jpg"));
        UCrop.of(mediaUri, destUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(1080, 1080)
                .start(this, UCROP_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UCROP_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                mediaUri = resultUri;
                showImage(resultUri);
                Toast.makeText(this, "Image cropped successfully ✅", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void shareMedia() {
        if (mediaUri == null) return;
        startActivity(Intent.createChooser(
                new Intent(Intent.ACTION_SEND)
                        .setType(isVideo ? "video/*" : "image/*")
                        .putExtra(Intent.EXTRA_STREAM, mediaUri),
                "Share via"
        ));
    }

    private void downloadMedia() {
        if (mediaUri == null) return;
        try {
            Uri outUri = getOutputUri(isVideo ? "mp4" : "jpg");
            copyUri(mediaUri, outUri);
            Toast.makeText(this, "Saved to Status Saver ✅", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save ❌", Toast.LENGTH_SHORT).show();
        }
    }

    private Uri getOutputUri(String ext) throws Exception {
        String filename = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String mimeType = isVideo ? "video/mp4" : "image/jpeg";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Status Saver");

            return getContentResolver().insert(
                    isVideo ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
            );
        } else {
            File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Status Saver");
            if (!folder.exists()) folder.mkdirs();
            return Uri.fromFile(new File(folder, filename + "." + ext));
        }
    }

    private void copyUri(Uri src, Uri dest) throws Exception {
        try (InputStream in = getContentResolver().openInputStream(src);
             OutputStream out = getContentResolver().openOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
        }
    }

    private void showMediaInfo() {
        if (mediaUri == null) return;
        try {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_media_info, null);
            TextView tvType = dialogView.findViewById(R.id.tvMediaType);
            TextView tvName = dialogView.findViewById(R.id.tvFileName);
            TextView tvSize = dialogView.findViewById(R.id.tvFileSize);
            TextView tvRes = dialogView.findViewById(R.id.tvResolution);
            TextView tvDur = dialogView.findViewById(R.id.tvDuration);
            Button btnOk = dialogView.findViewById(R.id.btnOk);

            tvType.setText(isVideo ? "Type: Video" : "Type: Image");
            tvName.setText("Name: " + getFileName(mediaUri));
            tvSize.setText("Size: " + getFileSize(mediaUri) + " KB");

            if (isVideo) {
                String[] videoMeta = getVideoMetadata(mediaUri);
                tvRes.setText("Resolution: " + videoMeta[0] + " x " + videoMeta[1]);
                tvDur.setText("Duration: " + videoMeta[2]);
                tvDur.setVisibility(View.VISIBLE);
            } else {
                String[] imageMeta = getImageResolution(mediaUri);
                tvRes.setText("Resolution: " + imageMeta[0] + " x " + imageMeta[1]);
                tvDur.setVisibility(View.GONE);
            }

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create();
            btnOk.setOnClickListener(v -> dialog.dismiss());
            dialog.show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to get media info", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------------- GENERIC HELPERS ----------------------
    private String getFileName(Uri uri) {
        String name = "Unknown";
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                if (idx != -1) name = cursor.getString(idx);
            } else if ("file".equals(uri.getScheme())) {
                name = new File(uri.getPath()).getName();
            }
        } catch (Exception ignored) {}
        return name;
    }

    private long getFileSize(Uri uri) {
        long size = 0;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
                if (idx != -1) size = cursor.getLong(idx) / 1024;
            } else if ("file".equals(uri.getScheme())) {
                size = new File(uri.getPath()).length() / 1024;
            }
        } catch (Exception ignored) {}
        return size;
    }

    private String[] getVideoMetadata(Uri uri) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(this, uri);
        int width = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        int height = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        long durMs = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        retriever.release();

        long hours = durMs / (1000 * 60 * 60);
        long minutes = (durMs / (1000 * 60)) % 60;
        long seconds = (durMs / 1000) % 60;
        String duration = (hours > 0 ? hours + "h " : "") + minutes + "m " + seconds + "s";

        return new String[]{String.valueOf(width), String.valueOf(height), duration};
    }

    private String[] getImageResolution(Uri uri) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(input, null, options);
        } catch (Exception ignored) {}
        return new String[]{String.valueOf(options.outWidth), String.valueOf(options.outHeight)};
    }
}
