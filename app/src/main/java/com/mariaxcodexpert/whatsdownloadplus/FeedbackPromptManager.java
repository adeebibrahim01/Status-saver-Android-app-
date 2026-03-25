package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.concurrent.TimeUnit;

public class FeedbackPromptManager {

    private static final String PREFS_NAME = "feedback_prefs";
    private static final String KEY_FEEDBACK_GIVEN = "feedback_given";
    private static final String KEY_LATER_TIMESTAMP = "later_timestamp";

    private static final long INITIAL_DELAY_MS = 60 * 1000; // 1 Minute (Pehli baar ke liye)
    private static final long LATER_GAP_MS = TimeUnit.DAYS.toMillis(1); // 1 Day gap after "Later"

    private final Activity activity;
    private final SharedPreferences prefs;
    private final Handler mainHandler;

    public FeedbackPromptManager(Activity activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void start() {
        if (prefs.getBoolean(KEY_FEEDBACK_GIVEN, false)) return;

        long lastLater = prefs.getLong(KEY_LATER_TIMESTAMP, 0);
        long now = System.currentTimeMillis();

        // 1. Agar user ne pehle kabhi "Later" nahi kiya, to 1 min baad dikhao
        if (lastLater == 0) {
            mainHandler.postDelayed(this::showFeedbackPrompt, INITIAL_DELAY_MS);
        }
        // 2. Agar "Later" kiya tha, to check karein ke kya 24 ghante guzar chuke hain?
        else if (now - lastLater > LATER_GAP_MS) {
            // App khulne ke 30 second baad dikhao (bilkul foran nahi taake user disturb na ho)
            mainHandler.postDelayed(this::showFeedbackPrompt, 30000);
        }
    }

    private void showFeedbackPrompt() {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        // Re-check in case state changed
        if (prefs.getBoolean(KEY_FEEDBACK_GIVEN, false)) return;

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.layout_feedback_prompt, null);

        Button btnGiveFeedback = dialogView.findViewById(R.id.btnGiveFeedback);
        ImageView ivClose = dialogView.findViewById(R.id.ivClose);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Dialog background ko transparent karne ke liye (agar custom rounded corners hain)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnGiveFeedback.setOnClickListener(v -> {
            openPlayStore();
            saveFeedbackGiven();
            dialog.dismiss();
        });

        ivClose.setOnClickListener(v -> {
            saveLaterTimestamp();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void openPlayStore() {
        try {
            String packageName = activity.getPackageName();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (Exception e) {
            String url = "https://play.google.com/store/apps/details?id=" + activity.getPackageName();
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }

    private void saveFeedbackGiven() {
        prefs.edit().putBoolean(KEY_FEEDBACK_GIVEN, true).apply();
    }

    private void saveLaterTimestamp() {
        prefs.edit().putLong(KEY_LATER_TIMESTAMP, System.currentTimeMillis()).apply();
    }
}