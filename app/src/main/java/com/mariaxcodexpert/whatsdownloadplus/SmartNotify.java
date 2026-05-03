package com.mariaxcodexpert.whatsdownloadplus;

import android.graphics.Color;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.snackbar.Snackbar;

/**
 * 🔥 Advance SmartNotify Engine (Fixed Library Group Access Error)
 * Features: Haptic Feedback, Modern Layout, & Floating Margins.
 */
public class SmartNotify {

    // Material Design 3 Colors
    private static final String COLOR_SUCCESS = "#2E7D32"; // Deep Green
    private static final String COLOR_ERROR   = "#D32F2F"; // Material Red
    private static final String COLOR_INFO    = "#1976D2"; // Bright Blue
    private static final String COLOR_WARNING = "#FFA000"; // Amber

    public static void success(View view, String message) {
        show(view, message, COLOR_SUCCESS, android.R.drawable.ic_menu_save);
    }

    public static void error(View view, String message) {
        show(view, message, COLOR_ERROR, android.R.drawable.ic_dialog_alert);
    }

    public static void info(View view, String message) {
        show(view, message, COLOR_INFO, android.R.drawable.ic_dialog_info);
    }

    public static void warning(View view, String message) {
        show(view, message, COLOR_WARNING, android.R.drawable.ic_dialog_info);
    }

    private static void show(View view, String message, String colorCode, int iconRes) {
        if (view == null) return;

        try {
            // 1. Create Snackbar
            Snackbar snackbar = Snackbar.make(view, "", Snackbar.LENGTH_SHORT);

            // 2. 🔥 FIX: Get the generic View instead of SnackbarLayout
            View snackbarBaseView = snackbar.getView();
            snackbarBaseView.setBackgroundColor(Color.TRANSPARENT);

            // 3. Custom Layout Inflation
            LayoutInflater inflater = LayoutInflater.from(view.getContext());
            View customView = inflater.inflate(R.layout.layout_custom_snackbar, null);

            // 4. UI References
            CardView card = customView.findViewById(R.id.parentCard);
            TextView textView = customView.findViewById(R.id.snackText);
            ImageView imageView = customView.findViewById(R.id.snackIcon);

            // 5. Data Binding
            textView.setText(message);
            try {
                imageView.setImageResource(iconRes);
            } catch (Exception e) {
                imageView.setImageResource(android.R.drawable.ic_dialog_info);
            }
            card.setCardBackgroundColor(Color.parseColor(colorCode));

            // 6. 🔥 ADVANCE: Haptic Feedback (Vibration for Premium Feel)
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

            // 7. 🔥 FIX: Safe View Injection
            if (snackbarBaseView instanceof ViewGroup snackbarLayout) {
                snackbarLayout.setPadding(0, 0, 0, 0); // Remove default padding

                // Margin Adjustment (Floating Look)
                ViewGroup.LayoutParams layoutParams = snackbarLayout.getLayoutParams();
                if (layoutParams instanceof FrameLayout.LayoutParams params) {
                    params.setMargins(40, 0, 40, 120); // Side and Bottom margins
                    snackbarLayout.setLayoutParams(params);
                } else if (layoutParams instanceof CoordinatorLayout.LayoutParams params) {
                    params.setMargins(40, 0, 40, 120);
                    snackbarLayout.setLayoutParams(params);
                }

                snackbarLayout.removeAllViews(); // Purane views saaf karein
                snackbarLayout.addView(customView, 0);
            }

            // 8. 🔥 ADVANCE: Fade-In Animation
            customView.setAlpha(0f);
            customView.animate().alpha(1f).setDuration(250).start();

            snackbar.show();

        } catch (Exception e) {
            Log.e("SmartNotify", "Error showing snackbar: " + e.getMessage());
        }
    }
}