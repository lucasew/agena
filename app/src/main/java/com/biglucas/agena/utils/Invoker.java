package com.biglucas.agena.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import com.biglucas.agena.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;


public final class Invoker {
    private Invoker() {}

    private static void runIntent(Activity activity, Uri uri, Intent intent) {
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            new MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.error_dont_know_how_to_handle_content_type)
                    .setPositiveButton("OK", null)
                    .setMessage(uri.toString())
                    .show();
        }
    }

    private static boolean isSafeGeminiUri(Uri uri) {
        if (uri == null) {
            return false;
        }
        // Prevent redirection attacks by ensuring the scheme is "gemini"
        // and the authority is not empty, which could lead to malformed URIs.
        return "gemini".equals(uri.getScheme()) && uri.getAuthority() != null && !uri.getAuthority().isEmpty();
    }

    private static boolean isSafeUri(Uri uri) {
        if (uri == null || uri.getScheme() == null) return false;
        String scheme = uri.getScheme().toLowerCase();
        return "gemini".equals(scheme) ||
               "http".equals(scheme) ||
               "https".equals(scheme) ||
               "mailto".equals(scheme);
    }

    private static Intent getBaseIntent(Uri uri) {
        return new Intent(Intent.ACTION_VIEW, uri);
    }

    public static void invoke(Activity activity, Uri uri) {
        if (!isSafeUri(uri)) {
            new MaterialAlertDialogBuilder(activity)
                    .setTitle(activity.getString(R.string.error_unsupported_scheme, uri.getScheme()))
                    .setMessage(uri.toString())
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        runIntent(activity, uri, getBaseIntent(uri));
    }

    public static void invoke(Activity activity, String uri) {
        invoke(activity, Uri.parse(uri.trim()));
    }

    public static void invokeNewWindow(Activity activity, Uri uri) {
        if (!isSafeGeminiUri(uri)) {
            new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.error_invalid_uri)
                .setMessage(uri.toString())
                .setPositiveButton("OK", null)
                .show();
            return;
        }
        Intent intent = getBaseIntent(uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        runIntent(activity, uri, intent);
    }

    public static void invokeNewWindow(Activity activity, String uri) {
        invokeNewWindow(activity, Uri.parse(uri.trim()));
    }
}
