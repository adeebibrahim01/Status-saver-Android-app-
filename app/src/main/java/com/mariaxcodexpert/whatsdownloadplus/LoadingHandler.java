package com.mariaxcodexpert.whatsdownloadplus;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class LoadingHandler {

    public interface LoadingCallback {
        void onComplete();
    }

    /**
     * Professional Glass Loading Handler
     * @param rootLayout Fragment ya Activity ka main layout jahan overlay mojood hai
     * @param message Display message (e.g., "Processing...", "Opening Gallery...")
     * @param duration Kitni der tak loading dikhani hai (milliseconds)
     * @param callback Kaam khatam hone par kya karna hai
     */
    public static void showLoading(View rootLayout, String message, int duration, LoadingCallback callback) {
        if (rootLayout == null) return;

        // Overlay Views find karein
        View overlay = rootLayout.findViewById(R.id.refreshOverlay);
        ProgressBar progress = rootLayout.findViewById(R.id.statusProgress);
        ImageView resultIcon = rootLayout.findViewById(R.id.ivStatusResultIcon);
        TextView infoText = rootLayout.findViewById(R.id.tvStatusInfo);

        if (overlay == null) return;

        // 1. Initial State Setup
        if (infoText != null) infoText.setText(message);
        if (progress != null) progress.setVisibility(View.VISIBLE);
        if (resultIcon != null) resultIcon.setVisibility(View.GONE);

        // 2. Show Overlay with Smooth Animation
        overlay.setVisibility(View.VISIBLE);
        overlay.setAlpha(0f);
        overlay.setScaleX(0.8f);
        overlay.setScaleY(0.8f);

        overlay.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .start();

        // 3. Simulated Processing Task
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // Success State (Show Checkmark)
            if (progress != null) progress.setVisibility(View.GONE);
            if (resultIcon != null) {
                resultIcon.setVisibility(View.VISIBLE);
                resultIcon.setScaleX(0.5f);
                resultIcon.setScaleY(0.5f);
                resultIcon.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
            }
            if (infoText != null) infoText.setText("Done!");

            // 4. Hide Overlay and Trigger Navigation/Action
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                overlay.animate()
                        .alpha(0f)
                        .scaleX(0.8f)
                        .scaleY(0.8f)
                        .setDuration(300)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                overlay.setVisibility(View.GONE);
                                if (callback != null) callback.onComplete();
                            }
                        }).start();
            }, 600); // Success icon dikhane ka time

        }, duration);
    }
}