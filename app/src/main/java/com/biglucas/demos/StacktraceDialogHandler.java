package com.biglucas.demos;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import java.io.PrintWriter;
import java.io.StringWriter;

public class StacktraceDialogHandler {
    private final Exception exception;

    public StacktraceDialogHandler(Exception e) {
        this.exception = e;
    }

    public void show(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setPositiveButton("OK", null);
        builder.setTitle(exception.getClass().getName());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        this.exception.printStackTrace(pw);
        builder.setMessage(sw.toString());
        AlertDialog dialog = builder.show();
        TextView messageView = (TextView) dialog.findViewById(android.R.id.message);
        messageView.setTypeface(Typeface.MONOSPACE);
        messageView.setGravity(Gravity.CENTER);
    }
}