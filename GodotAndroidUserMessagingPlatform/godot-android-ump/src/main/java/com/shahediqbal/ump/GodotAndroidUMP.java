package com.shahediqbal.ump;

import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.UsedByGodot;

public class GodotAndroidUMP extends GodotPlugin {
    private static final String TAG = "godot";
    private static final String PLUGIN_NAME = "GodotAndroidUserMessagingPlatform";

    private ConsentRequestParameters params;
    private ConsentInformation consentInformation;
    private ConsentForm consentForm;

    public GodotAndroidUMP(Godot godot) {
        super(godot);
    }

    @NonNull
    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @UsedByGodot
    public void initialize() {
// Set tag for underage of consent. false means users are not underage.
        params = new ConsentRequestParameters
                .Builder()
                .setTagForUnderAgeOfConsent(false)
                .build();
    }

    @UsedByGodot
    public void initializeForDebug(String testDeviceID) {
        ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(this.getActivity())
                .setDebugGeography(ConsentDebugSettings
                        .DebugGeography
                        .DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId(testDeviceID)
                .build();

        params = new ConsentRequestParameters
                .Builder()
                .setConsentDebugSettings(debugSettings)
                .build();
    }

    @UsedByGodot
    public void showConsentForm() {
        if (params == null) {
            Log.e(TAG, PLUGIN_NAME + ": " + "You must call initialize()/initializeForDebug(..) first");
            return;
        }

        consentInformation = UserMessagingPlatform.getConsentInformation(this.getActivity());
        consentInformation.requestConsentInfoUpdate(
                this.getActivity(),
                params,
                new ConsentInformation.OnConsentInfoUpdateSuccessListener() {
                    @Override
                    public void onConsentInfoUpdateSuccess() {
                        // The consent information state was updated.
                        // You are now ready to check if a form is available.
                        if (consentInformation.isConsentFormAvailable()) {
                            loadForm();
                        }
                    }
                },
                new ConsentInformation.OnConsentInfoUpdateFailureListener() {
                    @Override
                    public void onConsentInfoUpdateFailure(FormError formError) {
                        // Handle the error.
                        Log.e(TAG, PLUGIN_NAME + ": " + "onConsentInfoUpdateFailure()");
                        if (formError != null) {
                            Log.e(TAG, PLUGIN_NAME + ": " + "Error: " + formError.getMessage());
                        }
                    }
                });
    }

    @UsedByGodot
    public void ResetConsentState() {
        if (consentInformation == null)
            return;

        consentInformation.reset();
    }

    private void loadForm() {
        UserMessagingPlatform.loadConsentForm(
                this.getActivity(),
                new UserMessagingPlatform.OnConsentFormLoadSuccessListener() {
                    @Override
                    public void onConsentFormLoadSuccess(ConsentForm consentForm) {
                        GodotAndroidUMP.this.consentForm = consentForm;
                        if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {
                            consentForm.show(
                                    GodotAndroidUMP.this.getActivity(),
                                    new ConsentForm.OnConsentFormDismissedListener() {
                                        @Override
                                        public void onConsentFormDismissed(@Nullable FormError formError) {
                                            // Handle dismissal by reloading form.
                                            loadForm();
                                        }
                                    });

                        }
                    }
                },
                new UserMessagingPlatform.OnConsentFormLoadFailureListener() {
                    @Override
                    public void onConsentFormLoadFailure(FormError formError) {
                        // Handle the error
                        Log.e(TAG, PLUGIN_NAME + ": " + "onConsentFormLoadFailure()");
                        if (formError != null) {
                            Log.e(TAG, PLUGIN_NAME + ": " + "Error: " + formError.getMessage());
                        }
                    }
                }
        );
    }
}
