package com.mariaxcodexpert.whatsdownloadplus.Helper;


import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import androidx.appcompat.widget.AppCompatButton;

import com.mariaxcodexpert.whatsdownloadplus.R;

public class WhatsNewDialog {

    private static final String PREFS_NAME = "AppUpdatesConfig";
    private static final String KEY_PREFIX = "hide_update_v_";

    /**
     * @param context Context of the activity
     * @param versionCode Your current app version code from build.gradle
     */
    public static void display(Context context, int versionCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (prefs.getBoolean(KEY_PREFIX + versionCode, false)) {
            return;
        }

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.whatsnew);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.setCancelable(false);

        AppCompatButton btnExplore = dialog.findViewById(R.id.btn_explore);
        CheckBox cbDontShow = dialog.findViewById(R.id.cb_dont_show);

        btnExplore.setOnClickListener(v -> {
            if (cbDontShow.isChecked()) {
                prefs.edit().putBoolean(KEY_PREFIX + versionCode, true).apply();
            }
            dialog.dismiss();
        });

        dialog.show();
    }
}