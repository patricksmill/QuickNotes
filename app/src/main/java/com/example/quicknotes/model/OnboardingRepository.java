package com.example.quicknotes.model;

import android.app.Activity;
import android.content.Context;

public class OnboardingRepository {

    public OnboardingRepository(Context context) {
        context.getApplicationContext();
    }

    public boolean shouldShowOnboarding(Activity activity) {
        // Create manager on-demand with Activity context
        OnboardingManager om = new OnboardingManager(activity);
        return om.shouldShowOnboarding();
    }

    public void setOnboardingCompleted(Activity activity) {
        // Create manager on-demand with Activity context
        OnboardingManager om = new OnboardingManager(activity);
        om.setOnboardingCompleted();
    }

}
