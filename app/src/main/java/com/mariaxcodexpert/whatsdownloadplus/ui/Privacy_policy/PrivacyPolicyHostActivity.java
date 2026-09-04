package com.mariaxcodexpert.whatsdownloadplus.ui.Privacy_policy;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.mariaxcodexpert.whatsdownloadplus.R;

public class PrivacyPolicyHostActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

         try {
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                    .setAppearanceLightStatusBars(true);
            getWindow().setStatusBarColor(Color.WHITE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_privacy_policy_host);

        if (savedInstanceState == null) {
            if (findViewById(R.id.fragment_container) != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new PrivacyPolicyFragment())
                        .commit();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}