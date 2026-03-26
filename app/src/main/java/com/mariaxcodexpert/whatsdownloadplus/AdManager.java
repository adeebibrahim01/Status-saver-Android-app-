package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class AdManager {

    private static final String TAG = "AdManager";

    // -----------------------
    // CONFIGURATION
    // -----------------------
    private static final boolean TESTING = false; // Production ke waqt false rakhein
    private static final String TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    private static final String REAL_INTERSTITIAL = "ca-app-pub-9822767396000072/9444114867";

    private static InterstitialAd mInterstitialAd;
    private static boolean isAdLoading = false;
    private static int retryCount = 0;
    private static final int MAX_RETRY = 3;
    private static final Handler mHandler = new Handler(Looper.getMainLooper());

    // ===========================================================
    // INITIALIZATION & PRELOAD
    // ===========================================================
    public static void init(Context context) {
        // App start hote hi preload shuru karein (Application Context safe hai)
        preloadAd(context.getApplicationContext());
    }

    public static void preloadAd(Context context) {
        if (mInterstitialAd != null || isAdLoading) {
            return;
        }

        // Check for User Consent (GDPR)
        if (!canRequestAds()) {
            Log.d(TAG, "Cannot request ads: Consent missing");
            return;
        }

        isAdLoading = true;
        String adUnitId = TESTING ? TEST_INTERSTITIAL : REAL_INTERSTITIAL;

        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(context, adUnitId, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                mInterstitialAd = ad;
                isAdLoading = false;
                retryCount = 0;
                Log.d(TAG, "Ad Loaded Successfully ✔");

                // Default callbacks for unexpected events
                setupDefaultCallbacks(context.getApplicationContext());
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                mInterstitialAd = null;
                isAdLoading = false;
                Log.e(TAG, "Ad Failed to Load: " + error.getMessage());

                // Exponential Backoff logic
                if (retryCount < MAX_RETRY) {
                    retryCount++;
                    long delay = (long) Math.pow(2, retryCount) * 2000;
                    mHandler.postDelayed(() -> preloadAd(context), delay);
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

    // ===========================================================
    // SHOW AD LOGIC
    // ===========================================================
    public static void showInterstitial(Activity activity, AdCallback callback) {
        if (activity == null || activity.isFinishing()) {
            if (callback != null) callback.onAdClosed();
            return;
        }

        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad Dismissed");
                    mInterstitialAd = null;
                    // Preload agla ad (Application context use karein)
                    preloadAd(activity.getApplicationContext());
                    if (callback != null) callback.onAdClosed();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                    Log.e(TAG, "Ad Failed to Show: " + adError.getMessage());
                    mInterstitialAd = null;
                    preloadAd(activity.getApplicationContext());
                    if (callback != null) callback.onAdClosed();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad shown");
                }
            });

            mInterstitialAd.show(activity);
        } else {
            Log.d(TAG, "Ad not ready yet");
            preloadAd(activity.getApplicationContext());
            if (callback != null) callback.onAdClosed();
        }
    }

    // ===========================================================
    // HELPERS
    // ===========================================================

    public static boolean isAdLoaded() {
        return mInterstitialAd != null;
    }

    public static boolean canRequestAds() {
        ConsentFormManager consent = ConsentFormManager.getInstance();
        return consent == null || consent.canRequestAds();
    }

    public interface AdCallback {
        void onAdClosed();
    }
}