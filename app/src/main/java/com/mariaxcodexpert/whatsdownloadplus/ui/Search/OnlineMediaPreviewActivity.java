package com.mariaxcodexpert.whatsdownloadplus.ui.Search;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdView;
import com.mariaxcodexpert.whatsdownloadplus.Ads.AdManager;
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
    private ProgressBar actionProgress;
    private TextView tvActionPercent;
    private int lastProgress = 0;

    private OnlineMediaViewModel viewModel;
    private UserPsychologyManager psychologyManager;
    private String currentQuery;

    private boolean isUIHidden = false;
    private View adContainer, topBar, bottomControls;
    private AdView mAdView;

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_online_media_preview);

            viewModel = new ViewModelProvider(this).get(OnlineMediaViewModel.class);
            psychologyManager = new UserPsychologyManager(this);

            Intent intent = getIntent();
            if (intent != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    localMediaList = (ArrayList<MediaItem>) intent.getSerializableExtra("MEDIA_LIST", ArrayList.class);
                } else {
                    localMediaList = (ArrayList<MediaItem>) intent.getSerializableExtra("MEDIA_LIST");
                }
                startPosition = intent.getIntExtra("POSITION", 0);
                currentQuery = intent.getStringExtra("CURRENT_QUERY");
            }
            if (localMediaList == null || localMediaList.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_something_went_wrong), Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            viewModel.setMediaList(new ArrayList<>(localMediaList));

            initUI();
            setupViewPager();
            updateInitialTitle();

        } catch (Exception e) {
            Log.e("PREVIEW_ERROR", "General Crash Guard: " + e.getMessage());
            finish();
        }
    }
    private void updateInitialTitle() {
        TextView tvFullMediaTitle = findViewById(R.id.tvFullMediaTitle);
        if (tvFullMediaTitle != null && !localMediaList.isEmpty()) {
            tvFullMediaTitle.setText(localMediaList.get(startPosition).getTitle());
        }
    }

    private MediaItem getCurrentItemSafely() {
        if (viewPager != null && localMediaList != null && !localMediaList.isEmpty()) {
            int index = viewPager.getCurrentItem();
            if (index >= 0 && index < localMediaList.size()) {
                return localMediaList.get(index);
            }
        }
        return null;
    }

    private void initUI() {
        viewPager = findViewById(R.id.mediaViewPager);
        actionOverlay = findViewById(R.id.actionOverlay);
        actionProgress = findViewById(R.id.actionProgress);
        tvActionPercent = findViewById(R.id.tvActionPercent);
        adContainer = findViewById(R.id.adContainer);
        topBar = findViewById(R.id.closeButton);
        bottomControls = findViewById(R.id.bottomControls);

        TextView tvFullMediaTitle = findViewById(R.id.tvFullMediaTitle);

        if (tvFullMediaTitle != null) {
            tvFullMediaTitle.setOnClickListener(v -> {
                String textToCopy = tvFullMediaTitle.getText().toString();
                if (!textToCopy.trim().isEmpty()) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Status Title", textToCopy);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, getString(R.string.toast_title_copied), Toast.LENGTH_SHORT).show();
                }
            });
        }

        mAdView = findViewById(R.id.adView);
        if (mAdView != null) {
            AdManager.loadBannerAd(this, mAdView);
        }

        findViewById(R.id.closeButton).setOnClickListener(v -> finish());

        findViewById(R.id.cardDownload).setOnClickListener(v -> {
            MediaItem currentItem = getCurrentItemSafely();
            if (currentItem != null) {
                if (!currentItem.isDownloaded()) {
                    if (currentQuery != null) psychologyManager.trackSearch(currentQuery);
                    showAd(() -> handleMediaDownload(currentItem));
                } else {
                    Toast.makeText(this, getString(R.string.toast_already_gallery), Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.cardRepost).setOnClickListener(v -> {
            MediaItem currentItem = getCurrentItemSafely(); // 🔥 Safe Retrieve
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
            MediaItem currentItem = getCurrentItemSafely();
            if (currentItem != null) {
                if (currentQuery != null) psychologyManager.trackSearch(currentQuery);
                showAd(() -> {
                    String path = currentItem.isVideo() ? currentItem.getVideoUrl() : currentItem.getUrl();
                    if (path != null) {
                        MediaStatusUtils.shareMedia(this, Uri.parse(path), currentItem.isVideo());
                    }
                });
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // Index check for safety
                if (localMediaList != null && position < localMediaList.size()) {
                    MediaItem currentItem = localMediaList.get(position);
                    updateActionButtons(currentItem);

                    if (tvFullMediaTitle != null && currentItem != null) {
                        tvFullMediaTitle.setText(currentItem.getTitle());
                    }
                }
            }
        });
    }
    @Override
    protected void onPause() {
        if (mAdView != null) mAdView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdView != null) mAdView.resume();
    }

    private void toggleUI() {
        isUIHidden = !isUIHidden;
        float alpha = isUIHidden ? 0f : 1f;
        int visibility = isUIHidden ? View.GONE : View.VISIBLE;
        if (adContainer != null) adContainer.animate().alpha(alpha).setDuration(300).withEndAction(() -> adContainer.setVisibility(visibility)).start();
        if (topBar != null) topBar.animate().alpha(alpha).setDuration(300).withEndAction(() -> topBar.setVisibility(visibility)).start();
        if (bottomControls != null) bottomControls.animate().alpha(alpha).setDuration(300).withEndAction(() -> bottomControls.setVisibility(visibility)).start();
    }

    private void handleMediaDownload(MediaItem item) {
        if (item == null || isFinishing() || isDestroyed()) return;

        runOnUiThread(() -> {
            lastProgress = 0;
            if (actionOverlay != null) {
                actionOverlay.setVisibility(View.VISIBLE);
                actionOverlay.setAlpha(0f);
                actionOverlay.animate().alpha(1.0f).setDuration(300).start();
            }
            actionProgress.setProgress(0);
            tvActionPercent.setText("0%");
        });

        String downloadUrl = item.isVideo() ? item.getVideoUrl() : item.getUrl();
        if (downloadUrl == null) {
            runOnUiThread(() -> { if (actionOverlay != null) actionOverlay.setVisibility(View.GONE); });
            return;
        }

        String fileName = "Pexels_" + Math.abs(downloadUrl.hashCode()) + (item.isVideo() ? ".mp4" : ".jpg");

        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder().url(downloadUrl).build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull java.io.IOException e) {
                runOnUiThread(() -> {
                    if (actionOverlay != null) actionOverlay.setVisibility(View.GONE);
                    Toast.makeText(OnlineMediaPreviewActivity.this, getString(R.string.error_download_failed), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws java.io.IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() -> { if (actionOverlay != null) actionOverlay.setVisibility(View.GONE); });
                    return;
                }

                long totalBytes = response.body().contentLength();
                File tempFile = new File(getCacheDir(), fileName);

                try (okio.BufferedSink sink = okio.Okio.buffer(okio.Okio.sink(tempFile));
                     okio.BufferedSource source = response.body().source()) {

                    long bytesRead = 0;
                    long read;
                    int previousCalculatedProgress = -1;

                    while ((read = source.read(sink.buffer(), 8192)) != -1) {
                        bytesRead += read;

                        final int progress = totalBytes > 0 ? (int) ((bytesRead * 100) / totalBytes) : 0;

                        if (progress != previousCalculatedProgress && progress <= 100) {
                            previousCalculatedProgress = progress;
                            final int finalProgress = progress;

                            if (!isFinishing()) {
                                runOnUiThread(() -> {
                                    if (actionProgress != null) {
                                        actionProgress.setProgress(finalProgress);
                                        tvActionPercent.setText(finalProgress + "%");
                                    }
                                });
                            }
                        }
                    }

                    sink.flush();

                    lastProgress = previousCalculatedProgress;

                    MediaStatusUtils.saveToGallery(OnlineMediaPreviewActivity.this, Uri.fromFile(tempFile), null, fileName, item.isVideo(), 100, (success, savedUri) -> {
                        if (success && !isFinishing()) {
                            runOnUiThread(() -> finalizeSmoothDownload(item, downloadUrl));
                        } else {
                            runOnUiThread(() -> { if (actionOverlay != null) actionOverlay.setVisibility(View.GONE); });
                        }
                    });

                } catch (Exception e) {
                    Log.e("DOWNLOAD_ERROR", "Stream error: " + e.getMessage());
                    runOnUiThread(() -> { if (actionOverlay != null) actionOverlay.setVisibility(View.GONE); });
                } finally {
                    response.close();
                }
            }
        });
    }


    private void finalizeSmoothDownload(MediaItem item, String url) {
        if (isFinishing() || isDestroyed()) return;

        ValueAnimator finalAnim = ValueAnimator.ofInt(lastProgress, 100);
        finalAnim.setDuration(600);
        finalAnim.addUpdateListener(animation -> {
            if (!isFinishing() && actionProgress != null) {
                int val = (int) animation.getAnimatedValue();
                actionProgress.setProgress(val);
                tvActionPercent.setText(val + "%");
            }
        });
        finalAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isFinishing()) return;

                item.setDownloaded(true);
                viewModel.updateDownloadStatus(url, true);
                updateActionButtons(item);

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (actionOverlay != null && !isFinishing()) {
                        actionOverlay.animate().alpha(0f).setDuration(400).withEndAction(() -> {
                            actionOverlay.setVisibility(View.GONE);
                          }).start();
                    }
                }, 600);
            }
        });
        finalAnim.start();
    }
    private void setupViewPager() {
        adapter = new OnlineMediaPagerAdapter(this, localMediaList);

        adapter.setOnItemClickListener(this::toggleUI);

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

    private void updateActionButtons(MediaItem item) {
        ImageView downloadIcon = findViewById(R.id.downloadActionButton);
        TextView saveText = findViewById(R.id.saveText);
        if (item.isDownloaded()) {
            if (downloadIcon != null) {
                downloadIcon.setImageResource(R.drawable.ic_check_circle);
                downloadIcon.setAlpha(0.7f);
            }
            if (saveText != null) {
                saveText.setText(getString(R.string.action_saved));
                saveText.setTextColor(getResources().getColor(R.color.premium_gold));
            }
        } else {
            if (downloadIcon != null) {
                downloadIcon.setImageResource(R.drawable.ic_download);
                downloadIcon.setAlpha(1.0f);
            }
            if (saveText != null) {
                saveText.setText(getString(R.string.action_save));
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
        if (viewPager != null) {
            viewPager.setAdapter(null);
        }

        if (actionOverlay != null) {
            actionOverlay.clearAnimation();
        }
        if (findViewById(R.id.adContainer) != null) {
            findViewById(R.id.adContainer).clearAnimation();
        }
        if (mAdView != null) {
            try {
                mAdView.setAdListener(null);
                mAdView.destroy();
                mAdView = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            Glide.with(getApplicationContext()).pauseRequests();
        } catch (Exception e) {
        }

        super.onDestroy();
    }
}