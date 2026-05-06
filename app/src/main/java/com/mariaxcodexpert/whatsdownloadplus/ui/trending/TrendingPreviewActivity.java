package com.mariaxcodexpert.whatsdownloadplus.ui.trending;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.databinding.ActivityTrendingPreviewBinding;
import com.mariaxcodexpert.whatsdownloadplus.ui.utils.media.player.ExoPlayerManager;

import java.util.ArrayList;

@OptIn(markerClass = UnstableApi.class)
public class TrendingPreviewActivity extends AppCompatActivity {

    private ActivityTrendingPreviewBinding binding;
    private ExoPlayer player;
    private ArrayList<TrendMediaItem> mediaList;
    private int startingPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTrendingPreviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mediaList = (ArrayList<TrendMediaItem>) getIntent().getSerializableExtra("MEDIA_LIST");
        startingPosition = getIntent().getIntExtra("POSITION", 0);

        if (mediaList == null || mediaList.isEmpty()) {
            finish();
            return;
        }

        binding.btnBack.setOnClickListener(v -> finish());

        setupViewPager();
        // 1. IMAGE OPTIMIZATION: Pehle hi images buffer karna start kar dein
        preloadImages(startingPosition);
    }

    private void setupViewPager() {
        TrendingPagerAdapter adapter = new TrendingPagerAdapter(mediaList);
        binding.viewPager.setAdapter(adapter);

        // 2. PERFORMANCE: Isse agla aur pichla page (Image/Video) ready rehta hai
        binding.viewPager.setOffscreenPageLimit(3);

        binding.viewPager.setCurrentItem(startingPosition, false);

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                handleMediaPlayback(position);
                // Swipe hone par agay wali images preload karein
                preloadImages(position);
            }
        });

        binding.viewPager.post(() -> handleMediaPlayback(startingPosition));
    }

    private void preloadImages(int currentPos) {
        // Current position se agli 3 images ko background mein cache kar lo
        for (int i = currentPos; i < Math.min(currentPos + 4, mediaList.size()); i++) {
            TrendMediaItem item = mediaList.get(i);
            if (!item.isVideo()) {
                Glide.with(this)
                        .load(item.getMediaUrl())
                        .preload(); // Ye image ko RAM/Disk mein save kar lega
            }
        }
    }

    private void handleMediaPlayback(int position) {
        releasePlayer();
        TrendMediaItem currentItem = mediaList.get(position);

        if (currentItem.getTitle() != null) {
            binding.tvTrendTitle.setText("✧ " + currentItem.getTitle() + " ✧");
        }

        // Button listener update
        binding.btnSetStatus.setOnClickListener(v -> {
            String type = currentItem.isVideo() ? "video" : "image";
            MediaDownloadHelper.downloadToMediaStore(this, currentItem.getMediaUrl(), type, new MediaDownloadHelper.DownloadCallback() {
                @Override
                public void onDownloadCompleted(Uri uri, String mimeType) {
                    MediaDownloadHelper.shareToWhatsApp(TrendingPreviewActivity.this, uri, mimeType);
                }

                @Override
                public void onDownloadFailed(String error) {
                    Toast.makeText(TrendingPreviewActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Agar video hai to setup karein, warna releasePlayer() images ke liye kafi hai
        if (currentItem.isVideo()) {
            setupVideoForPage(position, currentItem.getMediaUrl());
        }
    }

    private void setupVideoForPage(int position, String url) {
        RecyclerView recyclerView = (RecyclerView) binding.viewPager.getChildAt(0);
        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);

        if (holder != null) {
            PlayerView pagerPlayerView = holder.itemView.findViewById(R.id.pagerPlayerView);
            ProgressBar pagerLoader = holder.itemView.findViewById(R.id.pagerLoader);

            player = ExoPlayerManager.createPlayer(this);
            pagerPlayerView.setPlayer(player);

            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    pagerLoader.setVisibility(state == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
                }
            });

            player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));
            player.prepare();
            player.setPlayWhenReady(true);
            player.setRepeatMode(Player.REPEAT_MODE_ONE);
        }
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}