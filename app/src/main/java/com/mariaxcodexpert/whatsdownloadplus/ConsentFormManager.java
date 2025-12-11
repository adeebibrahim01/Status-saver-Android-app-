package com.mariaxcodexpert.whatsdownloadplus;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.google.android.ump.ConsentForm;

public class ConsentFormManager {

    private final Activity activity;
    private ConsentInformation consentInformation;
    private static ConsentFormManager instance;

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
        consentInformation = UserMessagingPlatform.getConsentInformation(activity);

        ConsentRequestParameters params = new ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build();

        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () -> {
                    if (consentInformation.isConsentFormAvailable()) {
                        UserMessagingPlatform.loadConsentForm(activity,
                                consentForm -> consentForm.show(activity, formError -> onConsentReady.run()),
                                formError -> {
                                    Log.e("ConsentForm", "Load error: " + formError.getMessage());
                                    onConsentReady.run();
                                });
                    } else {
                        onConsentReady.run(); // Non-EU → directly load ads
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
                consentForm -> consentForm.show(activity, formError -> onConsentReady.run()),
                formError -> onConsentReady.run()
        );
    }

    public boolean canRequestAds() {
        // If consentInformation is null, assume non-EU user → ads allowed
        if (consentInformation == null) return true;

        // Return true if consent status allows ads (Non-EU: always true)
        return consentInformation.canRequestAds();
    }

}
