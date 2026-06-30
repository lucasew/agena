package com.biglucas.agena.utils;

import android.util.Log;

/**
 * Centralized error reporting utility.
 * All unexpected errors must funnel through this class.
 */
public class ErrorReporter {
    /**
     * Reports an error. If Sentry is added later, it will be initialized here.
     *
     * @param tag The log tag.
     * @param message The error message.
     * @param e The exception that occurred.
     */
    public static void reportError(String tag, String message, Throwable e) {
        Log.e(tag, message, e);
        // TODO: Send to Sentry or another crash reporting service here
    }

    /**
     * Reports an error without an exception object.
     *
     * @param tag The log tag.
     * @param message The error message.
     */
    public static void reportError(String tag, String message) {
        Log.e(tag, message);
        // TODO: Send to Sentry or another crash reporting service here
    }
}
