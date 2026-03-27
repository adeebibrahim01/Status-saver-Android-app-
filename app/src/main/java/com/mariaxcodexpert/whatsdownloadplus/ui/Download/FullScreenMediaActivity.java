package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.VideoView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.button.MaterialButton;
import com.mariaxcodexpert.whatsdownloadplus.R;

public class FullScreenMediaActivity extends AppCompatActivity {

    public static final String EXTRA_URI = "extra_uri";
    public static final String EXTRA_IS_VIDEO = "extra_is_video";

    private PhotoView fullImage;
    private VideoView fullVideo;
    private MaterialButton closeButton, shareActionButton, saveActionButton;
    private Uri mediaUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // UI Polish: Transparent Status Bar for Immersive Look
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_full_screen_media);

        initViews();

        String uriString = getIntent().getStringExtra(EXTRA_URI);
        boolean isVideo = getIntent().getBooleanExtra(EXTRA_IS_VIDEO, false);

        if (uriString != null) {
            mediaUri = Uri.parse(uriString);
            if (isVideo) setupVideo(); else setupImage();
        }

        handleListeners(isVideo);
    }

    private void initViews() {
        fullImage = findViewById(R.id.fullImage);
        fullVideo = findViewById(R.id.fullVideo);
        closeButton = findViewById(R.id.closeButton);
        shareActionButton = findViewById(R.id.shareActionButton);

    }

    private void handleListeners(boolean isVideo) {
        closeButton.setOnClickListener(v -> finish());

        // 🚀 Share Functionality
        shareActionButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(isVideo ? "video/*" : "image/*");
            intent.putExtra(Intent.EXTRA_STREAM, mediaUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Status via:"));
        });

    }

    private void setupImage() {
        fullVideo.setVisibility(View.GONE);
        fullImage.setVisibility(View.VISIBLE);
        Glide.with(this).load(mediaUri).into(fullImage);
        fullImage.setAlpha(0f);
        fullImage.animate().alpha(1f).setDuration(400).start();
    }

    private void setupVideo() {
        fullImage.setVisibility(View.GONE);
        fullVideo.setVisibility(View.VISIBLE);
        fullVideo.setVideoURI(mediaUri);

        MediaController mc = new MediaController(this);
        mc.setAnchorView(fullVideo);
        fullVideo.setMediaController(mc);

        fullVideo.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            fullVideo.start();
        });
    }
}