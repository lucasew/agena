package com.biglucas.agena.utils;

import android.util.Log;

/**
 * Centralized error reporter for AGENA application.
 * Follows the global instructions to have a single, centralized error-reporting function.
 */
public class ErrorReporter {

    /**
     * Reports an error message.
     * @param tag Used to identify the source of a log message.
     * @param message The message you would like logged.
     */
    public static void reportError(String tag, String message) {
        Log.e(tag, message);
    }

    /**
     * Reports an error message and a throwable.
     * @param tag Used to identify the source of a log message.
     * @param message The message you would like logged.
     * @param tr An exception to log.
     */
    public static void reportError(String tag, String message, Throwable tr) {
        Log.e(tag, message, tr);
    }
}
