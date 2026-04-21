package com.biglucas.agena.utils;

import android.util.Log;

/**
 * Centralized error reporting utility.
 * <p>
 * All code paths that handle unexpected errors MUST funnel through this class.
 * This ensures that errors are not swallowed silently and provides a single integration
 * point for future crash reporting/telemetry systems (like Sentry, Crashlytics, etc.).
 */
public class ErrorReporter {

    private ErrorReporter() {
        // Utility class, do not instantiate
    }

    /**
     * Reports an exception with an optional message.
     *
     * @param tag     The log tag to use.
     * @param message A descriptive message about the error context. Can be null.
     * @param e       The exception that occurred.
     */
    public static void reportException(String tag, String message, Throwable e) {
        if (message != null) {
            Log.e(tag, message, e);
        } else {
            Log.e(tag, "An exception occurred", e);
        }
        // TODO: In the future, send this to Sentry/Crashlytics or other telemetry backends here
    }

    /**
     * Reports an error message without an exception.
     *
     * @param tag     The log tag to use.
     * @param message A descriptive message about the error.
     */
    public static void reportError(String tag, String message) {
        Log.e(tag, message);
        // TODO: In the future, send this to Sentry/Crashlytics or other telemetry backends here
    }
}
