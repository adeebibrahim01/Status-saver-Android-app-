package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Date;

/**
 * 🚀 ULTRA-ADVANCE AD ENGINE v4.0 (Analytics & Production Stable)
 */
public class AdManager {

    private static final String TAG = "AdManager_System";

    // 🔥 PREMIUM STATUS FLAG
    public static boolean isPremiumUser = false;

    private static final boolean TESTING = true;

    private static final String TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    private static final String REAL_INTERSTITIAL = "ca-app-pub-9822767396000072/9444114867";

    private static final String TEST_APP_OPEN = "ca-app-pub-3940256099942544/9257395921";
    private static final String REAL_APP_OPEN = "ca-app-pub-9822767396000072/3854626938";

    private static InterstitialAd mInterstitialAd = null;
    private static AppOpenAd mAppOpenAd = null;
    private static long loadTime = 0;

    private static boolean isAdLoading = false;
    private static boolean isAppOpenLoading = false;
    private static final Handler mHandler = new Handler(Looper.getMainLooper());

    // 🔥 Firebase Analytics instance
    private static FirebaseAnalytics mFirebaseAnalytics;

    public static void init(Context context) {
        // Initialize Analytics first
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);

        if (isPremiumUser) {
            Log.d(TAG, "Premium Active: Skipping Ads Initialization");
            return;
        }

        runOnMainThread(() -> {
            preloadInterstitial(context);
            preloadAppOpen(context);
        });
    }

    // 🔥 Helper for Analytics Tracking
    private static void logAdEvent(String adType) {
        if (mFirebaseAnalytics != null && !isPremiumUser) {
            Bundle bundle = new Bundle();
            bundle.putString("ad_type", adType);
            mFirebaseAnalytics.logEvent("ad_impression_success", bundle);
            Log.d(TAG, "Analytics Logged: " + adType);
        }
    }

    public static void preloadAppOpen(Context context) {
        if (isPremiumUser) return;
        if (context == null || isAppOpenLoading || mAppOpenAd != null) return;
        if (!canRequestAds()) return;

        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnMainThread(() -> preloadAppOpen(context));
            return;
        }

        isAppOpenLoading = true;
        String adUnitId = TESTING ? TEST_APP_OPEN : REAL_APP_OPEN;
        AdRequest request = getAdRequest();

        AppOpenAd.load(context.getApplicationContext(), adUnitId, request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                mAppOpenAd = ad;
                isAppOpenLoading = false;
                loadTime = new Date().getTime();
                Log.d(TAG, "AppOpen: Preloaded Successfully ✔");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                isAppOpenLoading = false;
                mAppOpenAd = null;
                Log.e(TAG, "AppOpen Load Failed: " + loadAdError.getMessage());
            }
        });
    }

    public static void loadAndShowAppOpenAd(Activity activity, AdCallback callback) {
        if (isPremiumUser) {
            if (callback != null) callback.onAdClosed();
            return;
        }

        runOnMainThread(() -> {
            if (!canRequestAds()) {
                if (callback != null) callback.onAdClosed();
                return;
            }

            if (mAppOpenAd != null && isAdExpired()) {
                mAppOpenAd = null;
                preloadAppOpen(activity);
            }

            if (mAppOpenAd != null) {
                showPreloadedAppOpen(activity, callback);
            } else {
                fetchFreshAppOpen(activity, callback);
            }
        });
    }

    private static void fetchFreshAppOpen(Activity activity, AdCallback callback) {
        if (isPremiumUser || isAppOpenLoading) return;

        isAppOpenLoading = true;
        String adUnitId = TESTING ? TEST_APP_OPEN : REAL_APP_OPEN;

        Runnable timeout = () -> {
            if (isAppOpenLoading) {
                isAppOpenLoading = false;
                Log.d(TAG, "AppOpen Fetch Timeout!");
                if (callback != null) callback.onAdFailed();
            }
        };
        mHandler.postDelayed(timeout, 3000);

        AppOpenAd.load(activity, adUnitId, new AdRequest.Builder().build(), new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                mAppOpenAd = ad;
                isAppOpenLoading = false;
                mHandler.removeCallbacks(timeout);
                showPreloadedAppOpen(activity, callback);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                isAppOpenLoading = false;
                mHandler.removeCallbacks(timeout);
                if (callback != null) callback.onAdFailed();
            }
        });
    }

    public static void releaseAllAds() {
        isPremiumUser = true;
        runOnMainThread(() -> {
            if (mInterstitialAd != null) {
                mInterstitialAd = null;
                Log.d(TAG, "Interstitial Ad Released (User is Premium)");
            }
            if (mAppOpenAd != null) {
                mAppOpenAd = null;
                Log.d(TAG, "AppOpen Ad Released (User is Premium)");
            }
        });
    }

    private static void showPreloadedAppOpen(Activity activity, AdCallback callback) {
        if (isPremiumUser || activity == null || activity.isFinishing() || mAppOpenAd == null) {
            if (callback != null) callback.onAdFailed();
            return;
        }

        mAppOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                mAppOpenAd = null;
                preloadAppOpen(activity);
                if (callback != null) callback.onAdClosed();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                mAppOpenAd = null;
                if (callback != null) callback.onAdFailed();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                // 🔥 TRACK APP OPEN
                logAdEvent("AppOpen");
                if (callback != null) callback.onAdShowed();
                if (activity.getClass().getSimpleName().equals("Splash_screen")) {
                    try {
                        activity.getClass().getMethod("setAdShowingState").invoke(activity);
                    } catch (Exception ignored) {}
                }
            }
        });

        mAppOpenAd.show(activity);
    }

    public static void preloadInterstitial(Context context) {
        if (isPremiumUser || context == null || isAdLoading || mInterstitialAd != null) return;
        if (!canRequestAds()) return;

        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnMainThread(() -> preloadInterstitial(context));
            return;
        }

        isAdLoading = true;
        String adUnitId = TESTING ? TEST_INTERSTITIAL : REAL_INTERSTITIAL;

        InterstitialAd.load(context.getApplicationContext(), adUnitId, new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        mInterstitialAd = ad;
                        isAdLoading = false;
                        Log.d(TAG, "Interstitial: Loaded ✔");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        isAdLoading = false;
                        mInterstitialAd = null;
                    }
                });
    }

    public static void showInterstitial(Activity activity, AdCallback callback) {
        if (isPremiumUser) {
            if (callback != null) callback.onAdClosed();
            return;
        }

        runOnMainThread(() -> {
            if (!canRequestAds() || mInterstitialAd == null) {
                preloadInterstitial(activity);
                if (callback != null) callback.onAdFailed();
                return;
            }

            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    mInterstitialAd = null;
                    preloadInterstitial(activity);
                    if (callback != null) callback.onAdClosed();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    mInterstitialAd = null;
                    preloadInterstitial(activity);
                    if (callback != null) callback.onAdFailed();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    // 🔥 TRACK INTERSTITIAL
                    logAdEvent("Interstitial");
                    if (callback != null) callback.onAdShowed();
                }
            });

            mInterstitialAd.show(activity);
        });
    }

    private static AdRequest getAdRequest() {
        AdRequest.Builder builder = new AdRequest.Builder();
        if (ConsentFormManager.getInstance() != null &&
                !ConsentFormManager.getInstance().canRequestAds()) {
            builder.addNetworkExtrasBundle(com.google.ads.mediation.admob.AdMobAdapter.class,
                    ConsentFormManager.getNonPersonalizedBundle());
        }
        return builder.build();
    }

    public static void loadBannerAd(Activity activity, AdView adView) {
        runOnMainThread(() -> {
            if (isPremiumUser || adView == null || !canRequestAds()) {
                if (adView != null) {
                    adView.setVisibility(View.GONE);
                }
                return;
            }

            adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    adView.setVisibility(View.VISIBLE);
                }
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                    adView.setVisibility(View.GONE);
                }
                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    // 🔥 TRACK BANNER
                    logAdEvent("Banner");
                }
            });

            adView.loadAd(new AdRequest.Builder().build());
        });
    }

    private static void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mHandler.post(runnable);
        }
    }

    private static boolean isAdExpired() {
        return (new Date().getTime() - loadTime) > (3600000 * 4);
    }

    public static boolean isAppOpenAdLoaded() {
        if (isPremiumUser) return false;
        return mAppOpenAd != null && !isAdExpired();
    }

    public static boolean canRequestAds() {
        if (isPremiumUser) return false;
        try {
            return ConsentFormManager.getInstance() != null && ConsentFormManager.getInstance().canRequestAds();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isInterstitialLoaded() {
        if (isPremiumUser) return false;
        return mInterstitialAd != null;
    }

    public interface AdCallback {
        default void onAdShowed() {}
        void onAdClosed();
        default void onAdFailed() { onAdClosed(); }
    }
}