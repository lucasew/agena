package com.biglucas.agena.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.biglucas.agena.BuildConfig;
import com.biglucas.agena.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.PrintWriter;
import java.io.StringWriter;

public class StacktraceDialogHandler {
    private static final String TAG = "StacktraceDialogHandler";

    private StacktraceDialogHandler() {
        // Private constructor to prevent instantiation
    }
    public static void show(Context context, Exception exception) {
        if (!BuildConfig.DEBUG) {
            Log.e(TAG, "Stacktrace suppressed in release build", exception);
            Toast.makeText(context, R.string.error_generic, Toast.LENGTH_LONG).show();
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setPositiveButton("OK", null);
        builder.setTitle(exception.getClass().getName());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String stackTrace = sw.toString();
        builder.setMessage(stackTrace);

        // Add copy button
        builder.setNeutralButton(R.string.copy_stacktrace, (dialog, which) -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Stack Trace", stackTrace);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, R.string.stacktrace_copied, Toast.LENGTH_SHORT).show();
        });

        builder.show();
    }
}
