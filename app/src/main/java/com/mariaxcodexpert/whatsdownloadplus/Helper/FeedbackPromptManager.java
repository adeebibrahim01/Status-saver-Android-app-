package com.mariaxcodexpert.whatsdownloadplus.Helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.button.MaterialButton;
import java.util.concurrent.TimeUnit;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.mariaxcodexpert.whatsdownloadplus.R;

import android.os.Bundle;

public class FeedbackPromptManager {

    private static final String PREFS_NAME = "feedback_prefs";
    private static final String KEY_FEEDBACK_GIVEN = "feedback_given";
    private static final String KEY_LATER_TIMESTAMP = "later_timestamp";
    private static final String KEY_SUCCESSFUL_SAVES = "success_saves_count";

    private static final int MIN_SAVES_REQUIRED = 3;
    private static final long LATER_GAP_MS = TimeUnit.DAYS.toMillis(2);

    private final Activity activity;
    private final SharedPreferences prefs;

    public FeedbackPromptManager(Activity activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void incrementSuccessAndCheck() {
        if (prefs.getBoolean(KEY_FEEDBACK_GIVEN, false)) return;

        int currentSaves = prefs.getInt(KEY_SUCCESSFUL_SAVES, 0) + 1;
        prefs.edit().putInt(KEY_SUCCESSFUL_SAVES, currentSaves).apply();

        if (currentSaves >= MIN_SAVES_REQUIRED) {
            checkAndShow();
        }
    }

    private void checkAndShow() {
        long lastLater = prefs.getLong(KEY_LATER_TIMESTAMP, 0);
        long now = System.currentTimeMillis();

        if (now - lastLater > LATER_GAP_MS) {
            showFeedbackPrompt();
        }
    }

    private void showFeedbackPrompt() {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.layout_feedback_prompt, null);

        MaterialButton btnGiveFeedback = dialogView.findViewById(R.id.btnGiveFeedback);
        ImageView ivClose = dialogView.findViewById(R.id.ivClose);
        LinearLayout llStars = dialogView.findViewById(R.id.llStars);
        TextView tvTitle = dialogView.findViewById(R.id.tvFeedbackTitle);

        tvTitle.setText(activity.getString(R.string.feedback_title_question));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnGiveFeedback.setOnClickListener(v -> {
            handleRatingAction(5, dialog);
        });

        setupStarListeners(dialogView, dialog);

        ivClose.setOnClickListener(v -> {
            prefs.edit().putLong(KEY_LATER_TIMESTAMP, System.currentTimeMillis()).apply();
            prefs.edit().putInt(KEY_SUCCESSFUL_SAVES, 0).apply();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setupStarListeners(View view, AlertDialog dialog) {
        int[] starIds = {R.id.star1, R.id.star2, R.id.star3, R.id.star4, R.id.star5};

        for (int i = 0; i < starIds.length; i++) {
            final int rating = i + 1;
            View star = view.findViewById(starIds[i]);
            if (star != null) {
                star.setOnClickListener(v -> handleRatingAction(rating, dialog));
            }
        }
    }

    private void handleRatingAction(int stars, AlertDialog dialog) {
        logRatingEvent(stars);

        if (stars >= 4) {
            openPlayStore();
            prefs.edit().putBoolean(KEY_FEEDBACK_GIVEN, true).apply();
        } else {
            Toast.makeText(activity, activity.getString(R.string.toast_feedback_thank_you), Toast.LENGTH_LONG).show();
            prefs.edit().putBoolean(KEY_FEEDBACK_GIVEN, true).apply();
        }
        dialog.dismiss();
    }

    private void logRatingEvent(int rating) {
        try {
            FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(activity);
            Bundle bundle = new Bundle();
            bundle.putInt("rating_value", rating);
            bundle.putString("rating_category", rating >= 4 ? "positive" : "negative");
            mFirebaseAnalytics.logEvent("user_app_rating", bundle);
        } catch (Exception e) {

        }
    }


    private void openPlayStore() {
        String packageName = activity.getPackageName();
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
        } catch (Exception e) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }
}