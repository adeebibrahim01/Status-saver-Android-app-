package com.mariaxcodexpert.whatsdownloadplus.ui.Home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;

public class CardLiquidAnimator {

    public static void animate(
            @NonNull MaterialCardView card,
            @Nullable TextView text,
            @ColorInt int startColor,
            @ColorInt int endColor,
            long durationMillis,
            float pulseScale
    ) {
        // 1. Check if card is laid out (width check fix)
        if (card.getWidth() == 0) {
            card.post(() -> animate(card, text, startColor, endColor, durationMillis, pulseScale));
            return;
        }

        // 2. Prepare GradientDrawable
        GradientDrawable liquidFill = new GradientDrawable();
        liquidFill.setShape(GradientDrawable.RECTANGLE); // Card ke liye Rectangle behtar hai OVAL se
        liquidFill.setCornerRadius(card.getRadius());    // Card ke corners se match karein
        liquidFill.setGradientType(GradientDrawable.RADIAL_GRADIENT);

        // Start transparent colors
        int transparentStart = Color.argb(0, Color.red(startColor), Color.green(startColor), Color.blue(startColor));
        int transparentEnd = Color.argb(0, Color.red(endColor), Color.green(endColor), Color.blue(endColor));
        liquidFill.setColors(new int[]{transparentStart, transparentEnd});
        liquidFill.setGradientRadius(0f);

        card.setForeground(liquidFill);

        float maxRadius = (float) Math.hypot(card.getWidth(), card.getHeight());

        // 3. Main Fill Animator
        ValueAnimator fillAnimator = ValueAnimator.ofFloat(0f, 1f);
        fillAnimator.setDuration(durationMillis);
        fillAnimator.setInterpolator(new DecelerateInterpolator());

        fillAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            float radius = fraction * maxRadius;
            int alpha = (int) (fraction * 180); // 180 alpha subtle (pyara) lagta hai

            int sColor = Color.argb(alpha, Color.red(startColor), Color.green(startColor), Color.blue(startColor));
            int eColor = Color.argb(alpha, Color.red(endColor), Color.green(endColor), Color.blue(endColor));

            liquidFill.setGradientRadius(radius);
            liquidFill.setColors(new int[]{sColor, eColor});

            // Card Pulse Effect
            float scale = 1f + (pulseScale * fraction);
            card.setScaleX(scale);
            card.setScaleY(scale);
        });

        // 4. Liquid Flow Animator (Infinite)
        ValueAnimator flowAnimator = ValueAnimator.ofFloat(0f, 1f);
        flowAnimator.setDuration(2000);
        flowAnimator.setRepeatCount(ValueAnimator.INFINITE);
        flowAnimator.setRepeatMode(ValueAnimator.REVERSE);
        flowAnimator.setInterpolator(new LinearInterpolator());
        flowAnimator.addUpdateListener(anim -> {
            float p = (float) anim.getAnimatedValue();
            liquidFill.setGradientCenter(p, 1f - p); // Diagonal flow
        });

        // 5. Cleanup and Reset
        fillAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Smooth Fade Out
                card.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(300).start();

                ValueAnimator fadeOut = ValueAnimator.ofFloat(180f, 0f);
                fadeOut.setDuration(500);
                fadeOut.addUpdateListener(a -> {
                    int alpha = (int) (float) a.getAnimatedValue();
                    int s = Color.argb(alpha, Color.red(startColor), Color.green(startColor), Color.blue(startColor));
                    int e = Color.argb(alpha, Color.red(endColor), Color.green(endColor), Color.blue(endColor));
                    liquidFill.setColors(new int[]{s, e});
                });

                fadeOut.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        flowAnimator.cancel();
                        card.setForeground(null);
                    }
                });
                fadeOut.start();
            }
        });

        // Start both
        fillAnimator.start();
        flowAnimator.start();
    }
}