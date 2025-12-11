package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.lang.ref.WeakReference;

public class AdManager {

    private static final String TAG = "AdManager";

    private static final String TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    private static final String REAL_INTERSTITIAL = "ca-app-pub-9822767396000072/9444114867";

    private static InterstitialAd interstitialAd;
    private static boolean isLoading = false;
    private static final Handler handler = new Handler(Looper.getMainLooper());

    // -----------------------
    // Initialize ads (with consent check)
    // -----------------------
    public static void init(Context context) {
        ConsentFormManager consent = ConsentFormManager.getInstance();

        if (consent != null) {
            // EU user → request consent first
            consent.requestConsentForm(() -> loadInterstitial(context));
        } else {
            // Non-EU → load ad directly
            loadInterstitial(context);
        }
    }

    // -----------------------
    // Load interstitial ad
    // -----------------------
    public static void loadInterstitial(Context context) {
        if (interstitialAd != null || isLoading) return;

        isLoading = true;
        WeakReference<Context> weak = new WeakReference<>(context);
        AdRequest request = new AdRequest.Builder().build();

        InterstitialAd.load(context, REAL_INTERSTITIAL, request, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                interstitialAd = ad;
                isLoading = false;
                Log.d(TAG, "Interstitial Loaded ✅");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                interstitialAd = null;
                isLoading = false;
                Log.e(TAG, "Ad Failed: " + error.getMessage());
                showToast(context, "Ad failed to load");

                handler.postDelayed(() -> {
                    Context ctx = weak.get();
                    if (ctx != null) loadInterstitial(ctx);
                }, 4000);
            }
        });
    }

    // -----------------------
    // Show interstitial ad
    // -----------------------
    public static void showInterstitial(Activity activity, AdCallback adCallback) {
        if (interstitialAd == null) {
            // No ad → run callback immediately
            if (adCallback != null) adCallback.onAdClosed();
            loadInterstitial(activity); // preload for next
            return;
        }

        WeakReference<Activity> weak = new WeakReference<>(activity);

        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                interstitialAd = null;
                Activity act = weak.get();
                if (act != null) loadInterstitial(act); // preload next ad
                if (adCallback != null) adCallback.onAdClosed(); // 🔥 trigger callback after ad closes
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                interstitialAd = null;
                Activity act = weak.get();
                if (act != null) loadInterstitial(act); // preload next ad
                if (adCallback != null) adCallback.onAdClosed(); // 🔥 trigger callback if ad fails
            }
        });

        interstitialAd.show(activity);
    }

    // -----------------------
    // Check if interstitial ad is loaded
    // -----------------------
    public static boolean isAdLoaded() {
        return interstitialAd != null;
    }

    // -----------------------
    // Check if ads can be requested (EU consent)
    // -----------------------
    public static boolean canRequestAds() {
        ConsentFormManager consent = ConsentFormManager.getInstance();
        if (consent == null) return true; // Non-EU fallback
        return consent.canRequestAds();
    }

    // -----------------------
    // Ad callback interface
    // -----------------------
    public interface AdCallback {
        void onAdClosed();
    }

    private static void showToast(final Context context, final String msg) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        );
    }
}
