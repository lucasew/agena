package com.biglucas.agena.utils;

import android.util.Log;

/**
 * Centralized error reporting utility for the application.
 * All unexpected or unhandled exceptions must be routed through this class
 * rather than logging directly to console or third-party services.
 */
public final class ErrorReporter {
    private static final String TAG = "ErrorReporter";

    private ErrorReporter() {
        // Prevent instantiation
    }

    /**
     * Reports an exception that occurred during application execution.
     *
     * @param tag     A tag identifying the source of the error (typically the class name).
     * @param message A descriptive message about the error context.
     * @param e       The exception that occurred.
     */
    public static void reportException(String tag, String message, Throwable e) {
        // In the future, this is where we would integrate Sentry or Firebase Crashlytics
        Log.e(tag, message, e);
    }

    /**
     * Reports a non-fatal error condition.
     *
     * @param tag     A tag identifying the source of the error (typically the class name).
     * @param message A descriptive message about the error condition.
     */
    public static void reportError(String tag, String message) {
        // In the future, this is where we would integrate Sentry or Firebase Crashlytics
        Log.e(tag, message);
    }
}
