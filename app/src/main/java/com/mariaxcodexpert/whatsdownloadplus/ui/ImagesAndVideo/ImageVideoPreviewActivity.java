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
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.yalantis.ucrop.UCrop;

import java.io.File;
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

        btnClose.setOnClickListener(v -> finish());

        mediaUri = getIntent().getParcelableExtra(EXTRA_URI);
        isVideo = getIntent().getBooleanExtra(EXTRA_IS_VIDEO, false);

        if (mediaUri != null) {
            if (isVideo) showVideo(mediaUri);
            else showImage(mediaUri);
        }

        // Hide Edit/Crop card ONLY for video
        cardCrop.setVisibility(isVideo ? View.GONE : View.VISIBLE);

        // Share & Forward
        btnShare.setOnClickListener(v -> shareMedia());
        btnForward.setOnClickListener(v -> shareMedia());

        // Download
        btnDownload.setOnClickListener(v -> downloadMedia());

        // Crop (only for images)
        btnCrop.setOnClickListener(v -> cropImage());

        // Info
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
        playOverlay.setImageResource(R.drawable.ic_play_circle); // show play initially

        videoPreview.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            videoPreview.start();
            playOverlay.setVisibility(View.GONE); // hide overlay when playing
        });

        // Toggle play/pause when overlay clicked
        playOverlay.setOnClickListener(v -> {
            if (videoPreview.isPlaying()) {
                videoPreview.pause();
                playOverlay.setImageResource(R.drawable.ic_pause_circle);
                playOverlay.setVisibility(View.VISIBLE);
            } else {
                videoPreview.start();
                playOverlay.setVisibility(View.GONE);
            }
        });

        // Pause video if user clicks directly on video
        videoPreview.setOnClickListener(v -> {
            if (videoPreview.isPlaying()) {
                videoPreview.pause();
                playOverlay.setImageResource(R.drawable.ic_pause_circle);
                playOverlay.setVisibility(View.VISIBLE);
            }
        });
    }

    private void cropImage() {
        if (isVideo) return;

        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "cropped_" + System.currentTimeMillis() + ".jpg"));
        UCrop.of(mediaUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(1080, 1080)
                .start(this, UCROP_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UCROP_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri resultUri = UCrop.getOutput(data);
                if (resultUri != null) {
                    mediaUri = resultUri;
                    showImage(resultUri);
                    Toast.makeText(this, "Image cropped successfully ✅", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void shareMedia() {
        if (mediaUri == null) return;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(isVideo ? "video/*" : "image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, mediaUri);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void downloadMedia() {
        if (mediaUri == null) return;

        try {
            String filename = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String mimeType = isVideo ? "video/mp4" : "image/jpeg";
            Uri outUri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Status Saver");

                outUri = getContentResolver().insert(
                        isVideo ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values);
            } else {
                File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Status Saver");
                if (!folder.exists()) folder.mkdirs();
                File file = new File(folder, filename + (isVideo ? ".mp4" : ".jpg"));
                outUri = Uri.fromFile(file);
            }

            try (InputStream in = getContentResolver().openInputStream(mediaUri);
                 OutputStream out = getContentResolver().openOutputStream(outUri)) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }

            Toast.makeText(this, "Saved to Status Saver ✅", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save ❌", Toast.LENGTH_SHORT).show();
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

            String name = "Unknown";
            if ("content".equals(mediaUri.getScheme())) {
                Cursor cursor = getContentResolver().query(mediaUri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    if (nameIndex != -1) name = cursor.getString(nameIndex);
                    cursor.close();
                }
            } else if ("file".equals(mediaUri.getScheme())) {
                name = new File(mediaUri.getPath()).getName();
            }
            tvName.setText("Name: " + name);

            long size = 0;
            if ("content".equals(mediaUri.getScheme())) {
                Cursor cursor = getContentResolver().query(mediaUri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex);
                    cursor.close();
                }
            } else if ("file".equals(mediaUri.getScheme())) {
                size = new File(mediaUri.getPath()).length();
            }
            tvSize.setText("Size: " + (size / 1024) + " KB");

            if (isVideo) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(this, mediaUri);
                String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                retriever.release();

                long durMs = Long.parseLong(duration);
                long seconds = (durMs / 1000) % 60;
                long minutes = (durMs / (1000 * 60)) % 60;
                long hours = durMs / (1000 * 60 * 60);

                tvDur.setVisibility(View.VISIBLE);
                tvDur.setText("Duration: " + (hours > 0 ? hours + "h " : "") + minutes + "m " + seconds + "s");
                tvRes.setText("Resolution: " + width + " x " + height);
            } else {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                try (InputStream input = getContentResolver().openInputStream(mediaUri)) {
                    BitmapFactory.decodeStream(input, null, options);
                }
                tvRes.setText("Resolution: " + options.outWidth + " x " + options.outHeight);
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
}
