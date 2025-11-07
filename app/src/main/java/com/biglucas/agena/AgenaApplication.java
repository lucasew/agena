package com.biglucas.agena;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.material.color.DynamicColors;

/**
 * Application class for Agena Gemini Browser
 * Applies Material You dynamic colors globally before any Activity is created
 */
public class AgenaApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Apply Material You dynamic colors using the correct callback timing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (API 29+): Use onActivityPreCreated which is called BEFORE onCreate()
            registerActivityLifecycleCallbacks(new DynamicColorsApplier());
        } else {
            // Fallback: Use the standard method for older Android versions
            // This may show a brief fallback color flash on first launch
            DynamicColors.applyToActivitiesIfAvailable(this);
        }
    }

    private static class DynamicColorsApplier implements ActivityLifecycleCallbacks {
        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        public void onActivityPreCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            // Apply dynamic colors BEFORE Activity.onCreate() is called
            // This ensures colors are applied before setContentView()
            DynamicColors.applyToActivityIfAvailable(activity);
        }

        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

        @Override
        public void onActivityStarted(@NonNull Activity activity) {}

        @Override
        public void onActivityResumed(@NonNull Activity activity) {}

        @Override
        public void onActivityPaused(@NonNull Activity activity) {}

        @Override
        public void onActivityStopped(@NonNull Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {

        }

    }
}
