package com.mariaxcodexpert.whatsdownloadplus.Ads;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashMap;


public class AdManager {

    private static final String TAG = "AdManager_System";

    public static boolean isPremiumUser = false;
    private static final boolean TESTING = false;

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

    private static FirebaseAnalytics mFirebaseAnalytics;

    // 🔥 Cooldown Variables (Values in Seconds for Firebase)
    private static long appStartTime = 0;
    private static long lastAdShownTime = 0;
    private static boolean firstAdShown = false;

    // 🔥 Default Values (30 sec aur 120 sec/2 min)
    private static long INITIAL_DELAY_MILLIS = 30000;
    private static long COOLDOWN_MILLIS = 120000;

    public static void init(Context context) {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        appStartTime = System.currentTimeMillis();

        fetchFirebaseSettings();

        if (isPremiumUser) return;

        runOnMainThread(() -> {
            preloadAppOpen(context);
            preloadInterstitial(context);
        });
    }

    private static void fetchFirebaseSettings() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("ad_settings");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long initial = snapshot.child("initial_delay_sec").getValue(Long.class);
                    Long interval = snapshot.child("interstitial_interval_sec").getValue(Long.class);

                    if (initial != null) INITIAL_DELAY_MILLIS = initial * 1000;
                    if (interval != null) COOLDOWN_MILLIS = interval * 1000;

                    Log.d(TAG, "Firebase Live Update: Initial=" + INITIAL_DELAY_MILLIS + "ms, Cooldown=" + COOLDOWN_MILLIS + "ms");
                } else {
                    dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot innerSnapshot) {
                            if (!innerSnapshot.exists()) {
                                HashMap<String, Object> defaults = new HashMap<>();
                                defaults.put("initial_delay_sec", 30);
                                defaults.put("interstitial_interval_sec", 120);
                                dbRef.setValue(defaults);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase Live Error: " + error.getMessage());
            }
        });
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

                Log.e(TAG, "AppOpen Load Failed: "
                        + loadAdError.getMessage()
                        + " | Code: "
                        + loadAdError.getCode()
                        + ". Retrying in 5s...");

                Context appContext = context.getApplicationContext();

                mHandler.postDelayed(() -> {

                    if (!isAppOpenLoading &&
                            mAppOpenAd == null &&
                            canRequestAds()) {

                        preloadAppOpen(appContext);
                    }

                }, 5000);
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

    private static void showPreloadedAppOpen(Activity activity, AdCallback callback) {
        if (isPremiumUser || activity == null || activity.isFinishing() || activity.isDestroyed() || mAppOpenAd == null) {
            preloadInterstitial(activity);
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
                preloadInterstitial(activity);
                if (callback != null) callback.onAdFailed();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                logAdEvent("AppOpen");
                preloadInterstitial(activity);
                if (callback != null) callback.onAdShowed();
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

                        Log.e(TAG, "Interstitial Load Failed: "
                                + loadAdError.getMessage()
                                + " | Code: "
                                + loadAdError.getCode()
                                + ". Retrying in 5s...");

                        Context appContext = context.getApplicationContext();

                        mHandler.postDelayed(() -> {

                            if (!isAdLoading &&
                                    mInterstitialAd == null &&
                                    canRequestAds()) {

                                preloadInterstitial(appContext);
                            }

                        }, 5000);
                    }
                });
    }

    public static void showInterstitial(Activity activity, AdCallback callback) {

        if (isPremiumUser) {
            if (callback != null) callback.onAdClosed();
            return;
        }

        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.e(TAG, "Interstitial: Activity is invalid/destroyed.");
            if (callback != null) callback.onAdClosed();
            return;
        }

        final WeakReference<Activity> activityRef = new WeakReference<>(activity);

        runOnMainThread(() -> {
            Activity act = activityRef.get();
            if (act == null || act.isFinishing() || act.isDestroyed()) {
                if (callback != null) callback.onAdClosed();
                return;
            }

            long currentTime = System.currentTimeMillis();
            boolean isCooldownActive;

            if (!firstAdShown) {
                isCooldownActive = (currentTime - appStartTime < INITIAL_DELAY_MILLIS);
            } else {
                isCooldownActive = (currentTime - lastAdShownTime < COOLDOWN_MILLIS);
            }

            if (isCooldownActive) {
                long remaining = (!firstAdShown) ?
                        (INITIAL_DELAY_MILLIS - (currentTime - appStartTime)) :
                        (COOLDOWN_MILLIS - (currentTime - lastAdShownTime));

                Log.d(TAG, "Interstitial: Cooldown Active. Skipping. (" + (remaining / 1000) + "s left)");
                if (callback != null) callback.onAdClosed();
                return;
            }

            if (mInterstitialAd == null) {
                Log.e(TAG, "Interstitial: Not ready. Preloading for next time.");
                preloadInterstitial(act);
                if (callback != null) callback.onAdClosed();
                return;
            }

            String providerName = "Unknown";
            try {
                if (mInterstitialAd.getResponseInfo() != null &&
                        mInterstitialAd.getResponseInfo().getLoadedAdapterResponseInfo() != null) {
                    providerName = mInterstitialAd.getResponseInfo()
                            .getLoadedAdapterResponseInfo().getAdSourceInstanceName();
                }
            } catch (Exception e) {
                Log.e(TAG, "Mediation Info Error: " + e.getMessage());
            }

            final String finalProvider = providerName;
            Log.i(TAG, "🟢 [MEDIATION] Attempting to show: " + finalProvider);

            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial: Dismissed (" + finalProvider + ")");
                    mInterstitialAd = null;
                    lastAdShownTime = System.currentTimeMillis();

                    // Agli Activity ke liye background mein ad load karein
                    Activity currentAct = activityRef.get();
                    if (currentAct != null) preloadInterstitial(currentAct);

                    if (callback != null) callback.onAdClosed();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.e(TAG, finalProvider + " Failed to show: " + adError.getMessage());
                    mInterstitialAd = null;

                    Activity currentAct = activityRef.get();
                    if (currentAct != null) preloadInterstitial(currentAct);

                    if (callback != null) callback.onAdClosed();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    Log.i(TAG, "🟢 [SUCCESS] " + finalProvider + " Showed Successfully ✔");
                    logAdEvent("Interstitial_" + finalProvider);

                    firstAdShown = true;
                    lastAdShownTime = System.currentTimeMillis();

                    if (callback != null) callback.onAdShowed();
                }
            });

            try {
                mInterstitialAd.show(act);
            } catch (Exception e) {
                Log.e(TAG, "Fatal error showing Interstitial: " + e.getMessage());
                mInterstitialAd = null;
                if (callback != null) callback.onAdClosed();
            }
        });
    }
    private static void logAdEvent(String adType) {
        if (mFirebaseAnalytics != null && !isPremiumUser) {
            Bundle bundle = new Bundle();
            bundle.putString("ad_type", adType);
            mFirebaseAnalytics.logEvent("ad_impression_success", bundle);
        }
    }
    private static AdRequest getAdRequest() {
        AdRequest.Builder builder = new AdRequest.Builder();

        if (ConsentFormManager.getInstance() != null) {
            if (!ConsentFormManager.getInstance().canRequestAds()) {
                builder.addNetworkExtrasBundle(com.google.ads.mediation.admob.AdMobAdapter.class,
                        ConsentFormManager.getNonPersonalizedBundle());
            }
        }

        return builder.build();
    }
    public static void loadBannerAd(Activity activity, AdView adView) {
        runOnMainThread(() -> {
            if (isPremiumUser || adView == null || !canRequestAds()) {
                if (adView != null) adView.setVisibility(View.GONE);
                return;
            }

            adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    adView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError adError) {

                    super.onAdFailedToLoad(adError);

                    adView.setVisibility(View.GONE);

                    Log.e(TAG, "Banner Load Failed: "
                            + adError.getMessage()
                            + " | Code: "
                            + adError.getCode()
                            + ". Retrying in 10s...");


                    mHandler.postDelayed(() -> {

                        if (!isPremiumUser &&
                                canRequestAds() &&
                                adView != null) {

                            adView.loadAd(getAdRequest());
                        }

                    }, 10000);
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    logAdEvent("Banner");
                }
            });

            adView.loadAd(getAdRequest());
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

    public static boolean canRequestAds() {
        if (isPremiumUser) return false;

        try {
            ConsentFormManager consentManager = ConsentFormManager.getInstance();
            if (consentManager == null) {
                return false;
            }
            return consentManager.canRequestAds();
        } catch (Exception e) {
            Log.e(TAG, "canRequestAds Error: " + e.getMessage());
            return false;
        }
    }

    public static boolean isInterstitialLoaded() {
        return mInterstitialAd != null;
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

    public static boolean isAppOpenAdLoaded() {
        if (isPremiumUser) return false;
        return mAppOpenAd != null && !isAdExpired();
    }
    public interface AdCallback {
        default void onAdShowed() {}
        void onAdClosed();
        default void onAdFailed() { onAdClosed(); }
    }
}