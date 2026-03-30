package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class AdManager {

    private static final String TAG = "AdManager";

    // -----------------------
    // CONFIGURATION
    // -----------------------
    private static final boolean TESTING = false;

    // Interstitial IDs
    private static final String TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    private static final String REAL_INTERSTITIAL = "ca-app-pub-9822767396000072/9444114867";

    private static InterstitialAd mInterstitialAd = null;
    private static boolean isAdLoading = false;
    private static int retryCount = 0;
    private static final int MAX_RETRY = 5;
    private static final Handler mHandler = new Handler(Looper.getMainLooper());

    public static void init(Context context) {
        preloadAd(context);
    }

    // ==========================================
    // BANNER AD LOGIC (FIXED FOR XML)
    // ==========================================

    /**
     * Isko call karne se pehle XML mein adSize aur adUnitId hona lazmi hai.
     */
    public static void loadBannerAd(Activity activity, AdView adView) {
        if (adView == null) return;

        // 1. Consent Check
        if (!canRequestAds()) {
            adView.setVisibility(View.GONE);
            Log.d(TAG, "Banner: No consent, hiding ad view.");
            return;
        }

        try {
            // 🔥 FIX: Java se ab kuch bhi SET nahi karenge kyunki XML mein define hai.
            // Sirf loadAd call karenge jo XML wali ID aur Size utha lega.

            AdRequest adRequest = new AdRequest.Builder().build();

            adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    adView.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Banner Loaded Successfully ✔");
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                    adView.setVisibility(View.GONE);
                    Log.e(TAG, "Banner Failed: " + adError.getMessage());
                }
            });

            adView.loadAd(adRequest);

        } catch (Exception e) {
            Log.e(TAG, "Banner Error: " + e.getMessage());
        }
    }

    // ==========================================
    // INTERSTITIAL AD LOGIC (REMAINS SAME)
    // ==========================================

    public static void preloadAd(Context context) {
        if (context == null) return;
        final Context appContext = context.getApplicationContext();

        if (mInterstitialAd != null || isAdLoading) return;

        if (!canRequestAds()) {
            Log.d(TAG, "Consent missing. Preload cancelled.");
            return;
        }

        isAdLoading = true;
        String adUnitId = TESTING ? TEST_INTERSTITIAL : REAL_INTERSTITIAL;
        AdRequest adRequest = new AdRequest.Builder().build();

        Log.d(TAG, "Loading Interstitial (" + (TESTING ? "TEST" : "REAL") + ")...");

        InterstitialAd.load(appContext, adUnitId, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                mInterstitialAd = ad;
                isAdLoading = false;
                retryCount = 0;
                Log.d(TAG, "Interstitial Loaded ✔");
                setupDefaultCallbacks(appContext);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                mInterstitialAd = null;
                isAdLoading = false;
                Log.e(TAG, "Interstitial Failed: " + error.getMessage());

                if (retryCount < MAX_RETRY) {
                    retryCount++;
                    long delay = (long) Math.pow(2, retryCount) * 1000;
                    mHandler.postDelayed(() -> preloadAd(appContext), delay);
                }
            }
        });
    }

    private static void setupDefaultCallbacks(Context appContext) {
        if (mInterstitialAd == null) return;
        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                mInterstitialAd = null;
                preloadAd(appContext);
            }
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                mInterstitialAd = null;
                preloadAd(appContext);
            }
        });
    }

    public static void showInterstitial(Activity activity, AdCallback callback) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            if (callback != null) callback.onAdClosed();
            return;
        }

        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    mInterstitialAd = null;
                    preloadAd(activity.getApplicationContext());
                    if (callback != null) callback.onAdClosed();
                }
                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                    mInterstitialAd = null;
                    preloadAd(activity.getApplicationContext());
                    if (callback != null) callback.onAdClosed();
                }
            });
            mInterstitialAd.show(activity);
        } else {
            preloadAd(activity.getApplicationContext());
            if (callback != null) callback.onAdClosed();
        }
    }

    public static boolean isAdLoaded() {
        return mInterstitialAd != null;
    }

    public static boolean canRequestAds() {
        try {
            ConsentFormManager consent = ConsentFormManager.getInstance();
            return consent != null && consent.canRequestAds();
        } catch (Exception e) {
            return false;
        }
    }

    public interface AdCallback {
        void onAdClosed();
    }
}