package com.biglucas.agena.utils;

import android.util.Log;

/**
 * Centralized error reporting utility.
 * <p>
 * This class ensures that all unexpected errors and exceptions are logged
 * through a single choke point. This facilitates future integration with
 * error tracking services (like Sentry) without modifying the call sites.
 */
public final class ErrorReporter {

    private ErrorReporter() {
        // Prevent instantiation
    }

    /**
     * Reports an exception.
     *
     * @param tag The log tag.
     * @param message A descriptive message.
     * @param throwable The exception to report.
     */
    public static void reportException(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
        // Future: Sentry.captureException(throwable);
    }

    /**
     * Reports an error message.
     *
     * @param tag The log tag.
     * @param message The error message.
     */
    public static void reportError(String tag, String message) {
        Log.e(tag, message);
        // Future: Sentry.captureMessage(message);
    }
}
