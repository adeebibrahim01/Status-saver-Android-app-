package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import com.android.billingclient.api.*;
import java.util.List;

/**
 * 🚀 PREMIUM BILLING ENGINE v7.5 (Production & Security Optimized)
 */
public class BillingManager implements PurchasesUpdatedListener {

    private final BillingClient billingClient;
    private final Activity activity;
    private final BillingCallback callback;
    private static final String TAG = "BillingManager_Secure";
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_IS_PREMIUM = "isPremium";

    public interface BillingCallback {
        void onPremiumPurchased();
        void onBillingError(int errorCode, String technicalMessage);
    }

    public interface PurchaseDetailCallback {
        void onDetailsFound(String type, Purchase purchase);
    }

    public BillingManager(Activity activity, BillingCallback callback) {
        this.activity = activity;
        this.callback = callback;

        this.billingClient = BillingClient.newBuilder(activity)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts() // Security: Ensures pending one-time products are handled
                        .build())
                .build();
    }

    public void startConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Store Connection: Success");
                    checkActivePurchases();
                } else {
                    Log.e(TAG, "Store Connection Failed: " + billingResult.getDebugMessage());
                    if (callback != null) {
                        callback.onBillingError(billingResult.getResponseCode(), billingResult.getDebugMessage());
                    }
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                // Silent retry for better user experience
                Log.w(TAG, "Service Disconnected. Retrying...");
                new Handler(Looper.getMainLooper()).postDelayed(() -> startConnection(), 2000);
            }
        });
    }

    public void checkPurchaseDetails(PurchaseDetailCallback detailCallback) {
        if (!billingClient.isReady()) {
            activity.runOnUiThread(() -> detailCallback.onDetailsFound("none", null));
            return;
        }

        // Parallel checking for Lifetime and Monthly
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                (billingResult, inappList) -> {
                    if (!inappList.isEmpty()) {
                        activity.runOnUiThread(() -> detailCallback.onDetailsFound("lifetime", inappList.get(0)));
                    } else {
                        billingClient.queryPurchasesAsync(
                                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                                (billingResult2, subsList) -> {
                                    if (!subsList.isEmpty()) {
                                        activity.runOnUiThread(() -> detailCallback.onDetailsFound("monthly", subsList.get(0)));
                                    } else {
                                        activity.runOnUiThread(() -> detailCallback.onDetailsFound("none", null));
                                    }
                                }
                        );
                    }
                }
        );
    }

    public void checkActivePurchases() {
        // Step 1: Check Subscriptions (Most frequent check)
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                (billingResult, list) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && !list.isEmpty()) {
                        handlePurchases(list);
                    } else {
                        checkInAppPurchases();
                    }
                }
        );
    }

    private void checkInAppPurchases() {
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                (billingResult, list) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && !list.isEmpty()) {
                        handlePurchases(list);
                    } else {
                        deactivatePremium();
                    }
                }
        );
    }

    private void deactivatePremium() {
        activity.runOnUiThread(() -> {
            SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            if (prefs.getBoolean(KEY_IS_PREMIUM, false)) {
                prefs.edit().putBoolean(KEY_IS_PREMIUM, false).apply();
                AdManager.isPremiumUser = false;
                AdManager.init(activity); // Reload ads for free user
                Log.d(TAG, "Access Revoked: Reverting to Free Version");
            }
        });
    }

    public void buyProduct(String productId, String type) {
        if (!billingClient.isReady()) {
            Toast.makeText(activity, "Google Play is not ready. Check internet.", Toast.LENGTH_SHORT).show();
            return;
        }

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(List.of(QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(type)
                        .build()))
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsResult) -> {
            List<ProductDetails> productDetailsList = productDetailsResult.getProductDetailsList();
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && productDetailsList != null && !productDetailsList.isEmpty()) {

                ProductDetails productDetails = productDetailsList.get(0);
                BillingFlowParams.ProductDetailsParams.Builder productParamsBuilder =
                        BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails);

                // For Subscriptions: Get the latest offer token
                if (type.equals(BillingClient.ProductType.SUBS) && productDetails.getSubscriptionOfferDetails() != null) {
                    productParamsBuilder.setOfferToken(productDetails.getSubscriptionOfferDetails().get(0).getOfferToken());
                }

                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(List.of(productParamsBuilder.build()))
                        .build();

                activity.runOnUiThread(() -> billingClient.launchBillingFlow(activity, flowParams));
            } else {
                Log.e(TAG, "Product Query Error: " + billingResult.getDebugMessage());
                activity.runOnUiThread(() -> Toast.makeText(activity, "Service temporarily busy.", Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
        int responseCode = billingResult.getResponseCode();

        if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases);
        } else if (responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            checkActivePurchases(); // Re-verify and activate
        } else if (responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            // General secure error callback for developer logging
            if (callback != null) {
                callback.onBillingError(responseCode, billingResult.getDebugMessage());
            }
        }
    }

    private void handlePurchases(List<Purchase> purchases) {
        for (Purchase p : purchases) {
            // Security: Only process if state is truly PURCHASED
            if (p.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                if (!p.isAcknowledged()) {
                    AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(p.getPurchaseToken())
                            .build();
                    billingClient.acknowledgePurchase(params, result -> {
                        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            activatePremium();
                        }
                    });
                } else {
                    activatePremium();
                }
            }
        }
    }

    private void activatePremium() {
        activity.runOnUiThread(() -> {
            SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            if (!prefs.getBoolean(KEY_IS_PREMIUM, false)) {
                prefs.edit().putBoolean(KEY_IS_PREMIUM, true).apply();
                AdManager.isPremiumUser = true;
                AdManager.releaseAllAds(); // Instantly kill ads
                Log.d(TAG, "✧ PREMIUM ACTIVATED ✧");
            }
            if (callback != null) callback.onPremiumPurchased();
        });
    }

    public void endConnection() {
        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
        }
    }
}