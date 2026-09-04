package com.mariaxcodexpert.whatsdownloadplus.ui.Subscription;

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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.billingclient.api.BillingClient;
import com.mariaxcodexpert.whatsdownloadplus.R;

public class PremiumFragment extends Fragment {

    private static final String TAG = "Premium_Secure_Log";
    private BillingManager billingManager;
    private TextView tvStatusMonthly, tvStatusLifetime;
    private View cardMonthly, cardLifetime;
    private AlertDialog progressDialog;
    private boolean isUserInitiatedPurchase = false;
    private TextView PriceMonthly;
    private TextView PriceLifetime;

    private ProgressBar progressMonthly, progressLifetime;

    private static final String PREF_MONTHLY_PRICE = "pref_monthly_price";
    private static final String PREF_LIFETIME_PRICE = "pref_lifetime_price";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_premium, container, false);

        cardMonthly = view.findViewById(R.id.cardMonthly);
        cardLifetime = view.findViewById(R.id.cardLifetime);
        PriceMonthly = view.findViewById(R.id.tvPriceMonthly);
        PriceLifetime = view.findViewById(R.id.tvPriceLifetime);
        tvStatusMonthly = view.findViewById(R.id.tvStatusMonthly);
        tvStatusLifetime = view.findViewById(R.id.tvStatusLifetime);
        progressMonthly = view.findViewById(R.id.progressMonthly);
        progressLifetime = view.findViewById(R.id.progressLifetime);
        handleInitialPriceUI();
        initBilling();
        setupClicks(view);
        return view;
    }

    private void handleInitialPriceUI() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

        String savedMonthly = prefs.getString(PREF_MONTHLY_PRICE, null);
        String savedLifetime = prefs.getString(PREF_LIFETIME_PRICE, null);

        if (savedMonthly != null && !savedMonthly.isEmpty()) {
            PriceMonthly.setText(savedMonthly);
            PriceMonthly.setVisibility(View.VISIBLE);
            if (progressMonthly != null) progressMonthly.setVisibility(View.GONE);
        } else {
            PriceMonthly.setVisibility(View.GONE);
            if (progressMonthly != null) progressMonthly.setVisibility(View.VISIBLE);
        }
        if (savedLifetime != null && !savedLifetime.isEmpty()) {
            PriceLifetime.setText(savedLifetime);
            PriceLifetime.setVisibility(View.VISIBLE);
            if (progressLifetime != null) progressLifetime.setVisibility(View.GONE);
        } else {
            PriceLifetime.setVisibility(View.GONE);
            if (progressLifetime != null) progressLifetime.setVisibility(View.VISIBLE);
        }
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
                Log.e(TAG, "Billing Error Code: " + errorCode + " | " + technicalMessage);
                handleUserVisibleErrors(errorCode);
            }
        });

        billingManager.startConnection();
new Handler(Looper.getMainLooper()).postDelayed(this::loadPrices, 1000);
    }

    private void loadPrices() {
        if (billingManager == null || !isAdded()) return;

        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

        billingManager.fetchProductPrices("premium_monthly", BillingClient.ProductType.SUBS, newPrice -> {
            if (isAdded() && PriceMonthly != null) {
                PriceMonthly.setText(newPrice);
                prefs.edit().putString(PREF_MONTHLY_PRICE, newPrice).apply();

                PriceMonthly.setVisibility(View.VISIBLE);
                if (progressMonthly != null) progressMonthly.setVisibility(View.GONE);
            }
        });

        billingManager.fetchProductPrices("premium_lifetime", BillingClient.ProductType.INAPP, newPrice -> {
            if (isAdded() && PriceLifetime != null) {
                PriceLifetime.setText(newPrice);
                prefs.edit().putString(PREF_LIFETIME_PRICE, newPrice).apply();

                PriceLifetime.setVisibility(View.VISIBLE);
                if (progressLifetime != null) progressLifetime.setVisibility(View.GONE);
            }
        });
    }

    private void handleUserVisibleErrors(int errorCode) {
        if (!isAdded()) return;

        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();

        String userMessage;
        switch (errorCode) {
            case BillingClient.BillingResponseCode.USER_CANCELED:
                userMessage = getString(R.string.error_billing_cancelled);
                break;
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                userMessage = getString(R.string.error_billing_already_owned);
                updateSubscriptionStatus();
                break;
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
            case BillingClient.BillingResponseCode.NETWORK_ERROR:
                userMessage = getString(R.string.error_billing_network_lost);
                break;
            default:
                userMessage = getString(R.string.error_billing_play_store_busy);
                break;
        }

        if (isUserInitiatedPurchase) {
            Toast.makeText(getContext(), userMessage, Toast.LENGTH_SHORT).show();
            isUserInitiatedPurchase = false;
        }

        if (progressMonthly != null) progressMonthly.setVisibility(View.GONE);
        if (progressLifetime != null) progressLifetime.setVisibility(View.GONE);
        PriceMonthly.setVisibility(View.VISIBLE);
        PriceLifetime.setVisibility(View.VISIBLE);

        if (PriceMonthly.getText().toString().isEmpty()) PriceMonthly.setText(getString(R.string.fallback_not_available));
        if (PriceLifetime.getText().toString().isEmpty()) PriceLifetime.setText(getString(R.string.fallback_not_available)); // 🔥 Changed
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

        if (isPremium) {
            applyPremiumUI("monthly_active");
        }

        if (billingManager != null) {
            billingManager.checkPurchaseDetails((type, purchase) -> {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> applyPremiumUI(type));
                }
            });
        }
    }

    private void applyPremiumUI(String type) {
        if (!isAdded() || tvStatusMonthly == null || tvStatusLifetime == null) return;

        // Reset default states
        tvStatusMonthly.setVisibility(View.GONE);
        tvStatusLifetime.setVisibility(View.GONE);
        cardMonthly.setAlpha(1.0f);
        cardMonthly.setEnabled(true);
        cardLifetime.setAlpha(1.0f);
        cardLifetime.setEnabled(true);

        if ("lifetime".equals(type)) {
            tvStatusLifetime.setVisibility(View.VISIBLE);
            tvStatusLifetime.setText(getString(R.string.premium_state_active));
            cardMonthly.setAlpha(0.5f);
            cardMonthly.setEnabled(false);
            cardLifetime.setEnabled(false);
        } else if ("monthly".equals(type) || "monthly_active".equals(type)) {
            tvStatusMonthly.setVisibility(View.VISIBLE);
            tvStatusMonthly.setText(getString(R.string.premium_state_active));
            cardMonthly.setEnabled(false);
        }
    }

    private void showSuccessStatus() {
        if (!isAdded()) return;
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_billing_status, null);

        TextView tvMsg = dialogView.findViewById(R.id.tvDialogMessage);
        tvMsg.setText(getString(R.string.success_elite_welcome));

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
            showProgressDialog(getString(R.string.progress_verifying_google));
            billingManager.buyProduct(productId, type);

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