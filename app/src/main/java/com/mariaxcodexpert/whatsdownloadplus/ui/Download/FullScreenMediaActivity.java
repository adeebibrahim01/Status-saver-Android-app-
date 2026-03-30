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
import com.google.android.gms.ads.AdView;
import com.google.android.material.button.MaterialButton;
import com.mariaxcodexpert.whatsdownloadplus.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.SmartNotify;
import com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo.SavedFilesDB;

@OptIn(markerClass = UnstableApi.class)
public class FullScreenMediaActivity extends AppCompatActivity {

    // 🔥 Keys ko static final rakhein taake mismatch na ho
    public static final String EXTRA_URI = "EXTRA_URI";
    public static final String EXTRA_IS_VIDEO = "EXTRA_IS_VIDEO";

    private PhotoView fullImage;
    private PlayerView playerView;
    private ExoPlayer exoPlayer;
    private View bottomActions, topScrim;
    private MaterialButton closeButton, shareActionButton, repostActionButton, deleteActionButton;

    // Banner Ad View
    private AdView adView;

    private Uri mediaUri;
    private boolean isVideo = false;
    private boolean isUiVisible = true;
    private SavedFilesDB savedFilesDB;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-Edge display
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_full_screen_media);

        savedFilesDB = new SavedFilesDB(this);
        initViews();

        // Banner Ad loading using AdManager logic
        if (adView != null) {
            AdManager.loadBannerAd(this, adView);
        }

        // 🔥 DATA RETRIEVAL FIX
        String uriString = getIntent().getStringExtra(EXTRA_URI);
        isVideo = getIntent().getBooleanExtra(EXTRA_IS_VIDEO, false);

        if (uriString != null && !uriString.isEmpty()) {
            mediaUri = Uri.parse(uriString);

            if (isVideo) {
                setupVideo();
            } else {
                setupImage();
            }
        } else {
            SmartNotify.error(findViewById(android.R.id.content), "Media not found!");
            finish();
        }

        setupListeners();
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

        // Find the Banner AdView from XML
        adView = findViewById(R.id.adView);
    }

    private void setupImage() {
        playerView.setVisibility(View.GONE);
        fullImage.setVisibility(View.VISIBLE);

        // 🔥 HIGH PERFORMANCE LOADING
        Glide.with(this)
                .load(mediaUri)
                .placeholder(R.drawable.image_bg)
                .error(R.drawable.ic_download)
                .into(fullImage);
    }

    private void setupVideo() {
        fullImage.setVisibility(View.GONE);
        playerView.setVisibility(View.VISIBLE);

        // ExoPlayer Setup
        exoPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);

        MediaItem mediaItem = MediaItem.fromUri(mediaUri);
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        exoPlayer.prepare();
        exoPlayer.play();

        // Double Tap to Seek (10s)
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

        playerView.setClickable(true);
        playerView.setOnTouchListener((v, event) -> {
            detector.onTouchEvent(event);
            return true;
        });
    }

    private void setupListeners() {
        closeButton.setOnClickListener(v -> finish());
        shareActionButton.setOnClickListener(v -> shareMedia(false));
        repostActionButton.setOnClickListener(v -> shareMedia(true));

        deleteActionButton.setOnClickListener(v -> {
            if (mediaUri != null) deleteFile(mediaUri, v);
        });

        fullImage.setOnPhotoTapListener((view, x, y) -> toggleUI());
    }

    private void shareMedia(boolean isRepost) {
        if (mediaUri == null) return;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(isVideo ? "video/*" : "image/*");
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

    private void deleteFile(Uri fileUri, View view) {
        try {
            int deletedRows = getContentResolver().delete(fileUri, null, null);
            if (deletedRows > 0) {
                String fileName = getFileNameFromUri(fileUri);
                if (savedFilesDB != null && fileName != null) savedFilesDB.removeFile(fileName);
                SmartNotify.success(view, "Deleted successfully!");
                view.postDelayed(() -> {
                    setResult(Activity.RESULT_OK);
                    finish();
                }, 800);
            }
        } catch (Exception e) {
            SmartNotify.error(view, "Permission Denied");
        }
    }

    private String getFileNameFromUri(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getString(0);
        } catch (Exception ignored) {}
        return null;
    }

    private void toggleUI() {
        isUiVisible = !isUiVisible;
        float alpha = isUiVisible ? 1f : 0f;
        bottomActions.animate().alpha(alpha).setDuration(250).start();
        closeButton.animate().alpha(alpha).setDuration(250).start();
        topScrim.animate().alpha(alpha).setDuration(250).start();

        // Optional: Banner ko bhi toggle kar sakte hain agar zaroorat ho
        if (adView != null) {
            adView.animate().alpha(alpha).setDuration(250).start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) adView.resume();
    }

    @Override
    protected void onPause() {
        if (adView != null) adView.pause();
        super.onPause();
        if (exoPlayer != null) exoPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        if (adView != null) adView.destroy();
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}