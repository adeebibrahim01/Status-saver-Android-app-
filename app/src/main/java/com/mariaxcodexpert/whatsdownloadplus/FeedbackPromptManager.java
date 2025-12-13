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

public class FeedbackPromptManager {

    private static final String PREFS_NAME = "feedback_prefs";
    private static final String KEY_FEEDBACK_GIVEN = "feedback_given";
    private static final String KEY_LATER_TIMESTAMP = "later_timestamp";
    private static final long INITIAL_DELAY_MS = 1 * 60 * 1000; // 1 minute

    private final Activity activity;
    private final SharedPreferences prefs;
    private final Handler mainHandler;

    public FeedbackPromptManager(Activity activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void start() {
        boolean alreadyGiven = prefs.getBoolean(KEY_FEEDBACK_GIVEN, false);
        if (alreadyGiven) return; // user already gave feedback

        long lastLater = prefs.getLong(KEY_LATER_TIMESTAMP, 0);
        long delay = (lastLater == 0) ? INITIAL_DELAY_MS : 0; // 1 min only first time, else show immediately

        new Thread(() -> {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mainHandler.post(this::showFeedbackPrompt);
        }).start();
    }

    private void showFeedbackPrompt() {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        boolean alreadyGiven = prefs.getBoolean(KEY_FEEDBACK_GIVEN, false);
        if (alreadyGiven) return;

        // Inflate custom layout
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.layout_feedback_prompt, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvFeedbackTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvFeedbackMessage);
        Button btnGiveFeedback = dialogView.findViewById(R.id.btnGiveFeedback);
        ImageView ivClose = dialogView.findViewById(R.id.ivClose);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .setCancelable(false)
                .create();

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
        String url = "https://play.google.com/store/apps/details?id=" + activity.getPackageName();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        activity.startActivity(intent);
    }

    private void saveFeedbackGiven() {
        prefs.edit().putBoolean(KEY_FEEDBACK_GIVEN, true).apply();
    }

    private void saveLaterTimestamp() {
        prefs.edit().putLong(KEY_LATER_TIMESTAMP, System.currentTimeMillis()).apply();
    }
}
