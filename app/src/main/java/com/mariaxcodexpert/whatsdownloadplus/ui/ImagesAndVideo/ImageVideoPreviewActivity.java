package com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
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

public class ImageVideoPreviewActivity extends AppCompatActivity {

    // Aapke Adapter mein jo keys hain wahi yahan honi chahye
    public static final String EXTRA_URI = "uri";
    public static final String EXTRA_IS_VIDEO = "is_video";
    private static final int UCROP_REQUEST_CODE = 69;

    private ImageView imagePreview, btnClose, btnShare, btnCrop, btnForward, btnInfo, playOverlay;
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
        btnCrop = findViewById(R.id.btnCrop);
        cardCrop = findViewById(R.id.cardCrop);
        btnForward = findViewById(R.id.btnForward);
        btnInfo = findViewById(R.id.btnInfo);
        playOverlay = findViewById(R.id.playOverlay);
    }

    private void initMedia() {
        // 🔥 FIX 1: Kyunki Adapter se String bheji thi, isliye getStringExtra use karein
        String uriString = getIntent().getStringExtra(EXTRA_URI);
        isVideo = getIntent().getBooleanExtra(EXTRA_IS_VIDEO, false);

        if (uriString != null) {
            mediaUri = Uri.parse(uriString);
        }

        cardCrop.setVisibility(isVideo ? View.GONE : View.VISIBLE);

        if (mediaUri != null) {
            if (isVideo) showVideo(mediaUri);
            else showImage(mediaUri);
        } else {
            Toast.makeText(this, "Error: Media not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupActions() {
        btnClose.setOnClickListener(v -> finish());
        btnShare.setOnClickListener(v -> shareMedia());
        btnForward.setOnClickListener(v -> shareMedia());
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
                .into(imagePreview);
    }

    private void showVideo(Uri uri) {
        imagePreview.setVisibility(View.GONE);
        videoPreview.setVisibility(View.VISIBLE);
        playOverlay.setVisibility(View.VISIBLE);

        // 🔥 VideoView ko play karne se pehle ye zaroori hai
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
                playOverlay.setImageResource(R.drawable.ic_play_circle);
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

    private void shareMedia() {
        if (mediaUri == null) return;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(isVideo ? "video/*" : "image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, mediaUri);

        // 🔥 Scoped Storage ke liye permission dena zaroori hai
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    // --- Media Info Logic (Baqi code thik hai lekin handle errors) ---
    private void showMediaInfo() {
        if (mediaUri == null) return;
        try {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_media_info, null);
            // ... (Aapka purana dialog code) ...

            AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
            dialogView.findViewById(R.id.btnOk).setOnClickListener(v -> dialog.dismiss());

            // Info update
            ((TextView)dialogView.findViewById(R.id.tvMediaType)).setText(isVideo ? "Type: Video" : "Type: Image");
            ((TextView)dialogView.findViewById(R.id.tvFileName)).setText("Name: " + getFileName(mediaUri));

            dialog.show();
        } catch (Exception e) {
            Toast.makeText(this, "Info not available", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileName(Uri uri) {
        String name = "Unknown";
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME);
                if (idx != -1) name = cursor.getString(idx);
            }
        } catch (Exception ignored) {}
        return name;
    }
}