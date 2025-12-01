package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;

public class AdManager {

    private static RewardedInterstitialAd rewardedInterstitialAd;
    private static boolean isTesting = true; // true = test ads, false = real ads

    private static final String TEST_REWARDED_INTERSTITIAL = "ca-app-pub-3940256099942544/5354046379";
    private static final String REAL_REWARDED_INTERSTITIAL = "YOUR_REWARDED_INTERSTITIAL_ID";

    private static final String PREFS_NAME = "AdPrefs";
    private static final String KEY_FREE_DOWNLOADS = "freeDownloads";

    // Initialize AdMob
    public static void init(Context context) {
        MobileAds.initialize(context, initializationStatus -> {});
    }

    // Return correct ad unit
    private static String getAdUnit() {
        return isTesting ? TEST_REWARDED_INTERSTITIAL : REAL_REWARDED_INTERSTITIAL;
    }

    // Load rewarded interstitial
    public static void loadRewardedInterstitial(Context context) {
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedInterstitialAd.load(
                context,
                getAdUnit(),
                adRequest,
                new RewardedInterstitialAdLoadCallback() {

                    @Override
                    public void onAdLoaded(RewardedInterstitialAd ad) {
                        rewardedInterstitialAd = ad;
                        Log.d("AdManager", "Rewarded Interstitial Loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadError) {
                        rewardedInterstitialAd = null;
                        Log.e("AdManager", "Failed to load: " + loadError.getMessage());
                    }
                }
        );
    }

    // Show ad
    public static void showRewardedInterstitial(Activity activity, RewardListener listener) {
        if (rewardedInterstitialAd != null) {
            rewardedInterstitialAd.show(activity, rewardItem -> {
                listener.onRewardEarned(rewardItem.getAmount(), rewardItem.getType());
                // Give 2 free downloads after reward
                giveFreeDownloads(activity);
            });
        } else {
            Log.e("AdManager", "Rewarded Interstitial Not Loaded");
            listener.onAdNotAvailable();
        }
    }

    public static boolean isAdLoaded() {
        return rewardedInterstitialAd != null;
    }


    // Reward listener interface
    public interface RewardListener {
        void onRewardEarned(int amount, String type);
        void onAdNotAvailable();
    }

    // Check if user should see ad or has free downloads
    public static boolean shouldShowRewardAd(Context context) {
        int free = getFreeDownloads(context);
        return free <= 0;
    }

    // Give 2 free downloads
    private static void giveFreeDownloads(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_FREE_DOWNLOADS, 2).apply();
    }

    // Decrease free downloads after each download
    public static void decreaseFreeDownload(Context context) {
        int free = getFreeDownloads(context);
        if (free > 0) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putInt(KEY_FREE_DOWNLOADS, free - 1).apply();
        }
    }

    // Get current free downloads
    public static int getFreeDownloads(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_FREE_DOWNLOADS, 0);
    }
}
