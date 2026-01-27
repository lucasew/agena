package com.biglucas.agena.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.biglucas.agena.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Utility to display exception stack traces in a user-facing dialog.
 * <p>
 * This is primarily intended for debug builds or critical error boundaries where
 * immediate feedback to the developer/tester is required.
 * <p>
 * <b>Security Note:</b> Be cautious using this in production releases as it exposes
 * internal application state and stack traces.
 */
public class StacktraceDialogHandler {
    private StacktraceDialogHandler() {
        // Private constructor to prevent instantiation
    }

    /**
     * Shows a dialog containing the stack trace of the provided exception.
     * <p>
     * Includes a "Copy" button to facilitate error reporting by copying the
     * full stack trace to the clipboard.
     *
     * @param context The context to host the dialog (usually an Activity).
     * @param exception The exception to display.
     */
    public static void show(Context context, Exception exception) {
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
