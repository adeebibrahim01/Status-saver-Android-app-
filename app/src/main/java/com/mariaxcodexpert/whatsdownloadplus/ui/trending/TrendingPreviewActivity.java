package com.mariaxcodexpert.whatsdownloadplus.ui.trending;

import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.Toast;

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
import com.mariaxcodexpert.whatsdownloadplus.Ads.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.databinding.ActivityTrendingPreviewBinding;
import com.mariaxcodexpert.whatsdownloadplus.ui.utils.media.player.ExoPlayerManager;

import java.io.Serializable;
import java.util.ArrayList;

@OptIn(markerClass = UnstableApi.class)
public class TrendingPreviewActivity extends AppCompatActivity {

    private ActivityTrendingPreviewBinding binding;
    private ExoPlayer player;
    private ArrayList<TrendMediaItem> mediaList;
    private int startingPosition;
    private int lastProgress = 0;
    private boolean isUiVisible = true;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTrendingPreviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadIntentData();

        if (mediaList == null || mediaList.isEmpty()) {
            finish();
            return;
        }

        if (binding.adView != null) {
            AdManager.loadBannerAd(this, binding.adView);
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.tvTrendTitle.setOnClickListener(v -> {
            String titleText = binding.tvTrendTitle.getText().toString();
            String titleToCopy = titleText.replace("✧", "").trim();
            if (!titleToCopy.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Trending Title", titleToCopy);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, getString(R.string.toast_title_copied), Toast.LENGTH_SHORT).show();
                 }
            }
        });

        setupViewPager();
    }

    private void toggleUI() {
        if (binding == null) return;
        isUiVisible = !isUiVisible;
        float alpha = isUiVisible ? 1.0f : 0.0f;
        binding.adContainer.animate().alpha(alpha).setDuration(250).start();
        binding.bottomControls.animate().alpha(alpha).setDuration(250).start();
        binding.btnBack.animate().alpha(alpha).setDuration(250).start();

        int visibility = isUiVisible ? View.VISIBLE : View.GONE;
        mainHandler.postDelayed(() -> {
            if (binding != null) {
                binding.adContainer.setVisibility(visibility);
                binding.bottomControls.setVisibility(visibility);
                binding.btnBack.setVisibility(visibility);
            }
        }, 250);
    }

    @SuppressWarnings("unchecked")
    private void loadIntentData() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mediaList = getIntent().getSerializableExtra("MEDIA_LIST", (Class<ArrayList<TrendMediaItem>>) (Class<?>) ArrayList.class);
            } else {
                Serializable serializable = getIntent().getSerializableExtra("MEDIA_LIST");
                if (serializable instanceof ArrayList) {
                    mediaList = (ArrayList<TrendMediaItem>) serializable;
                }
            }
            startingPosition = getIntent().getIntExtra("POSITION", 0);
        } catch (Exception e) {
            mediaList = new ArrayList<>();
        }
    }

    private void setupViewPager() {
        TrendingPagerAdapter adapter = new TrendingPagerAdapter(mediaList);
        adapter.setOnItemClickListener(this::toggleUI);

        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setOffscreenPageLimit(2);
        binding.viewPager.setCurrentItem(startingPosition, false);

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                handleMediaPlayback(position);
                preloadImages(position);
            }
        });

        binding.viewPager.post(() -> handleMediaPlayback(startingPosition));
    }

    private void handleMediaPlayback(int position) {
        releasePlayer();
        if (binding == null || mediaList == null || position >= mediaList.size()) return;

        TrendMediaItem currentItem = mediaList.get(position);

        binding.tvTrendTitle.setText(currentItem.getTitle() != null ? currentItem.getTitle() : getString(R.string.fallback_trending_status));
        binding.btnSetStatus.setOnClickListener(v -> startPremiumDownload(currentItem));

        if (currentItem.isVideo() && currentItem.getMediaUrl() != null) {
            setupVideoForPage(position, currentItem.getMediaUrl());
        }
    }

    private void setupVideoForPage(int position, String url) {
        mainHandler.postDelayed(() -> {
            if (binding == null) return;
            View view = binding.viewPager.getChildAt(0);
            if (view instanceof RecyclerView) {
                RecyclerView.ViewHolder holder = ((RecyclerView) view).findViewHolderForAdapterPosition(position);
                if (holder != null) {
                    PlayerView pagerPlayerView = holder.itemView.findViewById(R.id.pagerPlayerView);
                    ProgressBar pagerLoader = holder.itemView.findViewById(R.id.pagerLoader);

                    if (pagerPlayerView != null) {
                        player = ExoPlayerManager.createPlayer(this);
                        pagerPlayerView.setPlayer(player);

                        player.addListener(new Player.Listener() {
                            @Override
                            public void onPlaybackStateChanged(int state) {
                                if (pagerLoader != null) {
                                    pagerLoader.setVisibility(state == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
                                }
                            }
                        });

                        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));
                        player.prepare();
                        player.setPlayWhenReady(true);
                        player.setRepeatMode(Player.REPEAT_MODE_ONE);
                    }
                }
            }
        }, 200);
    }

    private void startPremiumDownload(TrendMediaItem item) {
        if (binding == null) return;
        lastProgress = 0;
        binding.loaderContainer.setVisibility(View.VISIBLE);
        binding.loaderContainer.setAlpha(1.0f);

        updateProgressSmoothly(20);
        String type = item.isVideo() ? "video" : "image";

        MediaDownloadHelper.downloadToMediaStore(this, item.getMediaUrl(), type, new MediaDownloadHelper.DownloadCallback() {
            @Override
            public void onDownloadCompleted(Uri uri, String mimeType) {
                updateProgressSmoothly(100);
                mainHandler.postDelayed(() -> {
                    if (binding != null) {
                        binding.loaderContainer.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                            if (binding != null) {
                                binding.loaderContainer.setVisibility(View.GONE);
                                MediaDownloadHelper.shareToWhatsApp(TrendingPreviewActivity.this, uri, mimeType);
                            }
                        }).start();
                    }
                }, 600);
            }

            @Override
            public void onDownloadFailed(String error) {
                mainHandler.post(() -> {
                    if (binding != null) {
                        binding.loaderContainer.setVisibility(View.GONE);
                        Toast.makeText(TrendingPreviewActivity.this, getString(R.string.error_download_failed_network), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void updateProgressSmoothly(int target) {
        ValueAnimator animator = ValueAnimator.ofInt(lastProgress, target);
        animator.setDuration(800);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            if (binding != null) {
                int val = (int) animation.getAnimatedValue();
                binding.exportProgressBar.setProgress(val);
                binding.tvPercentage.setText(getString(R.string.progress_percentage_format_preview, val));
            }
        });
        animator.start();
        lastProgress = target;
    }

    private void preloadImages(int currentPos) {
        if (mediaList == null) return;
        for (int i = currentPos + 1; i < Math.min(currentPos + 3, mediaList.size()); i++) {
            TrendMediaItem item = mediaList.get(i);
            if (item != null && !item.isVideo() && item.getMediaUrl() != null) {
                Glide.with(this).load(item.getMediaUrl()).preload();
            }
        }
    }

    private void releasePlayer() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }

    @Override
    protected void onPause() {
        if (binding != null && binding.adView != null) binding.adView.pause();
        if (player != null) player.setPlayWhenReady(false);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null && binding.adView != null) binding.adView.resume();
        if (player != null) player.setPlayWhenReady(true);
    }

    @Override
    protected void onDestroy() {
        if (binding != null && binding.adView != null) binding.adView.destroy();
        releasePlayer();
        mainHandler.removeCallbacksAndMessages(null);
        binding = null;
        super.onDestroy();
    }
}