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
    private static boolean isTesting = true;  // testing mode

    private static final String TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    private static final String REAL_INTERSTITIAL = "YOUR_INTERSTITIAL_AD_ID";

    // Initialize AdMob
    public static void init(Context context) {
        MobileAds.initialize(context, initializationStatus -> {});
    }

    // Get correct ad unit id
    private static String getAdUnit() {
        return isTesting ? TEST_INTERSTITIAL : REAL_INTERSTITIAL;
    }

    // Load Interstitial Ad
    public static void loadInterstitial(Context context) {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(
                context,
                getAdUnit(),
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        interstitialAd = ad;
                        Log.d("AdManager", "Interstitial Loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadError) {
                        interstitialAd = null;
                        Log.e("AdManager", "Failed to load: " + loadError.getMessage());
                    }
                }
        );
    }

    // Show Interstitial Ad without callback
    public static void showInterstitial(Activity activity) {
        showInterstitial(activity, null);
    }

    // Callback interface
    public interface AdCallback {
        void onAdClosed();
    }

    // Show Interstitial Ad with callback
    public static void showInterstitial(Activity activity, AdCallback callback) {
        if (interstitialAd != null) {
            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    interstitialAd = null;
                    loadInterstitial(activity); // Load next ad
                    if (callback != null) callback.onAdClosed();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    interstitialAd = null;
                    loadInterstitial(activity);
                    if (callback != null) callback.onAdClosed();
                }
            });
            interstitialAd.show(activity);
        } else {
            Log.d("AdManager", "Interstitial not loaded.");
            loadInterstitial(activity);
            if (callback != null) callback.onAdClosed(); // call immediately if ad not ready
        }
    }

    // Check if Ad is loaded
    public static boolean isAdLoaded() {
        return interstitialAd != null;
    }
}
