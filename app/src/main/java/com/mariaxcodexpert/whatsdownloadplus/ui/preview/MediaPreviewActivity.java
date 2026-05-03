package com.mariaxcodexpert.whatsdownloadplus.ui.preview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.mariaxcodexpert.whatsdownloadplus.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.SmartNotify;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageDao;
import com.mariaxcodexpert.whatsdownloadplus.data.local.ImagesEntity.ImageEntity;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoDao;
import com.mariaxcodexpert.whatsdownloadplus.data.local.VideosEntity.VideoEntity;
import com.mariaxcodexpert.whatsdownloadplus.ui.gallery.GalleryViewModel;
import com.mariaxcodexpert.whatsdownloadplus.ui.magic_lab.MagicLabActivity;
import com.mariaxcodexpert.whatsdownloadplus.ui.utils.media.HDConverter;
import com.mariaxcodexpert.whatsdownloadplus.ui.utils.media.MediaStatusUtils;
import com.mariaxcodexpert.whatsdownloadplus.ui.utils.media.PROConverter;
import com.mariaxcodexpert.whatsdownloadplus.ShakeDetector;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@OptIn(markerClass = UnstableApi.class)
public class MediaPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_URI = "EXTRA_URI", EXTRA_IS_VIDEO = "EXTRA_IS_VIDEO",
            EXTRA_FILE_NAME = "EXTRA_FILE_NAME", EXTRA_IS_DOWNLOADED = "EXTRA_IS_DOWNLOADED";
    private static final String PREF_NAME = "DownloadPrefs", KEY_ALWAYS_ORIGINAL = "always_original";

    private Object currentMediaItem;
    private ImageView btnSaveIcon, btnDownloadStatus;
    private LinearLayout cardCrop, cardSave, cardInfo;
    private boolean isVideo, isDownloadedCurrent;
    private Uri mediaUri;
    private String fileName;
    private GalleryViewModel viewModel;
    private ViewPager2 viewPagerMedia;

    private View loaderContainer;
    private ProgressBar circleProgress;
    private TextView tvPercent, tvLabel;

    private int lastProgress = 0;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    private ImageDao imageDao; // 🔥 Add this
    private VideoDao videoDao; // 🔥 Add this
    private final java.util.concurrent.ExecutorService executor = com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase.databaseWriteExecutor; // 🔥 Add this
    private final ActivityResultLauncher<Intent> magicLabLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (MagicLabActivity.finalEditedResult != null) {
                        startFinalSave(-2, 0);
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_video_preview_activity);

        viewModel = new ViewModelProvider(this).get(GalleryViewModel.class);
// 🔥 Database initialization add krain
        com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase db =
                com.mariaxcodexpert.whatsdownloadplus.data.local.Database.AppDatabase.getInstance(this);
        imageDao = db.imageDao();
        videoDao = db.videoDao();

        isVideo = getIntent().getBooleanExtra(EXTRA_IS_VIDEO, false);
        fileName = getIntent().getStringExtra(EXTRA_FILE_NAME);
        String uriStr = getIntent().getStringExtra(EXTRA_URI);
        if (uriStr != null) mediaUri = Uri.parse(uriStr);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();

        mShakeDetector.setOnShakeListener(() -> {
            if (!isDownloadedCurrent) {
                vibrateOnShake();
                performSaveAction();
            } else {
                SmartNotify.success(findViewById(android.R.id.content), "Status already saved! ✅");
            }
        });

        bindViews();

// onCreate ke andar check ko is tarah krain
        if (getIntent().hasExtra("MEDIA_PATH")) {
            String url = getIntent().getStringExtra("MEDIA_PATH");
            isVideo = getIntent().getBooleanExtra("IS_VIDEO", false);
            String videoUrl = getIntent().getStringExtra("VIDEO_URL");
            // Default false rakho, setupOnlinePreview khud check ker k true kery ga
            isDownloadedCurrent = false;
            setupOnlinePreview(url, videoUrl, isVideo);
        } else {
            // Local WhatsApp Status Logic
            isDownloadedCurrent = getIntent().getBooleanExtra(EXTRA_IS_DOWNLOADED, false);
            initMedia();
        }
      
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { finish(); }
        });
    }

    private void setupOnlinePreview(String url, String videoUrl, boolean isVid) {
        executor.execute(() -> {
            // 🔥 Wahi unique ID banayein jo Fragment mein h
            String downloadUrl = isVid ? (videoUrl != null ? videoUrl : url) : url;
            String uniqueId = String.valueOf(downloadUrl.hashCode());
            String extension = isVid ? ".mp4" : ".jpg";
            String targetFileName = "Pexels_" + uniqueId + extension;

            boolean alreadyDownloaded = false;
            Object existingItem = null;

            if (isVid) {
                existingItem = videoDao.getVideoByUri(downloadUrl);
                if (existingItem == null) existingItem = videoDao.getVideoByFileName(targetFileName);
            } else {
                existingItem = imageDao.getImageByUri(url);
                if (existingItem == null) existingItem = imageDao.getImageByFileName(targetFileName);
            }

            if (existingItem != null) {
                if (existingItem instanceof ImageEntity) alreadyDownloaded = ((ImageEntity) existingItem).isDownloaded;
                else alreadyDownloaded = ((VideoEntity) existingItem).isDownloaded;
            }

            final boolean finalDownloaded = alreadyDownloaded;
            final Object finalItem = existingItem;
            final String finalFileName = targetFileName;

            runOnUiThread(() -> {
                fileName = finalFileName; // Activity ka fileName update krain
                isDownloadedCurrent = finalDownloaded;
                List<Object> onlineList = new ArrayList<>();

                if (finalDownloaded) {
                    currentMediaItem = finalItem;
                    onlineList.add(finalItem);
                } else {
                    if (isVid) {
                        VideoEntity v = new VideoEntity(finalFileName, url, "", System.currentTimeMillis(), false, 0);
                        v.setUri(downloadUrl);
                        currentMediaItem = v;
                        onlineList.add(v);
                    } else {
                        ImageEntity img = new ImageEntity(finalFileName, url, "", System.currentTimeMillis(), false, 0);
                        currentMediaItem = img;
                        onlineList.add(img);
                    }
                }
                setupPager(onlineList);
                updateUIState();
            });
        });
    }

    private void bindViews() {
        viewPagerMedia = findViewById(R.id.viewPagerMedia);
        btnSaveIcon = findViewById(R.id.btnSave);
        btnDownloadStatus = findViewById(R.id.downloadStatus);
        cardSave = findViewById(R.id.cardSave);
        cardCrop = findViewById(R.id.cardCrop);
        cardInfo = findViewById(R.id.cardInfo);

        loaderContainer = findViewById(R.id.loaderContainer);
        circleProgress = findViewById(R.id.exportProgressBar);
        tvPercent = findViewById(R.id.tvPercentage);
        tvLabel = findViewById(R.id.tvQualityLabel);

        viewPagerMedia.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageScrollStateChanged(int s) {
                if (s == ViewPager2.SCROLL_STATE_IDLE && viewPagerMedia.getAdapter() != null)
                    ((PreviewPagerAdapter) viewPagerMedia.getAdapter()).handlePlayback(viewPagerMedia.getCurrentItem());
            }
            @Override public void onPageSelected(int p) {
                PreviewPagerAdapter adapter = (PreviewPagerAdapter) viewPagerMedia.getAdapter();
                if (adapter != null && adapter.getData().size() > p) syncUIMetadata(adapter.getData().get(p));
            }
        });

        findViewById(R.id.btnClosePreview).setOnClickListener(v -> finish());
        findViewById(R.id.cardShare).setOnClickListener(v -> MediaStatusUtils.shareMedia(this, mediaUri, isVideo));

        cardCrop.setOnClickListener(v -> {
            if (isDownloadedCurrent) {
                SmartNotify.error(v, "Magic Lab unavailable after download!");
            } else if (isVideo) {
                SmartNotify.success(v, "Unlocking Next-Gen Video AI soon. 🔥");
            } else {
                startMagicLab();
            }
        });

        cardSave.setOnClickListener(v -> { if(!isDownloadedCurrent) performSaveAction(); else SmartNotify.success(v, "Already in Gallery! ✅"); });
        cardInfo.setOnClickListener(v -> showInfoDialog());
    }

    private void performSaveAction() {
        pauseVideo();
        boolean alwaysOriginal = getSharedPreferences(PREF_NAME, MODE_PRIVATE).getBoolean(KEY_ALWAYS_ORIGINAL, false);
        if (isVideo || alwaysOriginal) {
            showAd(() -> startFinalSave(0, 0));
            return;
        }

        View v = getLayoutInflater().inflate(R.layout.dialog_download_choice, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(v).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        CheckBox cbAlways = v.findViewById(R.id.cbDoNotShow);
        v.findViewById(R.id.btnMagicLab).setOnClickListener(view -> { dialog.dismiss(); startMagicLab(); });
        v.findViewById(R.id.btnOriginal).setOnClickListener(view -> {
            dialog.dismiss();
            if (cbAlways.isChecked()) getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit().putBoolean(KEY_ALWAYS_ORIGINAL, true).apply();
            showAd(() -> startFinalSave(0, 0));
        });
        v.findViewById(R.id.btnClose).setOnClickListener(view -> dialog.dismiss());
        dialog.show();
    }

    private void startFinalSave(int quality, int style) {
        runOnUiThread(() -> {
            lastProgress = 0;
            if (loaderContainer != null) {
                loaderContainer.setVisibility(View.VISIBLE);
                loaderContainer.setAlpha(0f);
                loaderContainer.animate().alpha(1.0f).setDuration(300).start();
            }
            updateProgressUI(0, quality == -2 ? "Saving Masterpiece..." : "Initializing AI Engine...");
        });

        new Thread(() -> {
            try {
                if (isVideo) {
                    updateProgressUI(50, "Saving Video...");
                    MediaStatusUtils.saveToGallery(this, mediaUri, null, fileName, true, 100, (s, u) -> {
                        if (s) finalizeDownload(u); else handleError();
                    });
                } else {
                    saveImageLogic(quality, style);
                }
            } catch (Exception e) {
                handleError();
            }
        }).start();
    }

    private void saveImageLogic(int q, int s) {
        new Thread(() -> {
            Bitmap resultBitmap = null;
            try {
                if (q == -2) {
                    resultBitmap = MagicLabActivity.finalEditedResult;
                    updateProgressUI(90, "Finalizing Masterpiece...");
                } else if (q == 0) {
                    updateProgressUI(20, "Fetching Image...");

                    // 🔥 CHECK: Agar URL internet wala h
                    if (mediaUri.toString().startsWith("http")) {
                        try {
                            resultBitmap = Glide.with(this)
                                    .asBitmap()
                                    .load(mediaUri.toString())
                                    .submit()
                                    .get(); // Background thread h isliye .get() chalay ga
                        } catch (Exception e) {
                            Log.e("PREVIEW_SAVE", "Glide download failed", e);
                        }
                    } else {
                        // Local WhatsApp file logic
                        try (InputStream is = getContentResolver().openInputStream(mediaUri)) {
                            resultBitmap = BitmapFactory.decodeStream(is);
                        }
                    }
                    updateProgressUI(80, "Decoding Image...");
                } else if (q == 1) {
                    // Aapka HDConverter logic...
                    resultBitmap = HDConverter.process8KExport(this, mediaUri, 0, 0, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 0, false, false, this::updateProgressUI);
                } else {
                    resultBitmap = PROConverter.processToIPhoneQuality(this, mediaUri, s, p -> updateProgressUI(p, "Applying Pro Filters..."));
                }

                if (resultBitmap != null) {
                    final Bitmap bitmapToSave = resultBitmap;
                    updateProgressUI(95, "Saving to Gallery...");
                    MediaStatusUtils.saveToGallery(this, mediaUri, bitmapToSave, fileName, false, 100, (success, uri) -> {
                        runOnUiThread(() -> {
                            if (success) finalizeDownload(uri);
                            else handleErrordata("Save Failed");

                            if (q == -2) MagicLabActivity.finalEditedResult = null;
                            if (bitmapToSave != null && !bitmapToSave.isRecycled() && q != -2) bitmapToSave.recycle();
                        });
                    });
                } else {
                    handleErrordata("Media Engine Error");
                }
            } catch (Exception | OutOfMemoryError e) {
                handleErrordata("Memory Error: Processing failed");
            }
        }).start();
    }
    private void updateProgressUI(int targetProgress, String status) {
        runOnUiThread(() -> {
            if (circleProgress == null || tvPercent == null) return;

            ValueAnimator animator = ValueAnimator.ofInt(lastProgress, targetProgress);
            animator.setDuration(500);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                int val = (int) animation.getAnimatedValue();
                circleProgress.setProgress(val);
                tvPercent.setText(val + "%");
            });
            animator.start();
            lastProgress = targetProgress;

            if (tvLabel != null) {
                tvLabel.setText(status != null && !status.isEmpty() ? status : (targetProgress < 100 ? "Processing..." : "Finalizing..."));
            }
        });
    }

    private void finalizeDownload(Uri u) {
        runOnUiThread(() -> {
            ValueAnimator finalAnim = ValueAnimator.ofInt(lastProgress, 100);
            finalAnim.setDuration(500);
            finalAnim.addUpdateListener(animation -> {
                int val = (int) animation.getAnimatedValue();
                circleProgress.setProgress(val);
                tvPercent.setText(val + "%");
            });
            finalAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (tvLabel != null) tvLabel.setText("Status Successfully Saved!");

                    // Database aur UI Sync logic
                    executor.execute(() -> {
                        String galleryPath = u.toString();
                        long currentTime = System.currentTimeMillis();

                        if (currentMediaItem instanceof ImageEntity) {
                            ImageEntity img = (ImageEntity) currentMediaItem;
                            img.isDownloaded = true; // 🔥 Object update for UI
                            img.gallery_path = galleryPath;
                            img.downloadTime = currentTime;

                            imageDao.insertImage(img); // DB update

                            // ViewModel ko notify krain take Gallery screen update ho
                            runOnUiThread(() -> {
                                if (viewModel != null) viewModel.markImageDownloaded(img, galleryPath);
                            });

                        } else if (currentMediaItem instanceof VideoEntity) {
                            VideoEntity vid = (VideoEntity) currentMediaItem;
                            vid.isDownloaded = true; // 🔥 Object update for UI
                            vid.gallery_path = galleryPath;
                            vid.downloadTime = currentTime;

                            videoDao.insertVideo(vid); // DB update

                            // ViewModel ko notify krain
                            runOnUiThread(() -> {
                                if (viewModel != null) viewModel.markVideoDownloaded(vid, galleryPath);
                            });
                        }

                        // Foran icon badalne k liye runOnUiThread use krain
                        runOnUiThread(() -> {
                            isDownloadedCurrent = true;
                            updateUIState(); // 🔥 Ab ye icon change kar dega
                        });
                    });

                    mainHandler.postDelayed(() -> {
                        if (loaderContainer != null) {
                            loaderContainer.animate().alpha(0f).setDuration(400).withEndAction(() -> {
                                loaderContainer.setVisibility(View.GONE);
                                lastProgress = 0;
                            }).start();
                        }
                    }, 1200);
                }
            });
            finalAnim.start();
        });
    }

    private void syncUIMetadata(@NonNull Object item) {
        currentMediaItem = item;
        isVideo = item instanceof VideoEntity;

        mediaUri = Uri.parse(isVideo ? ((VideoEntity)item).getUri() : ((ImageEntity)item).getUri());
        fileName = isVideo ? ((VideoEntity)item).fileName : ((ImageEntity)item).fileName;

        // Initial status purane object se
        isDownloadedCurrent = isVideo ? ((VideoEntity)item).isDownloaded : ((ImageEntity)item).isDownloaded;

        // 🔥 ALTERNATE: Existing methods use karte hue
        executor.execute(() -> {
            boolean freshStatus = false;
            if (isVideo) {
                // VideoDao mein bhi getImageByFileName jaisa method hoga
                VideoEntity freshVid = videoDao.getVideoByFileName(fileName);
                if (freshVid != null) freshStatus = freshVid.isDownloaded;
            } else {
                // Aapke ImageDao mein ye method maujood h
                ImageEntity freshImg = imageDao.getImageByFileName(fileName);
                if (freshImg != null) freshStatus = freshImg.isDownloaded;
            }

            if (freshStatus != isDownloadedCurrent) {
                final boolean statusToUpdate = freshStatus;
                runOnUiThread(() -> {
                    isDownloadedCurrent = statusToUpdate;

                    // Current object ko update krain taake sync rahy
                    if (currentMediaItem instanceof ImageEntity) ((ImageEntity) currentMediaItem).isDownloaded = statusToUpdate;
                    else if (currentMediaItem instanceof VideoEntity) ((VideoEntity) currentMediaItem).isDownloaded = statusToUpdate;

                    updateUIState();
                });
            }
        });

        updateUIState();
    }
    private void updateUIState() {
        if (btnSaveIcon == null || btnDownloadStatus == null) return;
        btnSaveIcon.setVisibility(isDownloadedCurrent ? View.GONE : View.VISIBLE);
        btnDownloadStatus.setVisibility(isDownloadedCurrent ? View.VISIBLE : View.GONE);
        if (isDownloadedCurrent) btnDownloadStatus.setImageResource(R.drawable.ic_double_tick);
        cardSave.setAlpha(isDownloadedCurrent ? 0.5f : 1.0f);
        cardCrop.setAlpha(isDownloadedCurrent ? 0.5f : 1.0f);
    }

    private void pauseVideo() {
        if (viewPagerMedia != null && viewPagerMedia.getAdapter() != null) {
            ((PreviewPagerAdapter) viewPagerMedia.getAdapter()).handlePlayback(-1);
        }
    }

    private void vibrateOnShake() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void showInfoDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_file_info, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(v).create();
        TextView tvName = v.findViewById(R.id.infoName);
        TextView tvType = v.findViewById(R.id.infoType);
        TextView tvStatus = v.findViewById(R.id.infoStatus);
        TextView tvPath = v.findViewById(R.id.infoPath);
        TextView tvGalleryPath = v.findViewById(R.id.infoGalleryPath);
        TextView tvDate = v.findViewById(R.id.infoDate);
        View layoutGallery = v.findViewById(R.id.layoutGallery);

        tvName.setText("Name: " + fileName);
        tvType.setText("Type: " + (isVideo ? "Video" : "Image"));
        if (isDownloadedCurrent) {
            tvStatus.setText("Status: Downloaded");
            tvStatus.setTextColor(android.graphics.Color.parseColor("#25D366"));
        } else {
            tvStatus.setText("Status: Pending");
            tvStatus.setTextColor(android.graphics.Color.parseColor("#FF9800"));
        }
        tvPath.setText("WA: " + mediaUri.getPath());

        String gPath = (currentMediaItem instanceof ImageEntity) ? ((ImageEntity) currentMediaItem).gallery_path : ((VideoEntity) currentMediaItem).gallery_path;
        if (isDownloadedCurrent && gPath != null && !gPath.isEmpty()) {
            layoutGallery.setVisibility(View.VISIBLE);
            tvGalleryPath.setText("Gallery: " + gPath);
        } else layoutGallery.setVisibility(View.GONE);

        long timestamp = (currentMediaItem instanceof ImageEntity) ? ((ImageEntity) currentMediaItem).downloadTime : ((VideoEntity) currentMediaItem).downloadTime;
        if (timestamp > 0) {
            String dateFormatted = new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(timestamp));
            tvDate.setText("Captured on: " + dateFormatted);
        } else tvDate.setText("Date: Not available");

        v.findViewById(R.id.btnClose).setOnClickListener(view -> dialog.dismiss());
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void initMedia() {
        viewModel.getUiState().observe(this, state -> {
            if (viewPagerMedia.getAdapter() == null && !state.isLoading && state.data != null && !state.data.isEmpty()) {
                setupPager(new ArrayList<>(state.data));
            }
        });

        String uriStr = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("statusFolderUri", null);
        if (uriStr != null) {
            viewModel.loadStatuses(this, Uri.parse(uriStr), false, isVideo);
        }
    }

    private void setupPager(List<Object> list) {
        viewPagerMedia.setAdapter(new PreviewPagerAdapter(this, list));
        int pos = 0;
        for (int i = 0; i < list.size(); i++) {
            String name = (list.get(i) instanceof ImageEntity) ? ((ImageEntity) list.get(i)).fileName : ((VideoEntity) list.get(i)).fileName;
            if (fileName != null && fileName.equals(name)) { pos = i; break; }
        }
        viewPagerMedia.setCurrentItem(pos, false);
        syncUIMetadata(list.get(pos));
        mainHandler.postDelayed(() -> { if (viewPagerMedia.getAdapter() != null) ((PreviewPagerAdapter) viewPagerMedia.getAdapter()).handlePlayback(viewPagerMedia.getCurrentItem()); }, 500);
    }

    private void showAd(Runnable action) {
        pauseVideo();
        if (AdManager.isInterstitialLoaded()) {
            AdManager.showInterstitial(this, new AdManager.AdCallback() {
                @Override public void onAdClosed() { action.run(); }
                @Override public void onAdFailed() { action.run(); }
            });
        } else action.run();
    }

    private void startMagicLab() {
        pauseVideo();
        magicLabLauncher.launch(new Intent(this, MagicLabActivity.class)
                .putExtra("MEDIA_URI", mediaUri.toString())
                .putExtra("FILE_NAME", fileName));
    }

    private void handleErrordata(String reason) {
        runOnUiThread(() -> {
            if (loaderContainer != null) loaderContainer.setVisibility(View.GONE);
            SmartNotify.error(findViewById(android.R.id.content), reason);
        });
    }

    private void handleError() { handleErrordata("Download Failed!"); }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSensorManager != null && mAccelerometer != null) {
            mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mShakeDetector);
        }
        super.onPause();
        pauseVideo();
    }

    @Override protected void onDestroy() {
        if (viewPagerMedia != null && viewPagerMedia.getAdapter() != null) ((PreviewPagerAdapter) viewPagerMedia.getAdapter()).releaseAll();
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}