package com.mariaxcodexpert.whatsdownloadplus.ui.Download;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.ads.AdView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.mariaxcodexpert.whatsdownloadplus.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.SmartNotify;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity;
import com.mariaxcodexpert.whatsdownloadplus.ShakeDetector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@OptIn(markerClass = UnstableApi.class)
public class FullScreenMediaActivity extends AppCompatActivity {

    public static final String EXTRA_LIST = MediaListAdapter.EXTRA_MEDIA_LIST;
    public static final String EXTRA_POS = MediaListAdapter.EXTRA_POSITION;
    public static final int RESULT_DELETED = 101;

    private ViewPager2 viewPager;
    private MediaPagerAdapter adapter;
    private List<Object> mediaList = new ArrayList<>();
    private int currentPosition = 0;

    private LinearLayout bottomActions;
    private View rootContainer, closeButton, adContainer;
    private AdView adView;

    private View deleteOverlay;
    private CircularProgressIndicator deleteProgress;
    private TextView tvDeletePercent;

    private boolean isUiVisible = true;
    private DownloadViewModel viewModel;

    // Shake Feature Variables
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;

    private final ActivityResultLauncher<IntentSenderRequest> deleteLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    if (viewModel != null) viewModel.completePendingDelete();
                } else {
                    if (deleteOverlay != null) deleteOverlay.setVisibility(View.GONE);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_media);

        viewModel = new ViewModelProvider(this).get(DownloadViewModel.class);

        handleIntentData();

        if (mediaList == null || mediaList.isEmpty()) {
            finish();
            return;
        }

        // Initialize Shake Sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();

        mShakeDetector.setOnShakeListener(() -> {
            // Shake hone par delete process trigger karein
            if (currentPosition >= 0 && currentPosition < mediaList.size() && (deleteOverlay == null || deleteOverlay.getVisibility() != View.VISIBLE)) {
                vibrateDevice();
                SmartNotify.success(rootContainer, "> GESTURE DETECTED: TERMINATING FILE...");
                processDeleteAction();
            }
        });

        initViews();
        setupViewPager();
        setupObservers();
        setupListeners();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                killEverything();
                finish();
            }
        });

        if (adView != null) AdManager.loadBannerAd(this, adView);
    }

    private void vibrateDevice() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(200);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handleIntentData() {
        Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(EXTRA_LIST)) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ArrayList<Serializable> serializableList = intent.getSerializableExtra(EXTRA_LIST, ArrayList.class);
                if (serializableList != null) {
                    mediaList = (List<Object>) (List<?>) serializableList;
                }
            } else {
                Serializable s = intent.getSerializableExtra(EXTRA_LIST);
                if (s instanceof ArrayList) {
                    mediaList = (List<Object>) s;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        currentPosition = intent.getIntExtra(EXTRA_POS, 0);
    }

    private void initViews() {
        rootContainer = findViewById(android.R.id.content);
        viewPager = findViewById(R.id.mediaViewPager);
        bottomActions = findViewById(R.id.bottomActions);
        closeButton = findViewById(R.id.closeButton);
        adView = findViewById(R.id.adView);
        adContainer = findViewById(R.id.adContainer);
        deleteOverlay = findViewById(R.id.deleteOverlay);
        deleteProgress = findViewById(R.id.deleteProgress);
        tvDeletePercent = findViewById(R.id.tvDeletePercent);
    }

    private void setupViewPager() {
        adapter = new MediaPagerAdapter(this, mediaList, this::toggleUI);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(1);
        viewPager.setCurrentItem(currentPosition, false);
        viewPager.registerOnPageChangeCallback(pageChangeCallback);

        viewPager.postDelayed(() -> {
            if (!isFinishing() && adapter != null) {
                adapter.handlePlayback(currentPosition);
            }
        }, 400);
    }

    private void setupObservers() {
        viewModel.deleteSuccess.observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                onDeleteProcessCompleted();
            }
        });

        viewModel.permissionIntent.observe(this, pendingIntent -> {
            if (pendingIntent != null) {
                IntentSenderRequest isr = new IntentSenderRequest.Builder(pendingIntent).build();
                deleteLauncher.launch(isr);
                viewModel.clearPermissionIntent();
            }
        });
    }

    private void setupListeners() {
        closeButton.setOnClickListener(v -> {
            killEverything();
            finish();
        });

        findViewById(R.id.cardShareFull).setOnClickListener(v -> shareMedia(false));
        findViewById(R.id.cardRepost).setOnClickListener(v -> shareMedia(true));

        findViewById(R.id.cardDelete).setOnClickListener(v -> {
            processDeleteAction();
        });
    }

    private void processDeleteAction() {
        if (currentPosition >= 0 && currentPosition < mediaList.size()) {
            // 1. Pehle video pause karo
            if (adapter != null) adapter.pauseAll();

            // 2. Interstitial dikhao
            AdManager.showInterstitial(this, new AdManager.AdCallback() {
                @Override
                public void onAdClosed() {
                    startDeleteAnimation();
                }

                @Override
                public void onAdFailed() {
                    startDeleteAnimation();
                }
            });
        }
    }

    private void startDeleteAnimation() {
        if (deleteOverlay == null) {
            viewModel.deleteFile(mediaList.get(currentPosition));
            return;
        }

        deleteOverlay.setVisibility(View.VISIBLE);
        deleteOverlay.setAlpha(0f);
        deleteOverlay.animate().alpha(1.0f).setDuration(200).start();

        ValueAnimator anim = ValueAnimator.ofInt(0, 100);
        anim.setDuration(600);
        anim.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            if (deleteProgress != null) deleteProgress.setProgress(val);
            if (tvDeletePercent != null) tvDeletePercent.setText(val + "%");
        });

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (currentPosition < mediaList.size()) {
                    if (adapter != null) adapter.releaseAll();
                    viewModel.deleteFile(mediaList.get(currentPosition));
                }
            }
        });
        anim.start();
    }

    private void onDeleteProcessCompleted() {
        if (deleteOverlay != null) {
            deleteOverlay.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                deleteOverlay.setVisibility(View.GONE);
            }).start();
        }

        setResult(RESULT_DELETED);

        if (currentPosition >= 0 && currentPosition < mediaList.size()) {
            mediaList.remove(currentPosition);
            adapter.notifyItemRemoved(currentPosition);
        }

        if (mediaList.isEmpty()) {
            finish();
        } else {
            if (currentPosition >= mediaList.size()) {
                currentPosition = mediaList.size() - 1;
            }

            viewPager.postDelayed(() -> {
                if (adapter != null && !isFinishing()) {
                    adapter.handlePlayback(currentPosition);
                }
            }, 300);
        }
    }

    private void shareMedia(boolean isRepost) {
        if (mediaList == null || mediaList.isEmpty() || currentPosition >= mediaList.size()) return;

        Object item = mediaList.get(currentPosition);
        String finalPath;
        boolean isVideo;

        if (item instanceof ImageEntity) {
            ImageEntity img = (ImageEntity) item;
            finalPath = (img.isDownloaded && img.gallery_path != null && !img.gallery_path.isEmpty())
                    ? img.gallery_path : img.getUri();
            isVideo = false;
        } else if (item instanceof VideoEntity) {
            VideoEntity vid = (VideoEntity) item;
            finalPath = (vid.isDownloaded && vid.gallery_path != null && !vid.gallery_path.isEmpty())
                    ? vid.gallery_path : vid.getUri();
            isVideo = true;
        } else return;

        if (finalPath == null || finalPath.isEmpty()) return;

        Uri mediaUri = Uri.parse(finalPath);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(isVideo ? "video/*" : "image/*");
        intent.putExtra(Intent.EXTRA_STREAM, mediaUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (isRepost) {
            intent.setPackage("com.whatsapp");
            try {
                startActivity(intent);
            } catch (Exception e) {
                SmartNotify.error(rootContainer, "WhatsApp not installed!");
            }
        } else {
            startActivity(Intent.createChooser(intent, "Share via:"));
        }
    }

    private final ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            currentPosition = position;
            if (adapter != null) adapter.handlePlayback(position);
        }
    };

    private void toggleUI() {
        isUiVisible = !isUiVisible;
        float alpha = isUiVisible ? 1f : 0f;

        if (!isUiVisible) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }

        View[] views = {bottomActions, closeButton, adContainer};
        for (View v : views) {
            if (v != null) {
                v.animate().alpha(alpha).setDuration(250)
                        .withStartAction(() -> { if (isUiVisible) v.setVisibility(View.VISIBLE); })
                        .withEndAction(() -> { if (!isUiVisible) v.setVisibility(View.GONE); })
                        .start();
            }
        }
    }

    private void killEverything() {
        if (adapter != null) adapter.releaseAll();
        if (viewPager != null) {
            viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
            viewPager.setAdapter(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register sensor
        if (mSensorManager != null && mAccelerometer != null) {
            mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        // Unregister sensor
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mShakeDetector);
        }
        super.onPause();
        if (adapter != null) adapter.pauseAll();
    }

    @Override
    protected void onDestroy() {
        killEverything();
        if (adView != null) adView.destroy();
        super.onDestroy();
    }
}