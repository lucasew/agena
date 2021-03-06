package com.biglucas.agena.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import com.biglucas.agena.R;


public class Invoker {
    private final Activity activity;
    private final Uri uri;

    public Invoker(Activity activity, Uri uri) {
        this.activity = activity;
        this.uri = uri;
    }

    public Invoker(Activity activity, String uri) {
        this(activity, Uri.parse(uri.trim()));
    }

    private void runIntent(Intent intent) {
        try {
            this.activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(this.activity)
                    .setTitle(R.string.error_dont_know_how_to_handle_content_type)
                    .setPositiveButton("OK", null)
                    .setMessage(this.uri.toString())
                    .show();
            //new StacktraceDialogHandler(e).show(this.activity);
        }
    }
    private Uri getUri() {
        return Uri.parse(this.uri.toString());
    }
    private Intent getBaseIntent() {
        return new Intent(Intent.ACTION_VIEW, getUri());
    }

    public void invoke() {
        runIntent(getBaseIntent());
    }

    public void invokeNewWindow() {
        Intent intent = getBaseIntent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        }
        runIntent(intent);
    }
}
