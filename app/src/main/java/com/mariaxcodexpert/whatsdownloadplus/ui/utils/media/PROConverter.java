package com.mariaxcodexpert.whatsdownloadplus.ui.utils.media;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.InputStream;

public class PROConverter {
    private static final String TAG = "PROConverter_8K";
    private static final long MAX_PIXELS_FOR_30MB = 7500000;
    private static final int MAX_8K_DIMENSION = 8192;
    private static final Handler UI_HANDLER = new Handler(Looper.getMainLooper());

    public interface ProgressListener { void onProgress(int percent); }

    private static void updateUIProgress(ProgressListener lp, int percent) {
        if (lp != null) UI_HANDLER.post(() -> lp.onProgress(percent));
    }

    public static Bitmap processToIPhoneQuality(Context ctx, Uri uri, int style, ProgressListener lp) {
        Bitmap source = null;
        Bitmap scaled = null;
        Bitmap result = null;

        try {
            updateUIProgress(lp, 5);
            Log.d(TAG, "Optimized 8K Engine Started...");

            // --- 0. Decode Bounds ---
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inJustDecodeBounds = true;
            try (InputStream is = ctx.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(is, null, opt);
            }

            // Initial memory safety
            opt.inJustDecodeBounds = false;
            opt.inMutable = true;
            opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
            if (opt.outWidth > 4000 || opt.outHeight > 4000) opt.inSampleSize = 2;

            try (InputStream is = ctx.getContentResolver().openInputStream(uri)) {
                source = BitmapFactory.decodeStream(is, null, opt);
            }

            if (source == null) return null;
            updateUIProgress(lp, 20);

            // --- 1. Resolution Balancer ---
            int targetW = (int) (source.getWidth() * 4.0f);
            int targetH = (int) (source.getHeight() * 4.0f);

            if (targetW > MAX_8K_DIMENSION || targetH > MAX_8K_DIMENSION) {
                float r = Math.min((float) MAX_8K_DIMENSION / targetW, (float) MAX_8K_DIMENSION / targetH);
                targetW *= r; targetH *= r;
            }

            long totalPixels = (long) targetW * targetH;
            if (totalPixels > MAX_PIXELS_FOR_30MB) {
                float memoryScale = (float) Math.sqrt((double) MAX_PIXELS_FOR_30MB / totalPixels);
                targetW = (int) (targetW * memoryScale);
                targetH = (int) (targetH * memoryScale);
            }

            // --- 2. Scaling ---
            scaled = Bitmap.createScaledBitmap(source, targetW, targetH, true);
            if (source != scaled) source.recycle(); // Free source RAM
            updateUIProgress(lp, 45);

            // --- 3. Ultra-HD Tone Engine ---
            // Fix: Paint object local rakha taake thread-safety rahe
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);

            result = Bitmap.createBitmap(scaled.getWidth(), scaled.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);

            // iPhone-style color logic
            float sat = 1.35f, con = 1.15f, bri = 5.0f;
            if (style == 1) { sat = 1.15f; con = 1.10f; bri = 8.0f; }
            else if (style == 2) { sat = 1.60f; con = 1.30f; bri = -2.0f; }

            ColorMatrix cm = new ColorMatrix();
            cm.setSaturation(sat);
            float t = (-0.5f * con + 0.5f) * 255f + bri;
            float[] matrixArray = { con, 0, 0, 0, t, 0, con, 0, 0, t, 0, 0, con, 0, t, 0, 0, 0, 1, 0 };
            cm.postConcat(new ColorMatrix(matrixArray));
            paint.setColorFilter(new ColorMatrixColorFilter(cm));

            // Scaled ko Result par draw kiya with color filter
            canvas.drawBitmap(scaled, 0, 0, paint);
            updateUIProgress(lp, 75);

            // --- 4. Sharpness Pass (Anti-Pixelation) ---
            paint.setColorFilter(null);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));
            paint.setAlpha(130);

            // Scaled ko hi halka sa offset de kar draw kiya overlay mode mein
            canvas.drawBitmap(scaled, 0.5f, 0.5f, paint);

            // Clean up
            paint.setXfermode(null);
            if (scaled != result) scaled.recycle();

            updateUIProgress(lp, 100);
            return result;

        } catch (Throwable e) {
            Log.e(TAG, "Critical Engine Error: " + e.getMessage());
            if (source != null && !source.isRecycled()) source.recycle();
            if (scaled != null && !scaled.isRecycled()) scaled.recycle();
            updateUIProgress(lp, 0);
            return null;
        }
    }
}