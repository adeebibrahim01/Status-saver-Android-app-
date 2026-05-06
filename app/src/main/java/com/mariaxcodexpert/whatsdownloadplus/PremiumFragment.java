package com.mariaxcodexpert.whatsdownloadplus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.billingclient.api.BillingClient;

import java.util.Objects;

/**
 * 🚀 GOD LEVEL PREMIUM FRAGMENT (Secure & User-Friendly)
 */
public class PremiumFragment extends Fragment {

    private static final String TAG = "Premium_Secure_Log";
    private BillingManager billingManager;
    private TextView tvStatusMonthly, tvStatusLifetime;
    private View cardMonthly, cardLifetime;
    private AlertDialog progressDialog;
    private boolean isUserInitiatedPurchase = false;
    private TextView PriceMonthly;
    private TextView PriceLifetime;
    private static final String PREF_MONTHLY_PRICE = "pref_monthly_price";
    private static final String PREF_LIFETIME_PRICE = "pref_lifetime_price";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_premium, container, false);

        tvStatusMonthly = view.findViewById(R.id.tvStatusMonthly);
        tvStatusLifetime = view.findViewById(R.id.tvStatusLifetime);
        cardMonthly = view.findViewById(R.id.cardMonthly);
        cardLifetime = view.findViewById(R.id.cardLifetime);
        PriceMonthly = view.findViewById(R.id.tvPriceMonthly);
        PriceLifetime =view.findViewById(R.id.tvPriceLifetime);
        initBilling();
        setupClicks(view);


        return view;
    }

    private void initBilling() {
        if (getActivity() == null) return;

        billingManager = new BillingManager(getActivity(), new BillingManager.BillingCallback() {
            @Override
            public void onPremiumPurchased() {
                if (isAdded()) {
                    updateSubscriptionStatus();
                    if (isUserInitiatedPurchase) {
                        showSuccessStatus();
                        isUserInitiatedPurchase = false;
                    }
                }
            }

            @Override
            public void onBillingError(int errorCode, String technicalMessage) {
                // Developer logic: Log the real error
                Log.e(TAG, "Billing Error Code: " + errorCode + " | " + technicalMessage);

                // User logic: Show only what matters
                handleUserVisibleErrors(errorCode);
            }
        });

        // 1. Connection start karein
        billingManager.startConnection();

        // 2. 🔥 Prices Load Karein (Connection ready hone ka thora wait karna behtar hai)
        // IDs wahi rakhein jo aapne Play Console aur handlePurchase mein use ki hain
        new Handler(Looper.getMainLooper()).postDelayed(this::loadPrices, 1200);
    }

    /**
     * Play Console se local currency (PKR, INR, USD etc) fetch karke UI update karta hai
     */
    private void loadPrices() {
        if (billingManager == null || !isAdded()) return;

        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

        // 1. Professional Placeholders (Khali string se behtar hai)
        // Agar pehli baar hai toh "..." dikhao, warna saved price
        String savedMonthly = prefs.getString(PREF_MONTHLY_PRICE, "Loading...");
        String savedLifetime = prefs.getString(PREF_LIFETIME_PRICE, "Loading...");

        if (PriceMonthly != null) PriceMonthly.setText(savedMonthly);
        if (PriceLifetime != null) PriceLifetime.setText(savedLifetime);

        // 2. Background Fetch with Smooth Transition
        // Monthly
        billingManager.fetchProductPrices("premium_monthly", BillingClient.ProductType.SUBS, price -> {
            if (isAdded() && PriceMonthly != null && !price.equals(savedMonthly)) {
                // Glitch se bachne ke liye halka sa fade animation
                PriceMonthly.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                    PriceMonthly.setText(price);
                    PriceMonthly.animate().alpha(1f).setDuration(200).start();
                    prefs.edit().putString(PREF_MONTHLY_PRICE, price).apply();
                }).start();
            }
        });

        // Lifetime
        billingManager.fetchProductPrices("premium_lifetime", BillingClient.ProductType.INAPP, price -> {
            if (isAdded() && PriceLifetime != null && !price.equals(savedLifetime)) {
                // Smooth transition for Lifetime price
                PriceLifetime.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                    PriceLifetime.setText(price);
                    PriceLifetime.animate().alpha(1f).setDuration(200).start();
                    prefs.edit().putString(PREF_LIFETIME_PRICE, price).apply();
                }).start();
            }
        });
    }
    private void handleUserVisibleErrors(int errorCode) {
        if (!isAdded()) return;

        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();

        String userMessage;
        switch (errorCode) {
            case BillingClient.BillingResponseCode.USER_CANCELED:
                userMessage = "Purchase cancelled.";
                break;
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                userMessage = "You already own this premium pass!";
                updateSubscriptionStatus();
                break;
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
            case BillingClient.BillingResponseCode.NETWORK_ERROR:
                userMessage = "Connection lost. Please check your internet.";
                break;
            default:
                userMessage = "Play Store is busy. Please try again later.";
                break;
        }

        if (isUserInitiatedPurchase) {
            Toast.makeText(getContext(), userMessage, Toast.LENGTH_SHORT).show();
            isUserInitiatedPurchase = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSubscriptionStatus();
    }

    private void updateSubscriptionStatus() {
        if (!isAdded() || getActivity() == null) return;

        SharedPreferences prefs = getActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        boolean isPremium = prefs.getBoolean("isPremium", false);

        // Instant UI update from cache
        if (isPremium) {
            applyPremiumUI("monthly_active"); // Default showing active if cache is true
        }

        // Secure Verification from Server/Play Store
        if (billingManager != null) {
            billingManager.checkPurchaseDetails((type, purchase) -> {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> applyPremiumUI(type));
                }
            });
        }
    }

    private void applyPremiumUI(String type) {
        if (!isAdded()) return;

        // Reset UI to default
        tvStatusMonthly.setVisibility(View.GONE);
        tvStatusLifetime.setVisibility(View.GONE);
        cardMonthly.setAlpha(1.0f);
        cardMonthly.setEnabled(true);
        cardLifetime.setAlpha(1.0f);
        cardLifetime.setEnabled(true);

        if ("lifetime".equals(type)) {
            tvStatusLifetime.setVisibility(View.VISIBLE);
            tvStatusLifetime.setText("ACTIVE");
            cardMonthly.setAlpha(0.5f);
            cardMonthly.setEnabled(false);
            cardLifetime.setEnabled(false);
        } else if ("monthly".equals(type) || "monthly_active".equals(type)) {
            tvStatusMonthly.setVisibility(View.VISIBLE);
            tvStatusMonthly.setText("ACTIVE");
            cardMonthly.setEnabled(false);
            // Monthly user can still upgrade to Lifetime, so cardLifetime stays enabled
        }
    }

    private void showSuccessStatus() {
        if (!isAdded()) return;
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_billing_status, null);

        TextView tvMsg = dialogView.findViewById(R.id.tvDialogMessage);
        tvMsg.setText("✧ Welcome to Elite ✧\nAds Removed Permanently.");

        View pb = dialogView.findViewById(R.id.dialogProgress);
        if (pb != null) pb.setVisibility(View.GONE);

        builder.setView(dialogView);
        builder.setCancelable(false);
        AlertDialog successDialog = builder.create();

        if (successDialog.getWindow() != null) {
            successDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        successDialog.show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (successDialog.isShowing()) successDialog.dismiss();
            if (isAdded() && getActivity() != null) {
                getActivity().onBackPressed();
            }
        }, 3000);
    }

    private void showProgressDialog(String message) {
        if (!isAdded()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_billing_status, null);
        TextView tvMsg = dialogView.findViewById(R.id.tvDialogMessage);
        tvMsg.setText(message);
        builder.setView(dialogView);
        builder.setCancelable(false);
        progressDialog = builder.create();
        if (progressDialog.getWindow() != null) {
            progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        progressDialog.show();
    }

    private void setupClicks(View view) {


        cardMonthly.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            handlePurchase("premium_monthly", BillingClient.ProductType.SUBS);
        });

        cardLifetime.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            handlePurchase("premium_lifetime", BillingClient.ProductType.INAPP);
        });
    }

    private void handlePurchase(String productId, String type) {
        if (billingManager != null) {
            isUserInitiatedPurchase = true;
            showProgressDialog("Verifying with Google...");
            billingManager.buyProduct(productId, type);

            // Safety timeout: Agar Play Store respond nahi karta to dialog hata do
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (progressDialog != null && progressDialog.isShowing() && isUserInitiatedPurchase) {
                    progressDialog.dismiss();
                }
            }, 6000);
        }
    }

    @Override
    public void onDestroy() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
        if (billingManager != null) billingManager.endConnection();
        super.onDestroy();
    }
}