package com.biglucas.agena;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.color.DynamicColors;

/**
 * Application class for Agena Gemini Browser
 * Applies Material You dynamic colors globally before any Activity is created
 */
public class AgenaApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Apply Material You dynamic colors globally using callback approach
        // This ensures colors are applied immediately when each Activity is created
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                // Apply dynamic colors to this specific activity
                DynamicColors.applyToActivityIfAvailable(activity);
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {}

            @Override
            public void onActivityResumed(@NonNull Activity activity) {}

            @Override
            public void onActivityPaused(@NonNull Activity activity) {}

            @Override
            public void onActivityStopped(@NonNull Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }
}
