package com.mariaxcodexpert.whatsdownloadplus.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

public class UiUtils {

    /**
     * 🔥 Snappy Click Animation
     * User jab button dabaye ga toh halka sa bounce effect miley ga (Premium feel)
     */
    public static void animateClick(View view, Runnable endAction) {
        view.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(80)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(80)
                            .withEndAction(endAction)
                            .start();
                }).start();
    }

    /**
     * 🔥 Pulse Animation for FAB
     * Download button halka halka "dharak" (pulse) raha hoga taake user ka dhyan jaye
     */
    public static void startPulseAnimation(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 1.1f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 1.1f, 1.0f);

        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);

        AnimatorSet pulseAnimation = new AnimatorSet();
        pulseAnimation.playTogether(scaleX, scaleY);
        pulseAnimation.setDuration(1200);
        pulseAnimation.start();
    }

    /**
     * 🔥 Open WhatsApp Helper
     * Safe way to launch WhatsApp with a guide message
     */
    public static void openWhatsApp(Context context) {
        Toast.makeText(context, "Please view the statuses in WhatsApp first!", Toast.LENGTH_LONG).show();

        // 1 second delay taake user message parh le
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.whatsapp");
                if (intent != null) {
                    context.startActivity(intent);
                } else {
                    // Agar WhatsApp install nahi hai
                    Toast.makeText(context, "WhatsApp not found!", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                // 🔥 Standard Android Logging: Tag, Message, aur Exception
                android.util.Log.e("SyncManager", "Error during background sync: " + e.getMessage(), e);
            }
        }, 1000);
    }
}