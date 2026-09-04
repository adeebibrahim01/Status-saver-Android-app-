package com.mariaxcodexpert.whatsdownloadplus.Splash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.mariaxcodexpert.whatsdownloadplus.Ads.AdManager;
import com.mariaxcodexpert.whatsdownloadplus.ui.Subscription.BillingManager;
import com.mariaxcodexpert.whatsdownloadplus.MainActivity;
import com.mariaxcodexpert.whatsdownloadplus.Permissions.PermissionsActivity;
import com.mariaxcodexpert.whatsdownloadplus.R;

public class Splash_screen extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_STATUS_FOLDER_URI = "statusFolderUri";
    private static final String KEY_FIRST_TIME = "isFirstTime";
    private static final String KEY_IS_PREMIUM = "isPremium";

    private enum AppState { LOADING, AD_VISIBLE, PROCEEDED }
    private AppState currentStatus = AppState.LOADING;
    private boolean isNavigating = false;

    private TextView tvAdStatus;
    private CountDownTimer adTimer;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private BillingManager billingManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        hideSystemUI();
        tvAdStatus = findViewById(R.id.tvAdStatus);

        syncLocalStatus();

        initBillingCheck();

        if (AdManager.isPremiumUser) {
            AdManager.releaseAllAds();
            Log.d("Splash_Debug", "Status: Premium. Ad engine stopped.");
        } else {
            Log.d("Splash_Debug", "Status: Free. Preloading App Open Ad.");
            AdManager.preloadAppOpen(this);
        }

        startAppFlow();
    }

    private void syncLocalStatus() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        AdManager.isPremiumUser = prefs.getBoolean(KEY_IS_PREMIUM, false);
    }

    private void initBillingCheck() {
        billingManager = new BillingManager(this, new BillingManager.BillingCallback() {
            @Override
            public void onPremiumPurchased() {
                AdManager.isPremiumUser = true;
                Log.d("Splash_Debug", "Sync: User is Premium");
            }

            @Override
            public void onBillingError(int errorCode, String technicalMessage) {
                Log.e("Splash_Debug", "Billing Error: " + errorCode + " | " + technicalMessage);
            }
        });

        billingManager.startConnection();
    }

    private void startAppFlow() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstTime = prefs.getBoolean(KEY_FIRST_TIME, true);

        if (isFirstTime) {
            mainHandler.postDelayed(this::proceedToNext, 1500);
        } else {
            showAdWithTimer();
        }
    }

    private void showAdWithTimer() {
        if (tvAdStatus != null) {
            tvAdStatus.setVisibility(View.VISIBLE);
            tvAdStatus.setText(getString(R.string.splash_ad_disclaimer));
        }

        if (AdManager.isPremiumUser) {
            proceedToNext();
            return;
        }

        AdManager.loadAndShowAppOpenAd(this, new AdManager.AdCallback() {
            @Override
            public void onAdShowed() {
                setAdShowingState();
            }

            @Override
            public void onAdClosed() {
                currentStatus = AppState.PROCEEDED;
                proceedToNext();
            }

            @Override
            public void onAdFailed() {
                proceedToNext();
            }
        });

        adTimer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                if (currentStatus == AppState.LOADING) {
                    proceedToNext();
                }
            }
        }.start();
    }


    public void setAdShowingState() {
        this.currentStatus = AppState.AD_VISIBLE;
        if (adTimer != null) {
            adTimer.cancel();
            adTimer = null;
        }
    }

    private void proceedToNext() {
        if (isFinishing() || isDestroyed() || isNavigating) return;
        if (currentStatus == AppState.AD_VISIBLE) return;

        isNavigating = true;

        Intent intent;
        if (isPermissionPersisted()) {
            intent = new Intent(this, MainActivity.class);
        } else {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(KEY_FIRST_TIME, false).apply();
            intent = new Intent(this, PermissionsActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private boolean isPermissionPersisted() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedUriStr = prefs.getString(KEY_STATUS_FOLDER_URI, null);
        if (savedUriStr == null) return false;
        try {
            Uri targetUri = Uri.parse(savedUriStr);
            return getContentResolver().getPersistedUriPermissions().stream()
                    .anyMatch(p -> p.getUri().equals(targetUri) && p.isReadPermission());
        } catch (Exception e) { return false; }
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onDestroy() {
        if (adTimer != null) adTimer.cancel();
        if (billingManager != null) billingManager.endConnection();
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}