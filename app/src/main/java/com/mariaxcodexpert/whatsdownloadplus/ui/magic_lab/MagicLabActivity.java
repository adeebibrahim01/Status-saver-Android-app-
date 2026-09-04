package com.mariaxcodexpert.whatsdownloadplus.ui.magic_lab;

import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.mariaxcodexpert.whatsdownloadplus.R;
import com.mariaxcodexpert.whatsdownloadplus.ui.utils.media.HDConverter;
import com.mariaxcodexpert.whatsdownloadplus.Ads.AdManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MagicLabActivity extends AppCompatActivity {
    public static Bitmap finalEditedResult = null;
    private ImageView imgPreview;
    private View scanLine, loaderContainer, mainCard, seekbarContainer;
    private TextView tvPercent, activeToolName, tvStatusLabel;
    private ProgressBar neonProgressBar;
    private SeekBar mainSeekBar;
    private Uri mediaUri;
    private Bitmap originalBitmap, processedBitmap;
    private SeekBar seekSmooth, seekSharp, seekBright, seekContrast, seekSaturation, seekExposure, seekWarmth, seekTint;
    private SeekBar seekClarity, seekVibrance, seekHighlights, seekShadows, seekVignette;
    private CheckBox cbDenoise, cbHDR;
    private MaterialButtonToggleGroup toggleGroup;
    private Button btnSave;
    private int currentViewMode = 1;
    private boolean isProcessing = false;
    private final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(4);
    private final ColorMatrix baseMatrix = new ColorMatrix();
    private final ColorMatrix tempMatrix = new ColorMatrix();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magic_lab);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.whatsapp_dark_green));
        String uriStr = getIntent().getStringExtra("MEDIA_URI");
        if (uriStr != null) mediaUri = Uri.parse(uriStr);
        initViews();
        loadOriginal();
    }

    private void initViews() {
        imgPreview = findViewById(R.id.imgPreviewMain);
        scanLine = findViewById(R.id.scanLine);
        loaderContainer = findViewById(R.id.loaderContainer);
        tvPercent = findViewById(R.id.tvPercentage);
        tvStatusLabel = findViewById(R.id.tvStatusLabel);
        neonProgressBar = findViewById(R.id.customProgressBar);
        mainCard = findViewById(R.id.mainCard);
        btnSave = findViewById(R.id.btnDownloadSelected);
        activeToolName = findViewById(R.id.activeToolName);
        mainSeekBar = findViewById(R.id.mainSeekBar);
        seekbarContainer = findViewById(R.id.seekbarContainer);
        toggleGroup = findViewById(R.id.toggleGroupQuality);
        cbDenoise = findViewById(R.id.cbDenoise);
        cbHDR = findViewById(R.id.cbHDR);
        bindSeekBar(R.id.toolSmooth_hidden, s -> { seekSmooth = s; s.setProgress(0); });
        bindSeekBar(R.id.toolExposure_hidden, s -> { seekExposure = s; s.setProgress(50); });
        bindSeekBar(R.id.toolContrast_hidden, s -> { seekContrast = s; s.setProgress(50); });
        bindSeekBar(R.id.toolBrightness_hidden, s -> { seekBright = s; s.setProgress(50); });
        bindSeekBar(R.id.toolSaturation_hidden, s -> { seekSaturation = s; s.setProgress(50); });
        bindSeekBar(R.id.toolWarmth_hidden, s -> { seekWarmth = s; s.setProgress(50); });
        bindSeekBar(R.id.toolTint_hidden, s -> { seekTint = s; s.setProgress(50); });
        bindSeekBar(R.id.toolDetail_hidden, s -> { seekSharp = s; s.setProgress(50); });
        bindSeekBar(R.id.toolClarity_hidden, s -> { seekClarity = s; s.setProgress(50); });
        bindSeekBar(R.id.toolVibrance_hidden, s -> { seekVibrance = s; s.setProgress(50); });
        bindSeekBar(R.id.toolHighlights_hidden, s -> { seekHighlights = s; s.setProgress(50); });
        bindSeekBar(R.id.toolShadows_hidden, s -> { seekShadows = s; s.setProgress(50); });
        bindSeekBar(R.id.toolVignette_hidden, s -> { seekVignette = s; s.setProgress(0); });

        setupClickListeners();
        setupToggleLogic();
        switchTool(getString(R.string.tool_exposure_label), seekExposure);
    }

    private void setupClickListeners() {
        findViewById(R.id.btnCloseDialog).setOnClickListener(v -> finish());
        findViewById(R.id.btnResetManual).setOnClickListener(v -> resetSettings());

        btnSave.setOnClickListener(v -> AdManager.showInterstitial(this, new AdManager.AdCallback() {
            @Override public void onAdClosed() { saveImage4K(); }
            @Override public void onAdFailed() { saveImage4K(); }
        }));

        setToolListener(R.id.toolSmooth, getString(R.string.tool_smooth_label), seekSmooth);
        setToolListener(R.id.toolExposure, getString(R.string.tool_exposure_label), seekExposure);
        setToolListener(R.id.toolContrast, getString(R.string.tool_contrast_label), seekContrast);
        setToolListener(R.id.toolBrightness, getString(R.string.tool_brightness_label), seekBright);
        setToolListener(R.id.toolSaturation, getString(R.string.tool_saturation_label), seekSaturation);
        setToolListener(R.id.toolWarmth, getString(R.string.tool_warmth_label), seekWarmth);
        setToolListener(R.id.toolTint, getString(R.string.tool_tint_label), seekTint);
        setToolListener(R.id.toolDetail, getString(R.string.tool_detail_label), seekSharp);

        setToolListener(R.id.toolClarity, getString(R.string.tool_clarity_label), seekClarity);
        setToolListener(R.id.toolVibrance, getString(R.string.tool_vibrance_label), seekVibrance);
        setToolListener(R.id.toolHighlights, getString(R.string.tool_highlights_label), seekHighlights);
        setToolListener(R.id.toolShadows, getString(R.string.tool_shadows_label), seekShadows);
        setToolListener(R.id.toolVignette, getString(R.string.tool_vignette_label), seekVignette);

        cbDenoise.setOnCheckedChangeListener((b, isChecked) -> applyLivePreview(false));
        cbHDR.setOnCheckedChangeListener((b, isChecked) -> applyLivePreview(false));
    }

    private void setToolListener(int id, String name, SeekBar target) {
        View v = findViewById(id);
        if (v != null) v.setOnClickListener(view -> switchTool(name, target));
    }

    private void switchTool(String toolName, SeekBar targetSeekBar) {
        if (targetSeekBar == null) return;
        activeToolName.setText(toolName);
        mainSeekBar.setMax(targetSeekBar.getMax());
        mainSeekBar.setProgress(targetSeekBar.getProgress());

        mainSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int p, boolean f) {
                if (f) {
                    targetSeekBar.setProgress(p);
                    applyLivePreview(false);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void applyLivePreview(boolean withAnimation) {
        if (imgPreview == null || processedBitmap == null || currentViewMode == 0) return;

        try {
            float b = (seekBright != null ? seekBright.getProgress() - 50 : 0) * 1.5f;
            float c = (seekContrast != null ? seekContrast.getProgress() / 50f : 1.0f);
            float s = (seekSaturation != null ? seekSaturation.getProgress() / 50f : 1.0f);

            if (seekVibrance != null) {
                s *= (1.0f + (seekVibrance.getProgress() - 50) / 100f);
            }

            float e = (seekExposure != null ? seekExposure.getProgress() / 50f : 1.0f);

            if (cbHDR != null && cbHDR.isChecked()) {
                s *= 1.3f;
                b += 5f;
            }

            baseMatrix.reset();
            baseMatrix.setSaturation(s);

            float scale = c * e;
            float translate = b + (127.5f * (1.0f - scale));

            float[] mat = {
                    scale, 0, 0, 0, translate,
                    0, scale, 0, 0, translate,
                    0, 0, scale, 0, translate,
                    0, 0, 0, 1, 0
            };

            tempMatrix.set(mat);
            baseMatrix.postConcat(tempMatrix);

            imgPreview.setColorFilter(new ColorMatrixColorFilter(baseMatrix));

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (seekSmooth != null) {
                    float smoothRadius = seekSmooth.getProgress() / 150.0f;

                    if (smoothRadius > 0.01f) {
                        imgPreview.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(
                                smoothRadius,
                                smoothRadius,
                                android.graphics.Shader.TileMode.CLAMP
                        ));
                    } else {
                        imgPreview.setRenderEffect(null);
                    }
                }
            }

        } catch (Exception err) {
            android.util.Log.e("MagicLab_Preview", "Error applying live preview: " + err.getMessage());
        }

        if (withAnimation) {
            showScanAnimation();
        }
    }

    private void saveImage4K() {
        if (isProcessing || mediaUri == null || isFinishing()) return;

        isProcessing = true;
        if (loaderContainer != null) loaderContainer.setVisibility(View.VISIBLE);
        if (tvStatusLabel != null) tvStatusLabel.setVisibility(View.VISIBLE);

        backgroundExecutor.execute(() -> {
            try {
                final Bitmap result4K = HDConverter.process8KExport(this, mediaUri,
                        seekSmooth.getProgress(), seekSharp.getProgress(),
                        seekBright.getProgress(), seekContrast.getProgress(),
                        seekSaturation.getProgress(), seekExposure.getProgress(),
                        seekWarmth.getProgress(), seekTint.getProgress(),
                        seekClarity.getProgress(), seekVibrance.getProgress(),
                        seekHighlights.getProgress(), seekShadows.getProgress(),
                        seekVignette.getProgress(),
                        cbDenoise.isChecked(), cbHDR.isChecked(), (p, status) -> runOnUiThread(() -> {
                            // Crash Handle: UI updates should only happen if activity is alive
                            if (!isFinishing()) {
                                if (tvPercent != null) tvPercent.setText(p + "%");
                                if (tvStatusLabel != null) tvStatusLabel.setText(status);
                                if (neonProgressBar != null) neonProgressBar.setProgress(p);
                            }
                        }));

                runOnUiThread(() -> {
                    if (isFinishing()) return;

                    isProcessing = false;
                    if (loaderContainer != null) loaderContainer.setVisibility(View.GONE);

                    if (result4K != null) {
                        finalEditedResult = result4K;
                        setResult(RESULT_OK, new Intent().putExtra("IS_EDITED", true));
                        finish();
                    } else {
                        Toast.makeText(this, getString(R.string.error_failed_process_4k), Toast.LENGTH_SHORT).show();
                           }
                });
            } catch (Exception | OutOfMemoryError e) {
                runOnUiThread(() -> {
                    isProcessing = false;
                    if (loaderContainer != null) loaderContainer.setVisibility(View.GONE);
                    Toast.makeText(this, getString(R.string.error_oom_processing), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    private void setupToggleLogic() {
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnViewOriginal) {
                    currentViewMode = 0;
                    imgPreview.setImageBitmap(originalBitmap);
                    imgPreview.setColorFilter(null);
                    seekbarContainer.setVisibility(View.INVISIBLE);
                } else {
                    currentViewMode = 1;
                    imgPreview.setImageBitmap(processedBitmap);
                    seekbarContainer.setVisibility(View.VISIBLE);
                    applyLivePreview(false);
                }
            }
        });
    }

    private void loadOriginal() {
        backgroundExecutor.execute(() -> {
            try {
                if (mediaUri == null) return;

                final Bitmap loaded = HDConverter.loadPreviewImage(this, mediaUri);
                runOnUiThread(() -> {
                    if (!isFinishing() && loaded != null) {
                        originalBitmap = loaded;
                        processedBitmap = loaded;
                        imgPreview.setImageBitmap(originalBitmap);
                        applyLivePreview(false);
                    } else if (loaded == null) {
                        Toast.makeText(this, getString(R.string.error_failed_load_image), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (OutOfMemoryError e) {
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.error_image_too_large), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void bindSeekBar(int layoutId, SeekBarBinder binder) {
        View layout = findViewById(layoutId);
        if (layout != null) {
            SeekBar s = layout.findViewById(R.id.seekbar);
            if (s != null) binder.onBind(s);
        }
    }

    private void resetSettings() {

        if(seekSmooth != null) seekSmooth.setProgress(0);
        if(seekExposure != null) seekExposure.setProgress(50);
        if(seekContrast != null) seekContrast.setProgress(50);
        if(seekBright != null) seekBright.setProgress(50);
        if(seekSaturation != null) seekSaturation.setProgress(50);
        if(seekWarmth != null) seekWarmth.setProgress(50);
        if(seekTint != null) seekTint.setProgress(50);
        if(seekSharp != null) seekSharp.setProgress(50);
        if(seekClarity != null) seekClarity.setProgress(50);
        if(seekVibrance != null) seekVibrance.setProgress(50);
        if(seekHighlights != null) seekHighlights.setProgress(50);
        if(seekShadows != null) seekShadows.setProgress(50);
        if(seekVignette != null) seekVignette.setProgress(0);
        cbDenoise.setChecked(false); cbHDR.setChecked(false);
        mainSeekBar.setProgress(50);
        applyLivePreview(true);
    }

    private void showScanAnimation() {
        scanLine.setVisibility(View.VISIBLE);
        scanLine.setTranslationY(0);
        scanLine.animate().translationY(mainCard.getHeight()).setDuration(600)
                .withEndAction(() -> scanLine.setVisibility(View.GONE)).start();
    }

    interface SeekBarBinder { void onBind(SeekBar s); }

    @Override protected void onDestroy() {
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdownNow();
        }
        if (originalBitmap != null && !originalBitmap.isRecycled()) {
            originalBitmap.recycle();
            originalBitmap = null;
        }
        super.onDestroy();
    }
}