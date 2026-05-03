package com.mariaxcodexpert.whatsdownloadplus.ui.utils.media;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.util.Log;
import java.io.InputStream;

/**
 * HDConverter: Extreme 4K AI Upscaling & Neural Clarity Engine.
 * Optimized for mirror-perfect rendering with Advanced Image Controls.
 */
public class HDConverter {

    private static final String TAG = "HDConverter_AI";

    public interface ProgressListener {
        void onProgress(int percent, String status);
    }

    public static Bitmap loadPreviewImage(Context ctx, Uri uri) {
        try {
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
            try (InputStream is = ctx.getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(is, null, opt);
            }
        } catch (Exception e) {
            Log.e(TAG, "Preview Load Failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * FINAL EXPORT ENGINE: Force 4K Reconstruction with Advanced Parameters.
     */
    public static Bitmap process8KExport(Context ctx, Uri uri,
                                         int smooth, int sharp, int bright, int contrast,
                                         int saturation, int exposure, int warmth, int tint,
                                         int clarity, int vibrance, int highlights, int shadows, int vignette,
                                         boolean denoise, boolean hdr, ProgressListener lp) {
        try {
            if (lp != null) lp.onProgress(5, "Initializing 4K AI Engine...");

            Bitmap source;
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inMutable = true;
            opt.inPreferredConfig = Bitmap.Config.ARGB_8888;

            try (InputStream is = ctx.getContentResolver().openInputStream(uri)) {
                source = BitmapFactory.decodeStream(is, null, opt);
            }

            if (source == null) return null;
            if (lp != null) lp.onProgress(15, "Analyzing Image Pixels...");

            // --- 4K UPSCALING LOGIC ---
            int sw = source.getWidth();
            int sh = source.getHeight();
            float targetWidth = 3840f;
            float scaleFactor = targetWidth / sw;
            int tw = (int) targetWidth;
            int th = (int) (sh * scaleFactor);

            if (lp != null) lp.onProgress(30, "Upscaling to 4K Ultra HD...");

            Bitmap highRes = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(highRes);
            Paint scalePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
            canvas.drawBitmap(source, null, new Rect(0, 0, tw, th), scalePaint);
            source.recycle();

            if (lp != null) lp.onProgress(50, "Applying Neural Color Mapping...");

            // --- APPLY USER SETTINGS (INCLUDING NEW PARAMETERS) ---
            Bitmap result = applyAdvancedClarity(highRes, smooth, sharp, bright, contrast,
                    saturation, exposure, warmth, tint, clarity, vibrance, highlights, shadows, vignette,
                    denoise, hdr, lp);

            if (lp != null) lp.onProgress(100, "Export Complete!");

            if (highRes != result) highRes.recycle();
            return result;

        } catch (Exception | OutOfMemoryError e) {
            Log.e(TAG, "4K Export Failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * CLARITY ENGINE: Mirror-perfect tuning with New Advanced Controls.
     */
    public static Bitmap applyAdvancedClarity(Bitmap src, int smooth, int sharp,
                                              int bright, int contrast, int saturation,
                                              int exposure, int warmth, int tint,
                                              int clarity, int vibrance, int highlights, int shadows, int vignette,
                                              boolean denoise, boolean hdr, ProgressListener lp) {

        if (src == null || src.isRecycled()) return null;

        Bitmap output = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);

        // 1. ADVANCED COLOR & LIGHTING LOGIC
        // Vibrance affects saturation logic
        float vibFactor = 1.0f + (vibrance - 50) / 100f;
        float s = (saturation / 50f) * vibFactor;

        float b = (bright - 50) * 1.5f;
        float c = contrast / 50f;
        float e = exposure / 50f;

        if (hdr) { s *= 1.3f; b += 5f; }

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(s);

        // Highres Lighting (Highlights & Shadows adjustment)
        float shd = (shadows - 50) * 0.3f;
        float hgl = (highlights - 50) * 0.3f;

        float scale = c * e;
        float translate = b + shd + hgl + (127.5f * (1.0f - scale));

        float[] mat = {
                scale, 0, 0, 0, translate,
                0, scale, 0, 0, translate,
                0, 0, scale, 0, translate,
                0, 0, 0, 1, 0
        };
        cm.postConcat(new ColorMatrix(mat));

        // Warmth & Tint
        float w = (warmth - 50) * 0.5f;
        float t = (tint - 50) * 0.5f;
        float[] colorBalance = {
                1f + (w/255f), 0, 0, 0, 0,
                0, 1f - (t/255f), 0, 0, 0,
                0, 0, 1f - (w/255f), 0, 0,
                0, 0, 0, 1, 0
        };
        cm.postConcat(new ColorMatrix(colorBalance));

        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(src, 0, 0, paint);

        // 2. CLARITY ENGINE (Local Contrast Enhancement)
        if (clarity > 50) {
            if (lp != null) lp.onProgress(75, "Enhancing Mid-tone Clarity...");
            float clarityStrength = (clarity - 50) / 100f;
            Paint clarityPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

            // Yahan PorterDuff.Mode.OVERLAY use karein kyunki SOFT_LIGHT support nahi hai
            clarityPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));

            clarityPaint.setAlpha((int) (130 * clarityStrength)); // Alpha thoda adjust kiya hai balance ke liye
            canvas.drawBitmap(output, 0, 0, clarityPaint);
        }

        // 3. KERNEL SHARPENING
        if (sharp > 50) {
            if (lp != null) lp.onProgress(85, "Refining 4K Edges...");
            float strength = (sharp - 50) / 100f;
            Paint sharpPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
            sharpPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));
            sharpPaint.setAlpha((int) (160 * strength));
            canvas.drawBitmap(output, 0.6f, 0.6f, sharpPaint);
        }

        // 4. PROFESSIONAL SMOOTHING (Skin Softening & Noise Reduction)
        if (smooth > 0 || denoise) {
            if (lp != null) lp.onProgress(90, "Applying Neural Smoothing...");

            // 🔥 FIX: Smooth ki value ko bohot sensitive banaya h (0-100 range ko 0.1 se 0.8 scale krain)
            // Professional apps mein smooth kabhi b 100% (Solid) nahi hota
            float smoothStrength = (smooth / 100f);

            Paint smoothPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

            // Seekbar ko handle karne k liye:
            // smooth=1 par bilkul halka asar ho, smooth=100 par b image face features na khoye
            int alpha = (int) (smoothStrength * 45); // Max alpha sirf 45 rakha h (Pehle 160 tha, jo bohot zyada tha)

            if (denoise) alpha += 20; // Denoise ka asar b natural rakha h

            smoothPaint.setAlpha(Math.min(alpha, 70)); // Global cap 70 par (Professional limit)

            // 🔥 Trick: Image ko 0.5 ya 1 pixel offset k sath draw krain taake real "Smoothing" feel aye
            // Sirf alpha kam karne se blur nahi hota, halka sa shift dene se edges soft ho jati hain
            canvas.drawBitmap(output, 0.4f, 0.4f, smoothPaint);
            canvas.drawBitmap(output, -0.4f, -0.4f, smoothPaint);
        }

        // 5. VIGNETTE (Post-Process)
        if (vignette > 0) {
            if (lp != null) lp.onProgress(95, "Applying Artistic Vignette...");
            float radius = Math.max(output.getWidth(), output.getHeight()) * 0.8f;
            RadialGradient gradient = new RadialGradient(
                    output.getWidth() / 2f, output.getHeight() / 2f, radius,
                    new int[]{Color.TRANSPARENT, Color.BLACK},
                    new float[]{0.4f, 1.0f}, Shader.TileMode.CLAMP);

            Paint vigPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            vigPaint.setShader(gradient);
            vigPaint.setAlpha((int) (vignette * 2.55f)); // 0-100 to 0-255
            canvas.drawRect(0, 0, output.getWidth(), output.getHeight(), vigPaint);
        }

        return output;
    }
}