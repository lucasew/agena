package com.biglucas.agena.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.biglucas.agena.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.PrintWriter;
import aimport java.io.StringWriter;

public class StacktraceDialogHandler {
    private static final String TAG = StacktraceDialogHandler.class.getSimpleName();
    private StacktraceDialogHandler() {
        // Private constructor to prevent instantiation
    }
    public static void show(Context context, Exception exception) {
        // Log the full stack trace for debugging purposes.
        // This is not shown to the user.
        Log.e(TAG, "An exception occurred: ", exception);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setPositiveButton("OK", null);
        builder.setTitle(R.string.error_generic);
        builder.setMessage(R.string.generic_error_message);

        // Prepare a sanitized error report for the user to copy.
        // This avoids leaking the full stack trace.
        final String errorReport = "Exception: " + exception.getClass().getName() + "\\n" +
                "Message: " + exception.getMessage() + "\\n";


        // Add copy button for a sanitized error report
        builder.setNeutralButton(R.string.copy_stacktrace, (dialog, which) -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Error Report", errorReport);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, R.string.error_report_copied, Toast.LENGTH_SHORT).show();
        });

        builder.show();
    }
}
