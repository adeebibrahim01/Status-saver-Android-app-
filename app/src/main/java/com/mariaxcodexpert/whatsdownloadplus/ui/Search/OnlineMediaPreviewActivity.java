package com.mariaxcodexpert.whatsdownloadplus.ui.Search;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.ads.AdView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.mariaxcodexpert.whatsdownloadplus.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.ui.utils.media.MediaStatusUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OnlineMediaPreviewActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private OnlineMediaPagerAdapter adapter;
    private List<MediaItem> localMediaList = new ArrayList<>();
    private int startPosition = 0;

    private View actionOverlay;
    private CircularProgressIndicator actionProgress;
    private TextView tvActionPercent;
    private int lastProgress = 0;

    private OnlineMediaViewModel viewModel;
    private UserPsychologyManager psychologyManager;
    private String currentQuery;

    @Override
    @SuppressWarnings("unchecked") // 🔥 FIX: Pure method level par warning suppress krain
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_media_preview);

        viewModel = new ViewModelProvider(this).get(OnlineMediaViewModel.class);
        psychologyManager = new UserPsychologyManager(this);

        Intent intent = getIntent();
        if (intent != null) {
            try {
                // Safe Extraction for Android 13+ and Older Versions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    localMediaList = intent.getSerializableExtra("MEDIA_LIST", ArrayList.class);
                } else {
                    localMediaList = (ArrayList<MediaItem>) intent.getSerializableExtra("MEDIA_LIST");
                }
            } catch (Exception e) {
                Log.e("PREVIEW_ERROR", "Data parsing failed: " + e.getMessage());
            }

            startPosition = intent.getIntExtra("POSITION", 0);
            currentQuery = intent.getStringExtra("CURRENT_QUERY");

            if (localMediaList != null) {
                viewModel.setMediaList(new ArrayList<>(localMediaList));
            }
        }

        if (localMediaList == null || localMediaList.isEmpty()) {
            finish();
            return;
        }

        initUI();
        setupViewPager();
    }

    private void initUI() {
        viewPager = findViewById(R.id.mediaViewPager);
        actionOverlay = findViewById(R.id.actionOverlay);
        actionProgress = findViewById(R.id.actionProgress);
        tvActionPercent = findViewById(R.id.tvActionPercent);

        AdView adView = findViewById(R.id.adView);
        if (adView != null) {
            AdManager.loadBannerAd(this, adView);
        }

        findViewById(R.id.closeButton).setOnClickListener(v -> finish());

        findViewById(R.id.cardDownload).setOnClickListener(v -> {
            MediaItem currentItem = localMediaList.get(viewPager.getCurrentItem());
            if (!currentItem.isDownloaded()) {
                if (currentQuery != null) psychologyManager.trackSearch(currentQuery);
                showAd(() -> handleMediaDownload(currentItem));
            } else {
                Toast.makeText(this, "✧ Already in Gallery ✧", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.cardRepost).setOnClickListener(v -> {
            MediaItem currentItem = localMediaList.get(viewPager.getCurrentItem());
            if (currentItem != null) {
                if (currentQuery != null) psychologyManager.trackSearch(currentQuery);
                showAd(() -> {
                    String path = currentItem.isVideo() ? currentItem.getVideoUrl() : currentItem.getUrl();
                    if (path != null) {
                        MediaStatusUtils.repostMedia(this, Uri.parse(path), currentItem.isVideo());
                    }
                });
            }
        });

        findViewById(R.id.cardShareFull).setOnClickListener(v -> {
            MediaItem currentItem = localMediaList.get(viewPager.getCurrentItem());
            if (currentItem != null) {
                if (currentQuery != null) psychologyManager.trackSearch(currentQuery);
                showAd(() -> {
                    String path = currentItem.isVideo() ? currentItem.getVideoUrl() : currentItem.getUrl();
                    MediaStatusUtils.shareMedia(this, Uri.parse(path), currentItem.isVideo());
                });
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateActionButtons(localMediaList.get(position));
            }
        });
    }

    private void setupViewPager() {
        adapter = new OnlineMediaPagerAdapter(this, localMediaList);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(1);
        viewPager.setCurrentItem(startPosition, false);
    }

    private void showAd(Runnable action) {
        if (AdManager.isInterstitialLoaded()) {
            AdManager.showInterstitial(this, new AdManager.AdCallback() {
                @Override public void onAdClosed() { action.run(); }
                @Override public void onAdFailed() { action.run(); }
            });
        } else {
            action.run();
        }
    }

    private void handleMediaDownload(MediaItem item) {
        runOnUiThread(() -> {
            lastProgress = 0;
            actionOverlay.setVisibility(View.VISIBLE);
            actionOverlay.setAlpha(0f);
            actionOverlay.animate().alpha(1.0f).setDuration(300).start();
            updateProgressSmoothly(0);
        });

        MediaStatusUtils.executor.execute(() -> {
            try {
                String downloadUrl = item.isVideo() ? item.getVideoUrl() : item.getUrl();
                String fileName = "Pexels_" + Math.abs(downloadUrl.hashCode()) + (item.isVideo() ? ".mp4" : ".jpg");

                updateProgressSmoothly(30);

                File file = Glide.with(this).asFile().load(downloadUrl)
                        .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get();

                if (file != null && file.exists()) {
                    updateProgressSmoothly(70);
                    MediaStatusUtils.saveToGallery(this, Uri.fromFile(file), null, fileName, item.isVideo(), 100, (success, savedUri) -> {
                        if (success) {
                            runOnUiThread(() -> finalizeSmoothDownload(item, downloadUrl));
                        } else {
                            runOnUiThread(() -> actionOverlay.setVisibility(View.GONE));
                        }
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> actionOverlay.setVisibility(View.GONE));
            }
        });
    }

    private void updateProgressSmoothly(int target) {
        runOnUiThread(() -> {
            ValueAnimator animator = ValueAnimator.ofInt(lastProgress, target);
            animator.setDuration(600);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                int val = (int) animation.getAnimatedValue();
                actionProgress.setProgress(val);
                tvActionPercent.setText(val + "%");
            });
            animator.start();
            lastProgress = target;
        });
    }

    private void finalizeSmoothDownload(MediaItem item, String url) {
        ValueAnimator finalAnim = ValueAnimator.ofInt(lastProgress, 100);
        finalAnim.setDuration(500);
        finalAnim.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            actionProgress.setProgress(val);
            tvActionPercent.setText(val + "%");
        });
        finalAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                item.setDownloaded(true);
                viewModel.updateDownloadStatus(url, true);
                updateActionButtons(item);
                Toast.makeText(OnlineMediaPreviewActivity.this, "✧ Processing Complete ✧", Toast.LENGTH_SHORT).show();
                actionOverlay.postDelayed(() -> {
                    actionOverlay.animate().alpha(0f).setDuration(400).withEndAction(() -> {
                        actionOverlay.setVisibility(View.GONE);
                    }).start();
                }, 800);
            }
        });
        finalAnim.start();
    }

    private void updateActionButtons(MediaItem item) {
        ImageView downloadIcon = findViewById(R.id.downloadActionButton);
        TextView saveText = findViewById(R.id.saveText);
        if (item.isDownloaded()) {
            if (downloadIcon != null) {
                downloadIcon.setImageResource(R.drawable.ic_check_circle);
                downloadIcon.setAlpha(0.7f);
            }
            if (saveText != null) {
                saveText.setText("SAVED");
                saveText.setTextColor(getResources().getColor(R.color.premium_gold));
            }
        } else {
            if (downloadIcon != null) {
                downloadIcon.setImageResource(R.drawable.ic_download);
                downloadIcon.setAlpha(1.0f);
            }
            if (saveText != null) {
                saveText.setText("SAVE");
                saveText.setTextColor(getResources().getColor(android.R.color.white));
            }
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        if (actionOverlay != null) actionOverlay.clearAnimation();
        if (viewPager != null) viewPager.unregisterOnPageChangeCallback(null);
        super.onDestroy();
        try {
            String[] tempFiles = {"share_temp.jpg", "share_temp.mp4", "repost_temp.jpg", "repost_temp.mp4"};
            for (String fileName : tempFiles) {
                File file = new File(getCacheDir(), fileName);
                if (file.exists()) file.delete();
            }
        } catch (Exception ignored) {}
    }
}