package com.biglucas.agena.utils;

import android.util.Log;

/**
 * Centralized utility for reporting unexpected exceptions.
 * All application code must use this instead of direct Log.e or silent catches.
 */
public class ErrorReporter {
    private static final String TAG = "ErrorReporter";

    private ErrorReporter() {
        // Utility class
    }

    /**
     * Reports an error message and its associated exception.
     * In a production app, this should be wired to a crash reporting tool (e.g. Sentry or Firebase).
     *
     * @param tag     The logging tag.
     * @param message A descriptive error message.
     * @param e       The exception to report.
     */
    public static void reportError(String tag, String message, Throwable e) {
        if (e != null) {
            Log.e(tag != null ? tag : TAG, message, e);
        } else {
            Log.e(tag != null ? tag : TAG, message);
        }
    }

    /**
     * Reports an error message without an exception.
     *
     * @param tag     The logging tag.
     * @param message A descriptive error message.
     */
    public static void reportError(String tag, String message) {
        Log.e(tag != null ? tag : TAG, message);
    }
}
