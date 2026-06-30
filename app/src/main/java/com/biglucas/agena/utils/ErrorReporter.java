package com.biglucas.agena.utils;

import android.util.Log;

public class ErrorReporter {
    public static void reportError(String tag, String message) {
        // Centralized error reporting
        Log.e(tag, message);
    }

    public static void reportError(String tag, String message, Throwable t) {
        // Centralized error reporting
        Log.e(tag, message, t);
    }
}
