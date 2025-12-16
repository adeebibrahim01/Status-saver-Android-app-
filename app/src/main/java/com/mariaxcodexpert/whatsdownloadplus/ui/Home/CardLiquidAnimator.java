package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CardLiquidAnimator {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Animate a MaterialCardView with liquid-like radial gradient fill.
     *
     * @param card           Target card
     * @param text           Optional TextView inside card (will remain unaffected)
     * @param startColor     Gradient start color
     * @param endColor       Gradient end color
     * @param durationMillis Duration of fill animation in milliseconds
     * @param pulseScale     Card pulse scale factor, e.g., 0.02f
     */
    public static void animate(
            @NonNull MaterialCardView card,
            @Nullable TextView text,
            @ColorInt int startColor,
            @ColorInt int endColor,
            long durationMillis,
            float pulseScale
    ) {
        executor.execute(() -> {

            // --- Prepare GradientDrawable ---
            GradientDrawable liquidFill = new GradientDrawable();
            liquidFill.setShape(GradientDrawable.OVAL);
            liquidFill.setGradientType(GradientDrawable.RADIAL_GRADIENT);

            // Start fully transparent
            int startAlpha = startColor & 0x00FFFFFF;
            int endAlpha = endColor & 0x00FFFFFF;
            liquidFill.setColors(new int[]{startAlpha, endAlpha});
            liquidFill.setGradientRadius(0f);

            mainHandler.post(() -> card.setForeground(liquidFill));

            int cardWidth = card.getWidth();

            // --- Fill Animator (smooth start & end) ---
            ValueAnimator fillAnimator = ValueAnimator.ofFloat(0f, 1f);
            fillAnimator.setDuration(durationMillis);
            fillAnimator.setInterpolator(new DecelerateInterpolator());
            fillAnimator.addUpdateListener(animation -> {
                float fraction = (float) animation.getAnimatedValue();

                float radius = fraction * (cardWidth / 2f);
                int alpha = (int) (fraction * 255);

                int startWithAlpha = (startColor & 0x00FFFFFF) | (alpha << 24);
                int endWithAlpha = (endColor & 0x00FFFFFF) | (alpha << 24);

                mainHandler.post(() -> {
                    liquidFill.setGradientRadius(radius);
                    liquidFill.setColors(new int[]{startWithAlpha, endWithAlpha});

                    // Pulse only on card
                    float scale = 1f + pulseScale * fraction;
                    card.setScaleX(scale);
                    card.setScaleY(scale);
                });
            });

            // --- Flow Animator (liquid effect) ---
            ValueAnimator flowAnimator = ValueAnimator.ofFloat(0f, 1f);
            flowAnimator.setDuration(1000); // slower flow for smooth effect
            flowAnimator.setRepeatCount(ValueAnimator.INFINITE);
            flowAnimator.setInterpolator(new LinearInterpolator());
            flowAnimator.addUpdateListener(animation -> {
                float progress = (float) animation.getAnimatedValue();
                mainHandler.post(() -> {
                    liquidFill.setGradientCenter(progress, progress);
                    card.invalidate();
                });
            });

            // --- Start Animations ---
            mainHandler.post(() -> {
                fillAnimator.start();
                flowAnimator.start();
            });

            // --- Smooth Reset ---
            fillAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    flowAnimator.cancel();

                    ValueAnimator fadeOut = ValueAnimator.ofFloat(1f, 0f);
                    fadeOut.setDuration(600);
                    fadeOut.addUpdateListener(anim -> {
                        float alpha = (float) anim.getAnimatedValue();
                        int startWithAlpha = (startColor & 0x00FFFFFF) | ((int) (alpha * 255) << 24);
                        int endWithAlpha = (endColor & 0x00FFFFFF) | ((int) (alpha * 255) << 24);
                        liquidFill.setColors(new int[]{startWithAlpha, endWithAlpha});
                        card.invalidate();
                    });
                    fadeOut.start();

                    fadeOut.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            card.setForeground(null);
                            card.setScaleX(1f);
                            card.setScaleY(1f);
                        }
                    });
                }
            });
        });
    }
}
