package com.biglucas.agena;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

/**
 * Application class for Agena Gemini Browser
 * Applies Material You dynamic colors globally before any Activity is created
 */
public class AgenaApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Apply Material You dynamic colors to all activities
        // This must be done in Application.onCreate() to ensure colors are applied
        // before any Activity is created, avoiding the fallback color "data race"
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
