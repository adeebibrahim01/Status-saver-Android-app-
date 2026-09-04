package com.mariaxcodexpert.whatsdownloadplus.Ads;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

public class ConsentFormManager {

    private static final String TAG = "ConsentFormManager";
    private final Activity activity;
    private final ConsentInformation consentInformation;
    private static ConsentFormManager instance;

    private ConsentFormManager(@NonNull Activity activity) {
        this.activity = activity;
        this.consentInformation = UserMessagingPlatform.getConsentInformation(activity);
    }

    public static void init(@NonNull Activity activity) {
        if (instance == null) instance = new ConsentFormManager(activity);
    }

    public static ConsentFormManager getInstance() {
        return instance;
    }

    public void requestConsentForm(Runnable onConsentReady) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        ConsentRequestParameters params = new ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build();

        consentInformation.requestConsentInfoUpdate(activity, params, () -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;

            if (!consentInformation.isConsentFormAvailable()) {
                Log.d(TAG, "Not required in this region. Proceeding with consent.");
                onConsentReady.run();
                return;
            }

            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                if (formError != null) {
                    Log.e(TAG, "Form Error: " + formError.getMessage());
                }
                onConsentReady.run();
            });

        }, requestError -> {
            Log.e(TAG, "Consent Error: " + requestError.getMessage());
            onConsentReady.run();
        });
    }

    public boolean canRequestAds() {
        if (consentInformation.getConsentStatus() == 3) {
            return false;
        }
        return true;
    }

    public static Bundle getNonPersonalizedBundle() {
        Bundle extras = new Bundle();
        extras.putString("npa", "1");
        return extras;
    }
}