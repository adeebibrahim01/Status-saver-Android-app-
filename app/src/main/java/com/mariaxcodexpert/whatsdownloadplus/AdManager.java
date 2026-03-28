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
    private static final boolean TESTING = false;
    private static final String TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    private static final String REAL_INTERSTITIAL = "ca-app-pub-9822767396000072/9444114867";

    private static InterstitialAd mInterstitialAd = null;
    private static boolean isAdLoading = false;
    private static int retryCount = 0;
    private static final int MAX_RETRY = 5; // Thoda zyada retry limit for poor internet
    private static final Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * App start hote hi Application class ya Splash mein call karein.
     */
    public static void init(Context context) {
        preloadAd(context);
    }

    /**
     * Preload Logic: Isko hamesha Application Context milna chahiye.
     */
    public static void preloadAd(Context context) {
        if (context == null) return;

        // Hamesha application context use karein taaki memory leak na ho
        final Context appContext = context.getApplicationContext();

        if (mInterstitialAd != null || isAdLoading) {
            return;
        }

        // GDPR Consent Check
        if (!canRequestAds()) {
            Log.d(TAG, "Consent missing. Preload cancelled.");
            return;
        }

        isAdLoading = true;
        String adUnitId = TESTING ? TEST_INTERSTITIAL : REAL_INTERSTITIAL;
        AdRequest adRequest = new AdRequest.Builder().build();

        Log.d(TAG, "Loading Interstitial...");

        InterstitialAd.load(appContext, adUnitId, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                mInterstitialAd = ad;
                isAdLoading = false;
                retryCount = 0; // Reset retries on success
                Log.d(TAG, "Ad Loaded Successfully ✔");

                // Pre-setting callbacks taaki show ke waqt delay na ho
                setupDefaultCallbacks(appContext);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                mInterstitialAd = null;
                isAdLoading = false;
                Log.e(TAG, "Ad Failed: " + error.getMessage());

                // Exponential Backoff: Agli koshish thodi der baad
                if (retryCount < MAX_RETRY) {
                    retryCount++;
                    long delay = (long) Math.pow(2, retryCount) * 1000; // 2s, 4s, 8s...
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
                Log.d(TAG, "Ad dismissed. Preloading next ad immediately...");
                mInterstitialAd = null;
                // JAISE HI AD BAND HO, NEXT AD LOAD SHURU KER DO
                preloadAd(appContext);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                mInterstitialAd = null;
                preloadAd(appContext);
            }
        });
    }

    /**
     * Show Logic: Status download button par isko call karein.
     */
    public static void showInterstitial(Activity activity, AdCallback callback) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            if (callback != null) callback.onAdClosed();
            return;
        }

        if (mInterstitialAd != null) {
            // Callback update karein taaki UI listener ko trigger kiya ja sakay
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    mInterstitialAd = null;
                    Log.d(TAG, "Ad Closed. Triggering next action.");

                    // Sabse pehle next ad load pe lagayein background mein
                    preloadAd(activity.getApplicationContext());

                    // Phir user ka kam hone dein (Status download etc)
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
            Log.d(TAG, "Ad Not Ready. Downloading status directly.");
            preloadAd(activity.getApplicationContext());
            if (callback != null) callback.onAdClosed();
        }
    }

    public static boolean isAdLoaded() {
        return mInterstitialAd != null;
    }

    public static boolean canRequestAds() {
        // Strict Check: Consent manager agar null hai to false dein (safety)
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