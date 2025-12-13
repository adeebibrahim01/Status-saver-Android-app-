package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

public class ConsentFormManager {

    private final Activity activity;
    private ConsentInformation consentInformation;
    private static ConsentFormManager instance;

    // Flag to avoid showing form multiple times
    private boolean formAlreadyShown = false;

    // Flag for testing mode
    private static final boolean IS_TESTING = true;

    private ConsentFormManager(@NonNull Activity activity) {
        this.activity = activity;
    }

    public static void init(@NonNull Activity activity) {
        if (instance == null) instance = new ConsentFormManager(activity);
    }

    public static ConsentFormManager getInstance() {
        return instance;
    }

    public void requestConsentForm(Runnable onConsentReady) {
        if (!IS_TESTING) {
            // Skip form if not testing
            onConsentReady.run();
            return;
        }

        if (formAlreadyShown) {
            // Already shown → skip
            onConsentReady.run();
            return;
        }

        consentInformation = UserMessagingPlatform.getConsentInformation(activity);

        ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId("EFB3EC451980C80F0A145C61936CE2ED")
                .build();

        ConsentRequestParameters params = new ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .setConsentDebugSettings(debugSettings)
                .build();

        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () -> {
                    if (consentInformation.isConsentFormAvailable()) {
                        loadAndShowForm(onConsentReady);
                    } else {
                        onConsentReady.run();
                    }
                },
                formError -> {
                    Log.e("ConsentForm", "Request error: " + formError.getMessage());
                    onConsentReady.run();
                }
        );
    }

    private void loadAndShowForm(Runnable onConsentReady) {
        UserMessagingPlatform.loadConsentForm(
                activity,
                consentForm -> {
                    if (!formAlreadyShown) {
                        consentForm.show(activity, formError -> {
                            if (formError != null) {
                                Log.e("ConsentForm", "Form error: " + formError.getMessage());
                            }
                            formAlreadyShown = true; // mark as shown
                            onConsentReady.run();
                        });
                    } else {
                        onConsentReady.run();
                    }
                },
                formError -> {
                    Log.e("ConsentForm", "Load form error: " + formError.getMessage());
                    onConsentReady.run();
                }
        );
    }

    public boolean canRequestAds() {
        if (consentInformation == null) return true;
        return consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.OBTAINED
                || consentInformation.canRequestAds();
    }

    public static Bundle getNonPersonalizedBundle() {
        Bundle extras = new Bundle();
        extras.putString("npa", "1");
        return extras;
    }
}
