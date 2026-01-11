package com.biglucas.agena.utils;

import android.content.Context;
import android.util.Log;

import com.biglucas.agena.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class StacktraceDialogHandler {
    private static final String TAG = "StacktraceDialogHandler";
    private StacktraceDialogHandler() {
        // Private constructor to prevent instantiation
    }
    public static void show(Context context, Exception exception) {
        Log.e(TAG, "Unhandled exception", exception);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setPositiveButton("OK", null);
        builder.setTitle(R.string.error_unhandled_title);
        builder.setMessage(R.string.error_generic);
        builder.show();
    }
}
