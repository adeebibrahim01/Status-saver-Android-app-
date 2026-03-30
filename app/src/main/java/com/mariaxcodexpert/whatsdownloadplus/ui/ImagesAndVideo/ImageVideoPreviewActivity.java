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
import com.mariaxcodexpert.whatsdownloadplus.SmartNotify;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

@OptIn(markerClass = UnstableApi.class)
public class ImageVideoPreviewActivity extends AppCompatActivity {

    private boolean isDataChanged = false;
    public static final String EXTRA_URI = "uri";
    public static final String EXTRA_IS_VIDEO = "is_video";
    private com.google.android.material.button.MaterialButton btnClose;
    private ImageView imagePreview, btnShare, btnCrop, btnTrim, btnInfo, btnSave;
    private TextView tvSave;
    private PlayerView playerView;
    private ExoPlayer exoPlayer;
    private LinearLayout cardCrop, cardTrim, cardSave;
    private boolean isVideo;
    private Uri mediaUri;
    private boolean isMuted = false;
    private String originalFileName; // Isme original WhatsApp name save hoga
    private final String SAVE_FOLDER_NAME = "Status Saver";
    private android.view.GestureDetector gestureDetector;
    // Top par jahan "private Uri mediaUri;" wagaira hain, wahan ye add karein:
    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful() && result.getUriContent() != null) {
                    mediaUri = result.getUriContent();
                    showImage(mediaUri);
                    notifyDataChanged();
                    checkIfAlreadySaved();

                    // FIX: Activity ka root view nikaal kar SmartNotify use kiya hai
                    View rootView = findViewById(android.R.id.content);
                    SmartNotify.success(rootView, "Edited Successfully! ✨");

                } else if (result.getError() != null) {
                    // Agar crop mein koi error aaye toh red snackbar dikhayein
                    View rootView = findViewById(android.R.id.content);
                    SmartNotify.error(rootView, "Crop Failed: " + result.getError().getMessage());
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

    private void showTrimDialog() {
        if (exoPlayer == null) return;

        // Root view for when dialog is not visible
        View rootView = findViewById(android.R.id.content);

        long totalDurationS = exoPlayer.getDuration() / 1000;
        if (totalDurationS <= 0) {
            SmartNotify.error(rootView, "Video loading, try again...");
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_trim, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etStart = dialogView.findViewById(R.id.etStartTime);
        EditText etEnd = dialogView.findViewById(R.id.etEndTime);
        TextView tvTotal = dialogView.findViewById(R.id.tvTotalDuration);

        tvTotal.setText("Total Duration: " + totalDurationS + "s");
        etEnd.setText(String.valueOf(Math.min(totalDurationS, 30)));

        dialogView.findViewById(R.id.btnConfirmTrim).setOnClickListener(v -> {
            String sStart = etStart.getText().toString();
            String sEnd = etEnd.getText().toString();

            if (sStart.isEmpty() || sEnd.isEmpty()) {
                // Yahan 'v' (button) use kar rahe hain kyunki dialog abhi open hai
                SmartNotify.info(v, "Please enter values");
                return;
            }

            try {
                long startMs = Long.parseLong(sStart) * 1000;
                long endMs = Long.parseLong(sEnd) * 1000;

                if (endMs <= startMs || endMs > exoPlayer.getDuration()) {
                    SmartNotify.error(v, "Invalid range! Check start/end points.");
                } else {
                    dialog.dismiss();
                    // Dismiss ke baad rootView use karein kyunki dialogView destroy ho chuka hoga
                    SmartNotify.success(rootView, "Trimming started... Please wait.");
                    executeMedia3Trim(startMs, endMs);
                }
            } catch (NumberFormatException e) {
                SmartNotify.error(v, "Please enter valid numbers");
            }
        });

        dialog.show();
    }

    // 🔥 NEW: Execute Custom Trim using Media3 Transformer
    private void executeMedia3Trim(long startMs, long endMs) {
// Root view nikaalein (Activity ke andar)
        View rootView = findViewById(android.R.id.content);

// SmartNotify use karein
        SmartNotify.info(rootView, "Trimming: " + (startMs/1000) + "s to " + (endMs/1000) + "s ⏳");
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
// Activity ka root view nikaalein
                            View rootView = findViewById(android.R.id.content);

// SmartNotify use karein
                            SmartNotify.success(rootView, "Trimmed! ✂️");
 });
                    }

                    @Override
                    public void onError(Composition composition, ExportResult exportResult, ExportException exportException) {
                        runOnUiThread(() -> {
                            // Activity ka root view nikaalein
                            View rootView = findViewById(android.R.id.content);

                            // SmartNotify ka error (Red) style use karein
                            SmartNotify.error(rootView, "Error: " + exportException.getMessage());
                        });
                    }
                })
                .build();

        try {
            transformer.start(editedMediaItem, outputFile.getAbsolutePath());
        } catch (Exception e) {
            // Activity ka root view nikaalein
            View rootView1 = findViewById(android.R.id.content);

            // SmartNotify ka error style (Red) use karein
            SmartNotify.error(rootView1, "Export failed: " + e.getMessage());
        }
    }

    private void showVideo(Uri uri) {
        imagePreview.setVisibility(View.GONE);
        playerView.setVisibility(View.VISIBLE);

        if (exoPlayer != null) {
            exoPlayer.release();
        }

        // 1. Setup Player with Audio Attributes & Seek Increments
        exoPlayer = new ExoPlayer.Builder(this)
                .setAudioAttributes(
                        new androidx.media3.common.AudioAttributes.Builder()
                                .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                                .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                                .build(),
                        true
                )
                .setSeekBackIncrementMs(10000)    // 10 sec back
                .setSeekForwardIncrementMs(10000) // 10 sec forward
                .build();

        // 2. Error Listener
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(androidx.media3.common.PlaybackException error) {
                SmartNotify.error(findViewById(android.R.id.content), "Error playing video! ❌");
            }
        });

        // 3. UI & Controls Customization
        playerView.setPlayer(exoPlayer);
        playerView.setKeepScreenOn(true);
        playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);

        playerView.setUseController(true);
        playerView.setControllerShowTimeoutMs(2000);
        playerView.setControllerHideOnTouch(true);

        // 🔥 MODERN GESTURES: Double Tap & Single Tap
        final android.view.GestureDetector gestureDetector = new android.view.GestureDetector(this,
                new android.view.GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onDoubleTap(android.view.MotionEvent e) {
                        float width = playerView.getWidth();
                        float x = e.getX();

                        if (x < width * 0.35) {
                            // Left Side Double Tap: Rewind
                            exoPlayer.seekBack();
                            SmartNotify.info(findViewById(android.R.id.content), "Rewind 10s ⏪");
                        } else if (x > width * 0.65) {
                            // Right Side Double Tap: Forward
                            exoPlayer.seekForward();
                            SmartNotify.info(findViewById(android.R.id.content), "Forward 10s ⏩");
                        }
                        return true;
                    }

                    @Override
                    public boolean onSingleTapConfirmed(android.view.MotionEvent e) {
                        // Middle/Single Tap: Toggle Mute
                        toggleMute();
                        return true;
                    }
                });

        // Touch listener jo controller ke clicks ko disturb nahi karega
        playerView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });

        // 4. Load Media
        exoPlayer.setMediaItem(MediaItem.fromUri(uri));
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        exoPlayer.setPlayWhenReady(true);
        exoPlayer.prepare();
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
                        // Activity ka root view nikaalein
                        View rootView = findViewById(android.R.id.content);

// SmartNotify use karein (Success Style)
                        SmartNotify.success(rootView, "Saved to Status Saver! ✅");

// Data refresh karein
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
                    // Activity ka root view (pure screen ka main container)
                    View rootView = findViewById(android.R.id.content);

// SmartNotify ka Success (Green) style
                    SmartNotify.success(rootView, "Saved Successfully! ✅");
                    notifyDataChanged();
                    checkIfAlreadySaved(); // 🔥
                }
            }
        } catch (Exception e) {
            // Activity ka root view nikaalein
            View rootView = findViewById(android.R.id.content);

            // SmartNotify ka error (Red) style use karein
            SmartNotify.error(rootView, "Save Failed: " + e.getMessage());
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
//ok
    private void toggleMute() {
        if (exoPlayer != null) {
            isMuted = !isMuted;
            exoPlayer.setVolume(isMuted ? 0.0f : 1.0f);
            // Root view nikaalein (Activity context mein)
            View rootView = findViewById(android.R.id.content);

// SmartNotify use karein (Muted/Unmuted toggle ke liye)
            SmartNotify.info(rootView, isMuted ? "Muted 🔇" : "Audio On 🔊");
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
        // Jab user screen se hat jaye (Fragment ya Activity change) toh pause karein
        if (exoPlayer != null) {
            exoPlayer.pause();
            exoPlayer.setPlayWhenReady(false); // Ye audio ko strictly stop kar deta hai
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Technical safety: onStop ensure karta hai ke background mein kuch na chale
        if (exoPlayer != null) {
            exoPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) exoPlayer.release();
    }
}