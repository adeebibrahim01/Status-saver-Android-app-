package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.animation.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.gms.ads.AdView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.mariaxcodexpert.whatsdownloadplus.*;
import com.mariaxcodexpert.whatsdownloadplus.Ads.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity;

import java.util.*;

@OptIn(markerClass = UnstableApi.class)
public class FullScreenMediaActivity extends AppCompatActivity {
    public static final String EXTRA_LIST = "extra_media_list", EXTRA_POS = "extra_position";
    public static final int RESULT_DELETED = 101;

    private ViewPager2 viewPager;
    private MediaPagerAdapter adapter;
    private List<Object> mediaList = new ArrayList<>();
    private int currentPosition = 0;
    private View root, delOverlay, bottomActions, closeBtn, adCont;
    private AdView adView;
    private CircularProgressIndicator delProgress;
    private TextView tvDelPercent;
    private DownloadViewModel viewModel;
    private boolean isUiVisible = true;

    private final ActivityResultLauncher<IntentSenderRequest> delLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(), r -> {
                if (r.getResultCode() == RESULT_OK && viewModel != null) viewModel.completePendingDelete();
                else if (delOverlay != null) delOverlay.setVisibility(View.GONE);
            });

    @Override
    protected void onCreate(@Nullable Bundle sav) {
        // Full screen setup
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        super.onCreate(sav);
        setContentView(R.layout.activity_full_screen_media);

        viewModel = new ViewModelProvider(this).get(DownloadViewModel.class);

        if (!handleIntent()) {
            finish();
            return;
        }

        initUI();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { kill(); finish(); }
        });

        if (adView != null) AdManager.loadBannerAd(this, adView);
    }

    private void showNotice(String msg) {
        if (root != null) {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }
    @SuppressWarnings("unchecked")
    private boolean handleIntent() {
        try {
            Intent in = getIntent();
            if (in == null) return false;

            String[] lKeys = {EXTRA_LIST, "extra_media_list", "media_list", "EXTRA_MEDIA_LIST"};
            String[] pKeys = {EXTRA_POS, "extra_position", "position", "EXTRA_POSITION"};

            for (String k : lKeys) {
                if (in.hasExtra(k)) {
                    Object s = in.getSerializableExtra(k);
                    if (s instanceof List) {
                        mediaList = new ArrayList<>((List<Object>) s);
                    }
                    break;
                }
            }

            for (String k : pKeys) {
                if (in.hasExtra(k)) {
                    currentPosition = in.getIntExtra(k, 0);
                    break;
                }
            }

            return mediaList != null && !mediaList.isEmpty() && currentPosition < mediaList.size();

        } catch (Exception e) {
            Log.e("INTENT_ERR", "Error parsing intent data: " + e.getMessage());
            return false;
        }
    }

    private void initUI() {
        root = findViewById(android.R.id.content);
        viewPager = findViewById(R.id.mediaViewPager);
        bottomActions = findViewById(R.id.bottomActions);
        closeBtn = findViewById(R.id.closeButton);
        adView = findViewById(R.id.adView);
        adCont = findViewById(R.id.adContainer);
        delOverlay = findViewById(R.id.deleteOverlay);
        delProgress = findViewById(R.id.deleteProgress);
        tvDelPercent = findViewById(R.id.tvDeletePercent);
        adapter = new MediaPagerAdapter(this, mediaList, this::toggleUI);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentPosition, false);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int p) {
                currentPosition = p;
                if (adapter != null) {
                    adapter.pauseAll();
                    adapter.handlePlayback(p);
                }
            }
        });

        viewPager.postDelayed(() -> {
            if (!isFinishing() && adapter != null) adapter.handlePlayback(currentPosition);
        }, 400);

        viewModel.deleteSuccess.observe(this, ok -> {
            if (Boolean.TRUE.equals(ok)) onDeleted();
        });

        viewModel.permissionIntent.observe(this, pi -> {
            if (pi != null) {
                delLauncher.launch(new IntentSenderRequest.Builder(pi).build());
                viewModel.clearPermissionIntent();
            }
        });

        closeBtn.setOnClickListener(v -> { kill(); finish(); });
        findViewById(R.id.cardShareFull).setOnClickListener(v -> share(false));
        findViewById(R.id.cardRepost).setOnClickListener(v -> share(true));
        findViewById(R.id.cardDelete).setOnClickListener(v -> processDelete());
    }

    private void processDelete() {
        if (mediaList == null || currentPosition < 0 || currentPosition >= mediaList.size()) {
            showNotice(getString(R.string.error_item_unavailable));
            return;
        }

        if (adapter != null) adapter.pauseAll();

        AdManager.showInterstitial(this, new AdManager.AdCallback() {
            @Override public void onAdClosed() { startDelAnim(); }
            @Override public void onAdFailed() { startDelAnim(); }
        });
    }

    private void startDelAnim() {
        if (mediaList == null || currentPosition >= mediaList.size()) return;

        if (delOverlay == null) {
            executeViewModelDelete();
            return;
        }

        delOverlay.setVisibility(View.VISIBLE);
        delOverlay.setAlpha(0f);
        delOverlay.animate().alpha(1f).setDuration(200);

        ValueAnimator anim = ValueAnimator.ofInt(0, 100).setDuration(600);
        anim.addUpdateListener(a -> {
            int v = (int) a.getAnimatedValue();
            if (delProgress != null) delProgress.setProgress(v);
            if (tvDelPercent != null) tvDelPercent.setText(v + "%");
        });

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                executeViewModelDelete();
            }
        });
        anim.start();
    }

    private void executeViewModelDelete() {
        try {
            if (viewModel != null && currentPosition < mediaList.size()) {
                viewModel.deleteFile(mediaList.get(currentPosition));
            }
        } catch (Exception e) {
            if (delOverlay != null) delOverlay.setVisibility(View.GONE);
            showNotice(getString(R.string.error_delete_failed));
        }
    }
    private void onDeleted() {
        try {
            if (adapter != null) {
                adapter.removeItem(currentPosition);
            }

            if (delOverlay != null) {
                delOverlay.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction(() -> delOverlay.setVisibility(View.GONE));
            }

            setResult(RESULT_DELETED);

            if (currentPosition >= 0 && currentPosition < mediaList.size()) {
                mediaList.remove(currentPosition);

                if (adapter != null) {
                    adapter.notifyItemRemoved(currentPosition);
                    adapter.notifyItemRangeChanged(currentPosition, mediaList.size());
                }
            }

            if (mediaList.isEmpty()) {
                kill();
                finish();
            } else {
                if (currentPosition >= mediaList.size()) {
                    currentPosition = mediaList.size() - 1;
                }

                viewPager.postDelayed(() -> {
                    if (!isFinishing() && adapter != null) {
                        adapter.pauseAll();
                        adapter.handlePlayback(currentPosition);
                    }
                }, 350);
            }
        } catch (Exception e) {
            Log.e("DELETE_UI_ERR", "Error updating UI after delete: " + e.getMessage());
            kill();
            finish();
        }
    }
    private void share(boolean repost) {
        if (mediaList == null || mediaList.isEmpty() || currentPosition >= mediaList.size()) {
            showNotice(getString(R.string.error_file_not_ready_share));
            return;
        }

        try {
            Object item = mediaList.get(currentPosition);
            String path = "";
            boolean isVid = false;

            if (item instanceof ImageEntity) {
                ImageEntity img = (ImageEntity) item;
                path = img.isDownloaded ? img.gallery_path : img.getUri();
            } else if (item instanceof VideoEntity) {
                VideoEntity vid = (VideoEntity) item;
                path = vid.isDownloaded ? vid.gallery_path : vid.getUri();
                isVid = true;
            }

            if (path == null || path.isEmpty()) {
                showNotice(getString(R.string.error_path_not_found));
                return;
            }

            Uri mediaUri = Uri.parse(path);
            Intent intent = new Intent(Intent.ACTION_SEND)
                    .setType(isVid ? "video/*" : "image/*")
                    .putExtra(Intent.EXTRA_STREAM, mediaUri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (repost) {
                intent.setPackage("com.whatsapp");
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    try {
                        intent.setPackage("com.whatsapp.w4b");
                        startActivity(intent);
                    } catch (Exception e2) {
                        showNotice(getString(R.string.toast_whatsapp_not_installed));
                    }
                }
            } else {
                startActivity(Intent.createChooser(intent, getString(R.string.share_chooser_title)));
            }
        } catch (Exception e) {
            showNotice(getString(R.string.error_share_failed));
        }
    }
    private void toggleUI() {
        isUiVisible = !isUiVisible;
        float alpha = isUiVisible ? 1f : 0f;

         View decorView = getWindow().getDecorView();
        int uiOptions = isUiVisible ? View.SYSTEM_UI_FLAG_VISIBLE :
                (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        decorView.setSystemUiVisibility(uiOptions);

         if (adView != null) {
            if (isUiVisible) {
                adView.resume();
            } else {
                adView.pause();
            }
        }

        View[] targets = {bottomActions, closeBtn, adCont};
        for (View v : targets) {
            if (v != null) {
                v.animate()
                        .alpha(alpha)
                        .setDuration(250)
                        .withStartAction(() -> {
                            if (isUiVisible) v.setVisibility(View.VISIBLE);
                        })
                        .withEndAction(() -> {
                            if (!isUiVisible) v.setVisibility(View.GONE);
                        })
                        .start();
            }
        }
    }

    private void kill() {
        if (adapter != null) adapter.releaseAll();
        if (viewPager != null) viewPager.setAdapter(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adapter != null) adapter.pauseAll();
        if (adView != null) adView.pause();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) adView.resume();
    }

    @Override
    protected void onDestroy() {
        kill();
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
        super.onDestroy();
    }
}