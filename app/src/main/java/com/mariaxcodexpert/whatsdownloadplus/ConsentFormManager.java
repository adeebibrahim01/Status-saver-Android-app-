package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

/**
 * 100% Production-Ready Consent Manager
 * Optimized for GDPR & AdMob Policy Compliance
 */
public class ConsentFormManager {

    private static final String TAG = "ConsentFormManager";
    private final Activity activity;
    private final ConsentInformation consentInformation;
    private static ConsentFormManager instance;

    private boolean formAlreadyShown = false;

    // 🔥 Release se pehle isay FALSE karein
    private static final boolean IS_TESTING = false;

    private ConsentFormManager(@NonNull Activity activity) {
        this.activity = activity;
        this.consentInformation = UserMessagingPlatform.getConsentInformation(activity);

        // Testing ke liye Reset: Taake har baar app run karne par form dikhe
        if (IS_TESTING) {
            consentInformation.reset();
        }
    }

    public static void init(@NonNull Activity activity) {
        if (instance == null) instance = new ConsentFormManager(activity);
    }

    public static ConsentFormManager getInstance() {
        return instance;
    }

    public void requestConsentForm(Runnable onConsentReady) {
        // Safety Check: Agar activity khatam ho chuki ho
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        ConsentRequestParameters.Builder paramsBuilder = new ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false);

        if (IS_TESTING) {
            ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    // Naya Hashed ID check karein Logcat mein agar form na aaye
                    .addTestDeviceHashedId("EFB3EC451980C80F0A145C61936CE2ED")
                    .build();
            paramsBuilder.setConsentDebugSettings(debugSettings);
        }

        ConsentRequestParameters params = paramsBuilder.build();

        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () -> {
                    // Check activity state again before UI operation
                    if (activity.isFinishing() || activity.isDestroyed()) return;

                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                            activity,
                            formError -> {
                                if (formError != null) {
                                    Log.e(TAG, "Form Error: " + formError.getMessage());
                                }
                                formAlreadyShown = true;
                                onConsentReady.run();
                            }
                    );
                },
                requestError -> {
                    Log.e(TAG, "Consent Update Error: " + requestError.getMessage());
                    onConsentReady.run(); // Fail-safe
                }
        );
    }

    /**
     * AdManager mein check karein ads request karne se pehle
     */
    public boolean canRequestAds() {
        return consentInformation != null && consentInformation.canRequestAds();
    }

    /**
     * Agar user ne personalize ads mana kiye hon toh ye bundle use karein
     */
    public static Bundle getNonPersonalizedBundle() {
        Bundle extras = new Bundle();
        extras.putString("npa", "1");
        return extras;
    }
}