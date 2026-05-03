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
import com.mariaxcodexpert.whatsdownloadplus.AdManager;

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

    // Existing SeekBars
    private SeekBar seekSmooth, seekSharp, seekBright, seekContrast, seekSaturation, seekExposure, seekWarmth, seekTint;

    // New SeekBars (Missing ones fixed)
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

        // Binding All SeekBars (Hidden Layouts)
        bindSeekBar(R.id.toolSmooth_hidden, s -> { seekSmooth = s; s.setProgress(0); });
        bindSeekBar(R.id.toolExposure_hidden, s -> { seekExposure = s; s.setProgress(50); });
        bindSeekBar(R.id.toolContrast_hidden, s -> { seekContrast = s; s.setProgress(50); });
        bindSeekBar(R.id.toolBrightness_hidden, s -> { seekBright = s; s.setProgress(50); });
        bindSeekBar(R.id.toolSaturation_hidden, s -> { seekSaturation = s; s.setProgress(50); });
        bindSeekBar(R.id.toolWarmth_hidden, s -> { seekWarmth = s; s.setProgress(50); });
        bindSeekBar(R.id.toolTint_hidden, s -> { seekTint = s; s.setProgress(50); });
        bindSeekBar(R.id.toolDetail_hidden, s -> { seekSharp = s; s.setProgress(50); });

        // New Tools Binding
        bindSeekBar(R.id.toolClarity_hidden, s -> { seekClarity = s; s.setProgress(50); });
        bindSeekBar(R.id.toolVibrance_hidden, s -> { seekVibrance = s; s.setProgress(50); });
        bindSeekBar(R.id.toolHighlights_hidden, s -> { seekHighlights = s; s.setProgress(50); });
        bindSeekBar(R.id.toolShadows_hidden, s -> { seekShadows = s; s.setProgress(50); });
        bindSeekBar(R.id.toolVignette_hidden, s -> { seekVignette = s; s.setProgress(0); });

        setupClickListeners();
        setupToggleLogic();
        switchTool("EXPOSURE AI", seekExposure);
    }

    private void setupClickListeners() {
        findViewById(R.id.btnCloseDialog).setOnClickListener(v -> finish());
        findViewById(R.id.btnResetManual).setOnClickListener(v -> resetSettings());

        btnSave.setOnClickListener(v -> AdManager.showInterstitial(this, new AdManager.AdCallback() {
            @Override public void onAdClosed() { saveImage4K(); }
            @Override public void onAdFailed() { saveImage4K(); }
        }));

        // Horizontal Icons Click Listeners
        setToolListener(R.id.toolSmooth, "TEXTURE SMOOTH", seekSmooth);
        setToolListener(R.id.toolExposure, "EXPOSURE AI", seekExposure);
        setToolListener(R.id.toolContrast, "DYNAMIC CONTRAST", seekContrast);
        setToolListener(R.id.toolBrightness, "LUMINANCE", seekBright);
        setToolListener(R.id.toolSaturation, "SATURATION", seekSaturation);
        setToolListener(R.id.toolWarmth, "COLOR TEMP", seekWarmth);
        setToolListener(R.id.toolTint, "TINT", seekTint);
        setToolListener(R.id.toolDetail, "4K DETAIL", seekSharp);

        // New Tools Listeners
        setToolListener(R.id.toolClarity, "CLARITY AI", seekClarity);
        setToolListener(R.id.toolVibrance, "VIBRANCE", seekVibrance);
        setToolListener(R.id.toolHighlights, "HIGHLIGHTS", seekHighlights);
        setToolListener(R.id.toolShadows, "SHADOWS", seekShadows);
        setToolListener(R.id.toolVignette, "VIGNETTE", seekVignette);

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

        float b = (seekBright.getProgress() - 50) * 1.5f;
        float c = seekContrast.getProgress() / 50f;
        float s = (seekSaturation.getProgress() / 50f);

        // Simple Vibrance implementation for preview
        if (seekVibrance != null) s *= (1.0f + (seekVibrance.getProgress() - 50) / 100f);

        float e = seekExposure.getProgress() / 50f;

        if (cbHDR.isChecked()) { s *= 1.3f; b += 5f; }

        baseMatrix.setSaturation(s);
        float scale = c * e;
        float translate = b + (127.5f * (1.0f - scale));

        float[] mat = { scale, 0, 0, 0, translate, 0, scale, 0, 0, translate, 0, 0, scale, 0, translate, 0, 0, 0, 1, 0 };
        tempMatrix.set(mat);
        baseMatrix.postConcat(tempMatrix);

        imgPreview.setColorFilter(new ColorMatrixColorFilter(baseMatrix));

        // --- SMOOTHING FIX ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // 100/150 = 0.66 radius.
            // Professional apps mein smoothing radius 1.0 se kam rakha jata h
            // taake details kharab na hon.
            float smoothRadius = seekSmooth.getProgress() / 150.0f;

            if (smoothRadius > 0.01f) {
                // Sirf itna blur jo noise ko khatam kray, puri image ko nahi
                imgPreview.setRenderEffect(RenderEffect.createBlurEffect(
                        smoothRadius,
                        smoothRadius,
                        Shader.TileMode.CLAMP
                ));
            } else {
                imgPreview.setRenderEffect(null);
            }
        }

        if (withAnimation) showScanAnimation();
    }

    private void saveImage4K() {
        if (isProcessing || mediaUri == null) return;

        isProcessing = true;
        loaderContainer.setVisibility(View.VISIBLE);
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
                            tvPercent.setText(p + "%");
                            if (tvStatusLabel != null) tvStatusLabel.setText(status);
                            neonProgressBar.setProgress(p);
                        }));

                runOnUiThread(() -> {
                    loaderContainer.setVisibility(View.GONE);
                    isProcessing = false;
                    if (result4K != null) {
                        finalEditedResult = result4K;
                        setResult(RESULT_OK, new Intent().putExtra("IS_EDITED", true));
                        finish();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> { loaderContainer.setVisibility(View.GONE); isProcessing = false; });
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
            originalBitmap = HDConverter.loadPreviewImage(this, mediaUri);
            runOnUiThread(() -> {
                if (originalBitmap != null) {
                    imgPreview.setImageBitmap(originalBitmap);
                    processedBitmap = originalBitmap;
                    applyLivePreview(false);
                }
            });
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
        // Resetting all 13 SeekBars
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
        backgroundExecutor.shutdownNow();
        super.onDestroy();
    }
}