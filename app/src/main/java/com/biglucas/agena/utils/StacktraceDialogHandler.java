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

public class StacktraceDialogHandler {
    private final Exception exception;

    public StacktraceDialogHandler(Exception e) {
        this.exception = e;
    }

    public void show(View view) {
        show(view.getContext());
    }
    public void show(Context context) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setPositiveButton("OK", null);
        builder.setTitle(exception.getClass().getName());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        this.exception.printStackTrace(pw);
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
