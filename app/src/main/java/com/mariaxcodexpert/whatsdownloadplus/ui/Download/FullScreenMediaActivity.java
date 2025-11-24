package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.mariaxcodexpert.whatsdownloadplus.R;

public class FullScreenMediaActivity extends AppCompatActivity {

    public static final String EXTRA_URI = "extra_uri";
    public static final String EXTRA_IS_VIDEO = "extra_is_video";

    private ImageView fullImage, closeButton;
    private VideoView fullVideo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_media);

        fullImage = findViewById(R.id.fullImage);
        fullVideo = findViewById(R.id.fullVideo);
        closeButton = findViewById(R.id.closeButton);

        Uri mediaUri = getIntent().getParcelableExtra(EXTRA_URI);
        boolean isVideo = getIntent().getBooleanExtra(EXTRA_IS_VIDEO, false);

        if (isVideo) {
            fullImage.setVisibility(View.GONE);
            fullVideo.setVisibility(View.VISIBLE);

            fullVideo.setVideoURI(mediaUri);
            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(fullVideo);
            fullVideo.setMediaController(mediaController);
            fullVideo.start();

            fadeInView(fullVideo);
        } else {
            fullVideo.setVisibility(View.GONE);
            fullImage.setVisibility(View.VISIBLE);

            Glide.with(this)
                    .load(mediaUri)
                    .placeholder(R.drawable.image_bg)
                    .error(R.drawable.ic_download)
                    .into(fullImage);

            fadeInView(fullImage);
        }

        // Close button click
        closeButton.setOnClickListener(v -> finish());

        // Tap anywhere to close
        fullImage.setOnClickListener(v -> finish());
        fullVideo.setOnCompletionListener(mp -> finish());
    }

    private void fadeInView(View view) {
        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .setDuration(300)
                .start();
    }

}
