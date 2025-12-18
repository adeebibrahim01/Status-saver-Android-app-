package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.lang.ref.WeakReference;

public class AdManager {

    private static final String TAG = "AdManager";

    // -----------------------
    // TESTING MODE
    // -----------------------
    private static final boolean TESTING = false;
    private static final String TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    private static final String REAL_INTERSTITIAL = "ca-app-pub-9822767396000072/9444114867";

    private static InterstitialAd interstitialAd;
    private static boolean isLoading = false;
    private static int retryCount = 0;
    private static final int MAX_RETRY = 5;
    private static final Handler handler = new Handler(Looper.getMainLooper());

    private static WeakReference<Activity> lastActivity;

    // ===========================================================
    // INIT
    // ===========================================================
    public static void init(Context context) {
        ConsentFormManager consent = ConsentFormManager.getInstance();

        if (consent != null) {
            consent.requestConsentForm(() -> preloadAd(context));
        } else {
            preloadAd(context);
        }
    }

    // ===========================================================
    // PRELOAD AD
    // ===========================================================
    private static void preloadAd(Context context) {
        if (interstitialAd != null || isLoading) return;

        ConsentFormManager consent = ConsentFormManager.getInstance();
        boolean canRequest = consent == null || consent.canRequestAds();

        isLoading = true;
        String adUnitId = TESTING ? TEST_INTERSTITIAL : REAL_INTERSTITIAL;
        Log.d(TAG, "Preloading Interstitial: " + (TESTING ? "TEST ID" : "REAL ID") +
                " | CanRequestAds: " + canRequest);

        WeakReference<Context> weakContext = new WeakReference<>(context);

        AdRequest.Builder builder = new AdRequest.Builder();

        if (consent != null && !consent.canRequestAds()) {
            // Non-personalized ad if user didn't consent
            builder.addNetworkExtrasBundle(AdMobAdapter.class,
                    ConsentFormManager.getNonPersonalizedBundle());
        }

        AdRequest request = builder.build();

        InterstitialAd.load(context, adUnitId, request, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                interstitialAd = ad;
                isLoading = false;
                retryCount = 0;
                Log.d(TAG, "Interstitial Loaded ✔");

                interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad dismissed, preloading next");
                        interstitialAd = null;
                        Activity act = lastActivity != null ? lastActivity.get() : null;
                        if (act != null) preloadAd(act);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError error) {
                        Log.e(TAG, "Ad failed to show: " + error.getMessage());
                        interstitialAd = null;
                        Activity act = lastActivity != null ? lastActivity.get() : null;
                        if (act != null) preloadAd(act);
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                interstitialAd = null;
                isLoading = false;
                Log.e(TAG, "Ad Failed to Load: " + error.getMessage());

                if (retryCount < MAX_RETRY) {
                    retryCount++;
                    long delay = Math.min((long) Math.pow(2, retryCount) * 1000, 15000);
                    handler.postDelayed(() -> {
                        Context ctx = weakContext.get();
                        if (ctx != null) preloadAd(ctx);
                    }, delay);
                }
            }
        });
    }

    // ===========================================================
    // SHOW INTERSTITIAL
    // ===========================================================
    public static void showInterstitial(Activity activity, AdCallback callback) {
        lastActivity = new WeakReference<>(activity);

        if (interstitialAd == null) {
            Log.d(TAG, "Ad not ready, preloading...");
            preloadAd(activity);
            if (callback != null) callback.onAdClosed();
            return;
        }

        interstitialAd.show(activity);
    }

    // ===========================================================
    // CHECK IF LOADED
    // ===========================================================
    public static boolean isAdLoaded() {
        return interstitialAd != null;
    }

    // ===========================================================
    // CAN REQUEST ADS
    // ===========================================================
    public static boolean canRequestAds() {
        ConsentFormManager consent = ConsentFormManager.getInstance();
        return consent == null || consent.canRequestAds();
    }

    // ===========================================================
    // CALLBACK
    // ===========================================================
    public interface AdCallback {
        void onAdClosed();
        void onAdFailedToShow();
        void onAdShown();

        void onAdFailed();
    }
}
