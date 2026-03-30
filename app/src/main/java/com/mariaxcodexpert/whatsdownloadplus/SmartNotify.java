package com.mariaxcodexpert.whatsdownloadplus;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import com.google.android.material.snackbar.Snackbar;

public class SmartNotify {

    public static void success(View view, String message) {
        showCustomSnack(view, message, "#2E7D32", android.R.drawable.ic_menu_save);
    }

    public static void error(View view, String message) {
        showCustomSnack(view, message, "#C62828", android.R.drawable.ic_dialog_alert);
    }

    public static void info(View view, String message) {
        showCustomSnack(view, message, "#1A1C1E", android.R.drawable.ic_dialog_info);
    }

    // 🔥 Ye method add karein, error khatam ho jayega
    public static void warning(View view, String message) {
        // Orange/Amber color code for warning visibility
        showCustomSnack(view, message, "#FF8F00", android.R.drawable.ic_dialog_alert);
    }

    private static void showCustomSnack(View view, String message, String colorCode, int iconRes) {
        try {
            Snackbar snackbar = Snackbar.make(view, "", Snackbar.LENGTH_SHORT);
            View snackbarView = snackbar.getView();
            snackbarView.setBackgroundColor(Color.TRANSPARENT);

            ViewGroup snackbarLayout = (ViewGroup) snackbarView;
            snackbarLayout.setPadding(0, 0, 0, 0);

            // layout_custom_snackbar inflate ho raha hai
            View customView = LayoutInflater.from(view.getContext()).inflate(R.layout.layout_custom_snackbar, null);

            CardView card = customView.findViewById(R.id.parentCard);
            TextView textView = customView.findViewById(R.id.snackText);
            ImageView imageView = customView.findViewById(R.id.snackIcon);

            textView.setText(message);
            imageView.setImageResource(iconRes);
            card.setCardBackgroundColor(Color.parseColor(colorCode));

            snackbarLayout.addView(customView, 0);
            snackbar.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}