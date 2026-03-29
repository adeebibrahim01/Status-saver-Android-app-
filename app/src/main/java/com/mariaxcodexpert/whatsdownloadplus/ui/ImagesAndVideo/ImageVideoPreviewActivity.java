package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.Transformer;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.mariaxcodexpert.whatsdownloadplus.R;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

@OptIn(markerClass = UnstableApi.class)
public class ImageVideoPreviewActivity extends AppCompatActivity {

    private boolean isDataChanged = false;
    public static final String EXTRA_URI = "uri";
    public static final String EXTRA_IS_VIDEO = "is_video";

    private ImageView imagePreview, btnClose, btnShare, btnCrop, btnTrim, btnInfo, btnSave;
    private TextView tvSave;
    private PlayerView playerView;
    private ExoPlayer exoPlayer;
    private LinearLayout cardCrop, cardTrim, cardSave;
    private boolean isVideo;
    private Uri mediaUri;
    private boolean isMuted = false;
    private String originalFileName; // Isme original WhatsApp name save hoga
    private final String SAVE_FOLDER_NAME = "Status Saver";
    // Top par jahan "private Uri mediaUri;" wagaira hain, wahan ye add karein:
    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful() && result.getUriContent() != null) {
                    mediaUri = result.getUriContent();
                    showImage(mediaUri);
                    notifyDataChanged();
                    checkIfAlreadySaved();
                    Toast.makeText(this, "Edited Successfully! ✨", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_video_preview_activity);

        bindViews();
        initMedia();
        setupActions();
        checkIfAlreadySaved();
        setupBackPressed();
    }

    private void bindViews() {
        imagePreview = findViewById(R.id.imagePreview);
        playerView = findViewById(R.id.playerView);
        btnClose = findViewById(R.id.btnClosePreview);
        btnShare = findViewById(R.id.btnShare);
        btnSave = findViewById(R.id.btnSave);
        tvSave = findViewById(R.id.tvSave);
        btnCrop = findViewById(R.id.btnCrop);
        btnTrim = findViewById(R.id.btnTrim);
        cardCrop = findViewById(R.id.cardCrop);
        cardTrim = findViewById(R.id.cardTrim);
        cardSave = findViewById(R.id.cardSave);
        btnInfo = findViewById(R.id.btnInfo);
    }

    private void initMedia() {
        String uriString = getIntent().getStringExtra(EXTRA_URI);
        isVideo = getIntent().getBooleanExtra(EXTRA_IS_VIDEO, false);

        // 🔥 Original name intent se uthayen, agar na mile toh fallback name den
        originalFileName = getIntent().getStringExtra("FILE_NAME");
        if (originalFileName == null) {
            originalFileName = "status_" + System.currentTimeMillis() + (isVideo ? ".mp4" : ".jpg");
        }

        if (uriString != null) mediaUri = Uri.parse(uriString);

        cardCrop.setVisibility(isVideo ? View.GONE : View.VISIBLE);
        cardTrim.setVisibility(isVideo ? View.VISIBLE : View.GONE);

        if (mediaUri != null) {
            if (isVideo) showVideo(mediaUri);
            else showImage(mediaUri);
        } else {
            finish();
        }
    }

    private void setupActions() {
        btnClose.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        btnShare.setOnClickListener(v -> shareMedia());
        btnCrop.setOnClickListener(v -> startAdvancedCrop());

        // 🔥 Custom Trim Dialog call
        btnTrim.setOnClickListener(v -> showTrimDialog());

        btnInfo.setOnClickListener(v -> showMediaInfo());
        cardSave.setOnClickListener(v -> saveMediaToGallery());

        if (playerView != null) {
            playerView.setOnClickListener(v -> {
                if (isVideo && exoPlayer != null) toggleMute();
            });
        }
    }

    // 🔥 NEW: Custom Trim Dialog to get Start and End points
    private void showTrimDialog() {
        if (exoPlayer == null) return;

        long totalDurationS = exoPlayer.getDuration() / 1000;
        if (totalDurationS <= 0) {
            Toast.makeText(this, "Video loading, try again...", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_trim, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText etStart = dialogView.findViewById(R.id.etStartTime);
        EditText etEnd = dialogView.findViewById(R.id.etEndTime);
        TextView tvTotal = dialogView.findViewById(R.id.tvTotalDuration);

        tvTotal.setText("Total Duration: " + totalDurationS + "s");
        etEnd.setText(String.valueOf(Math.min(totalDurationS, 30))); // Default to 30 or total

        dialogView.findViewById(R.id.btnConfirmTrim).setOnClickListener(v -> {
            String sStart = etStart.getText().toString();
            String sEnd = etEnd.getText().toString();

            if (sStart.isEmpty() || sEnd.isEmpty()) {
                Toast.makeText(this, "Please enter values", Toast.LENGTH_SHORT).show();
                return;
            }

            long startMs = Long.parseLong(sStart) * 1000;
            long endMs = Long.parseLong(sEnd) * 1000;

            if (endMs <= startMs || endMs > exoPlayer.getDuration()) {
                Toast.makeText(this, "Invalid range", Toast.LENGTH_SHORT).show();
            } else {
                dialog.dismiss();
                executeMedia3Trim(startMs, endMs);
            }
        });

        dialog.show();
    }

    // 🔥 NEW: Execute Custom Trim using Media3 Transformer
    private void executeMedia3Trim(long startMs, long endMs) {
        Toast.makeText(this, "Trimming: " + (startMs/1000) + "s to " + (endMs/1000) + "s ⏳", Toast.LENGTH_LONG).show();

        File outputDir = new File(getExternalFilesDir(null), "TrimmedVideos");
        if (!outputDir.exists()) outputDir.mkdirs();
        File outputFile = new File(outputDir, "trimmed_" + System.currentTimeMillis() + ".mp4");

        MediaItem.ClippingConfiguration clippingConfiguration = new MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs)
                .setEndPositionMs(endMs)
                .build();

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(mediaUri)
                .setClippingConfiguration(clippingConfiguration)
                .build();

        EditedMediaItem editedMediaItem = new EditedMediaItem.Builder(mediaItem)
                .setRemoveAudio(false)
                .build();

        Transformer transformer = new Transformer.Builder(this)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .addListener(new Transformer.Listener() {
                    @Override
                    public void onCompleted(Composition composition, ExportResult exportResult) {
                        runOnUiThread(() -> {
                            mediaUri = Uri.fromFile(outputFile);
                            // 🔥 Important: originalFileName ko naye trimmed file ke naam se update karein
                            originalFileName = outputFile.getName();

                            showVideo(mediaUri);
                            notifyDataChanged();

                            // Isko call karne se button "Save" par wapas aa jayega
                            checkIfAlreadySaved();

                            Toast.makeText(ImageVideoPreviewActivity.this, "Trimmed! ✂️", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(Composition composition, ExportResult exportResult, ExportException exportException) {
                        runOnUiThread(() -> Toast.makeText(ImageVideoPreviewActivity.this, "Error: " + exportException.getMessage(), Toast.LENGTH_LONG).show());
                    }
                })
                .build();

        try {
            transformer.start(editedMediaItem, outputFile.getAbsolutePath());
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showVideo(Uri uri) {
        imagePreview.setVisibility(View.GONE);
        playerView.setVisibility(View.VISIBLE);

        if (exoPlayer != null) exoPlayer.release();

        // 🚀 Advanced Media3 Player Features
        exoPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);
        playerView.setUseController(true); // Seeker/Controls dikhane ke liye
        playerView.setControllerAutoShow(true);
        playerView.setControllerHideOnTouch(true);

        exoPlayer.setMediaItem(MediaItem.fromUri(uri));
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        exoPlayer.prepare();
        exoPlayer.play();
    }

    private void showImage(Uri uri) {
        imagePreview.setVisibility(View.VISIBLE);
        playerView.setVisibility(View.GONE);
        Glide.with(this).load(uri).into(imagePreview);
    }

    private void startAdvancedCrop() {
        if (isVideo || mediaUri == null) return;
        CropImageOptions options = new CropImageOptions();
        options.guidelines = CropImageView.Guidelines.ON;
        cropImageLauncher.launch(new CropImageContractOptions(mediaUri, options));
    }

    private void saveMediaToGallery() {
        if (mediaUri == null) return;
        String fileName = getFileName(mediaUri); // Original name use hoga
        String mimeType = isVideo ? "video/mp4" : "image/jpeg";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + SAVE_FOLDER_NAME);

                Uri externalUri = isVideo ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                Uri destUri = getContentResolver().insert(externalUri, values);

                if (destUri != null) {
                    try (InputStream is = getContentResolver().openInputStream(mediaUri);
                         OutputStream os = getContentResolver().openOutputStream(destUri)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        showDownloadNotification(fileName);
                        Toast.makeText(this, "Saved to Status Saver! ✅", Toast.LENGTH_SHORT).show();
                        notifyDataChanged();
                        checkIfAlreadySaved(); // 🔥 Save hote hi button disable ho jayega
                    }
                }
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), SAVE_FOLDER_NAME);
                if (!dir.exists()) dir.mkdirs();
                File destFile = new File(dir, fileName);

                try (InputStream is = getContentResolver().openInputStream(mediaUri);
                     OutputStream os = new java.io.FileOutputStream(destFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(destFile)));
                    showDownloadNotification(fileName);
                    Toast.makeText(this, "Saved Successfully! ✅", Toast.LENGTH_SHORT).show();
                    notifyDataChanged();
                    checkIfAlreadySaved(); // 🔥
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Save Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void shareMedia() {
        if (mediaUri == null) return;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(isVideo ? "video/*" : "image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, mediaUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void showMediaInfo() {
        if (mediaUri == null) return;
        try {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_media_info, null);
            AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
            if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            TextView tvType = dialogView.findViewById(R.id.tvMediaType);
            TextView tvName = dialogView.findViewById(R.id.tvFileName);
            tvType.setText("Type: " + (isVideo ? "Video" : "Image"));
            tvName.setText("Name: " + getFileName(mediaUri));
            dialogView.findViewById(R.id.btnOk).setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        } catch (Exception ignored) {}
    }

    private String getFileName(Uri uri) {
        return originalFileName; // 🔥 Ab ye naya random name generate nahi karega
    }
    private void setupBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isDataChanged) setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void notifyDataChanged() {
        isDataChanged = true;
    }

    private void showDownloadNotification(String fileName) {
        String channelId = "status_download_channel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Downloads", NotificationManager.IMPORTANCE_DEFAULT);
            if (notificationManager != null) notificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Download Complete")
                .setContentText(fileName + " has been saved.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        if (notificationManager != null) notificationManager.notify(fileName.hashCode(), builder.build());
    }

    private void toggleMute() {
        if (exoPlayer != null) {
            isMuted = !isMuted;
            exoPlayer.setVolume(isMuted ? 0.0f : 1.0f);
            Toast.makeText(this, isMuted ? "Muted 🔇" : "Audio On 🔊", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkIfAlreadySaved() {
        if (mediaUri == null || originalFileName == null) return;

        // Edited files ko hamesha enable rakhein
        if (originalFileName.startsWith("trimmed_") || originalFileName.startsWith("cropped_")) {
            updateSaveButtonUI(false);
            return;
        }

        executor.execute(() -> {
            boolean isSaved = false;
            try {
                // Logic 1: Direct File Check (Same as Adapter)
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), SAVE_FOLDER_NAME);
                File file = new File(dir, originalFileName);
                if (file.exists() && file.length() > 0) {
                    isSaved = true;
                }

                // Logic 2: MediaStore Check (Same as Adapter)
                if (!isSaved && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Uri collection = isVideo ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=? AND " + MediaStore.MediaColumns.RELATIVE_PATH + " LIKE ?";
                    String[] selectionArgs = new String[]{originalFileName, "%" + SAVE_FOLDER_NAME + "%"};

                    try (android.database.Cursor cursor = getContentResolver().query(collection, new String[]{MediaStore.MediaColumns._ID}, selection, selectionArgs, null)) {
                        if (cursor != null && cursor.getCount() > 0) {
                            isSaved = true;
                        }
                    }
                }
            } catch (Exception ignored) {}

            boolean finalIsSaved = isSaved;
            runOnUiThread(() -> updateSaveButtonUI(finalIsSaved));
        });
    }

    private void updateSaveButtonUI(boolean isSaved) {
        if (isSaved) {
            cardSave.setEnabled(false);
            cardSave.setAlpha(0.5f);
            btnSave.setColorFilter(Color.GRAY);
            if (tvSave != null) tvSave.setText("Saved");
        } else {
            cardSave.setEnabled(true);
            cardSave.setAlpha(1.0f);
            btnSave.setColorFilter(Color.WHITE);
            if (tvSave != null) tvSave.setText("Save");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null) exoPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) exoPlayer.release();
    }
}