package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.SmartNotify;
import com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo.SavedFilesDB;

import java.io.File;

@OptIn(markerClass = UnstableApi.class)
public class FullScreenMediaActivity extends AppCompatActivity {

    public static final String EXTRA_URI = "extra_uri";
    public static final String EXTRA_IS_VIDEO = "extra_is_video";

    private PhotoView fullImage;
    private PlayerView playerView;
    private ExoPlayer exoPlayer;
    private View bottomActions, topScrim;
    private MaterialButton closeButton, shareActionButton, repostActionButton, deleteActionButton;

    private Uri mediaUri;
    private boolean isUiVisible = true;
    private SavedFilesDB savedFilesDB;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_full_screen_media);

        savedFilesDB = new SavedFilesDB(this);
        initViews();

        String uriString = getIntent().getStringExtra(EXTRA_URI);
        boolean isVideo = getIntent().getBooleanExtra(EXTRA_IS_VIDEO, false);

        if (uriString != null) {
            mediaUri = Uri.parse(uriString);
            if (isVideo) setupVideo(); else setupImage();
        }

        setupListeners(isVideo);
    }

    private void initViews() {
        fullImage = findViewById(R.id.fullImage);
        playerView = findViewById(R.id.fullPlayerView);
        bottomActions = findViewById(R.id.bottomActions);
        topScrim = findViewById(R.id.topScrim);

        closeButton = findViewById(R.id.closeButton);
        shareActionButton = findViewById(R.id.shareActionButton);
        repostActionButton = findViewById(R.id.repostActionButton);
        deleteActionButton = findViewById(R.id.deleteActionButton);
    }

    private void setupListeners(boolean isVideo) {
        closeButton.setOnClickListener(v -> finish());
        shareActionButton.setOnClickListener(v -> shareMedia(false));
        repostActionButton.setOnClickListener(v -> shareMedia(true));

        // 🔥 Dialog khatam, ab click par direct delete hoga
        deleteActionButton.setOnClickListener(v -> {
            if (mediaUri != null) {
                deleteFile(mediaUri, isVideo, v);
            }
        });

        fullImage.setOnPhotoTapListener((view, x, y) -> toggleUI());
    }

    // 🔥 EXACT SAME LOGIC AS DOWNLOAD ADAPTER
    private void deleteFile(Uri fileUri, boolean isVideo, View view) {
        boolean deleted = false;
        try {
            // 1. Check Scheme and Delete Physically
            if ("file".equals(fileUri.getScheme())) {
                File file = new File(fileUri.getPath());
                if (file.exists()) {
                    deleted = file.delete();
                }
            } else {
                // MediaStore delete (Android 10+)
                deleted = getContentResolver().delete(fileUri, null, null) > 0;
            }

            if (deleted) {
                // 2. Get Filename for DB
                String fileName;
                if ("file".equals(fileUri.getScheme())) {
                    fileName = new File(fileUri.getPath()).getName();
                } else {
                    fileName = getFileNameFromUri(fileUri);
                }

                // 3. Remove from Database
                if (savedFilesDB != null && fileName != null) {
                    savedFilesDB.removeFile(fileName);
                }

                // 4. Update Gallery Scan (For Android 9/Legacy)
                if ("file".equals(fileUri.getScheme())) {
                    android.media.MediaScannerConnection.scanFile(this,
                            new String[]{fileUri.getPath()}, null, null);
                }

                SmartNotify.success(view, "Deleted successfully!");

                // Finish activity with result OK so fragment can refresh if needed
                view.postDelayed(() -> {
                    setResult(Activity.RESULT_OK);
                    finish();
                }, 500);

            } else {
                SmartNotify.error(view, "Could not delete file");
            }

        } catch (Exception e) {
            e.printStackTrace();
            SmartNotify.error(view, "Error: " + e.getMessage());
        }
    }

    private String getFileNameFromUri(Uri uri) {
        if ("file".equals(uri.getScheme())) {
            return new File(uri.getPath()).getName();
        }
        String[] projection = { MediaStore.MediaColumns.DISPLAY_NAME };
        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                return cursor.getString(index);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setupImage() {
        playerView.setVisibility(View.GONE);
        fullImage.setVisibility(View.VISIBLE);
        Glide.with(this).load(mediaUri).into(fullImage);
    }

    private void setupVideo() {
        fullImage.setVisibility(View.GONE);
        playerView.setVisibility(View.VISIBLE);

        exoPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);
        exoPlayer.setMediaItem(MediaItem.fromUri(mediaUri));
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        exoPlayer.prepare();
        exoPlayer.play();

        GestureDetector detector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (e.getX() < playerView.getWidth() * 0.35) exoPlayer.seekBack();
                else if (e.getX() > playerView.getWidth() * 0.65) exoPlayer.seekForward();
                return true;
            }
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleUI();
                return true;
            }
        });

        playerView.setOnTouchListener((v, event) -> {
            detector.onTouchEvent(event);
            return true;
        });
    }

    private void toggleUI() {
        isUiVisible = !isUiVisible;
        float alpha = isUiVisible ? 1f : 0f;
        bottomActions.animate().alpha(alpha).setDuration(300).start();
        closeButton.animate().alpha(alpha).setDuration(300).start();
        topScrim.animate().alpha(alpha).setDuration(300).start();

        if (isUiVisible) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void shareMedia(boolean isRepost) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(getIntent().getBooleanExtra(EXTRA_IS_VIDEO, false) ? "video/*" : "image/*");
        intent.putExtra(Intent.EXTRA_STREAM, mediaUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (isRepost) {
            intent.setPackage("com.whatsapp");
            try {
                startActivity(intent);
            } catch (Exception e) {
                SmartNotify.error(findViewById(android.R.id.content), "WhatsApp not installed!");
            }
        } else {
            startActivity(Intent.createChooser(intent, "Share via:"));
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