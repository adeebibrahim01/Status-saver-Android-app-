package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.AdError;

public class AdManager {

    private static InterstitialAd interstitialAd;
    private static boolean isTesting = false;  // toggle test/real ads

    private static final String TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    private static final String REAL_INTERSTITIAL = "ca-app-pub-9822767396000072/9444114867";

    private static boolean isLoading = false; // prevent multiple simultaneous loads

    // -----------------------
    // Initialize AdMob
    // -----------------------
    public static void init(Context context) {
        MobileAds.initialize(context, initializationStatus -> {
            Log.d("AdManager", "AdMob Initialized");
        });
    }

    // -----------------------
    // Get current ad unit
    // -----------------------
    private static String getAdUnit() {
        return isTesting ? TEST_INTERSTITIAL : REAL_INTERSTITIAL;
    }

    // -----------------------
    // Load Interstitial Ad
    // -----------------------
    public static void loadInterstitial(Context context) {
        if (interstitialAd != null || isLoading) return; // already loaded or loading

        isLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(context, getAdUnit(), adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(InterstitialAd ad) {
                interstitialAd = ad;
                isLoading = false;
                Log.d("AdManager", "Interstitial Loaded ✅");
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadError) {
                interstitialAd = null;
                isLoading = false;
                Log.e("AdManager", "Failed to load interstitial: " + loadError.getMessage());
                // Retry after short delay (optional)
                context.getMainLooper().getQueue().addIdleHandler(() -> {
                    loadInterstitial(context);
                    return false;
                });
            }
        });
    }

    // -----------------------
    // Show Interstitial Ad
    // -----------------------
    public static void showInterstitial(Activity activity) {
        showInterstitial(activity, null);
    }

    public interface AdCallback {
        void onAdClosed();
    }

    public static void showInterstitial(Activity activity, AdCallback callback) {
        if (interstitialAd != null) {
            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d("AdManager", "Interstitial Closed");
                    interstitialAd = null;
                    loadInterstitial(activity); // preload next
                    if (callback != null) callback.onAdClosed();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    Log.e("AdManager", "Interstitial failed to show: " + adError.getMessage());
                    interstitialAd = null;
                    loadInterstitial(activity); // preload next
                    if (callback != null) callback.onAdClosed();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d("AdManager", "Interstitial Shown");
                }
            });
            interstitialAd.show(activity);
        } else {
            Log.d("AdManager", "Interstitial not ready, loading now...");
            loadInterstitial(activity);
            if (callback != null) callback.onAdClosed(); // immediately call callback
        }
    }

    // -----------------------
    // Check if ad is ready
    // -----------------------
    public static boolean isAdLoaded() {
        return interstitialAd != null;
    }

    // -----------------------
    // Optional: toggle test mode
    // -----------------------
    public static void setTesting(boolean testing) {
        isTesting = testing;
    }
}
