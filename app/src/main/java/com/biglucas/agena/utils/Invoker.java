package com.biglucas.agena.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import com.biglucas.agena.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Utility class for handling navigation and Intent launching within the application.
 * <p>
 * Centralizes the logic for opening URIs, ensuring consistent error handling (e.g., ActivityNotFoundException)
 * and security checks for new windows.
 */
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

    /**
     * Validates that a URI is safe to open as a Gemini link.
     * <p>
     * Enforces the "gemini" scheme and requires a non-empty authority to prevent
     * redirection attacks or malformed URIs that could exploit the parser.
     */
    private static boolean isSafeGeminiUri(Uri uri) {
        if (uri == null) {
            return false;
        }
        // Prevent redirection attacks by ensuring the scheme is "gemini"
        // and the authority is not empty, which could lead to malformed URIs.
        return "gemini".equals(uri.getScheme()) && uri.getAuthority() != null && !uri.getAuthority().isEmpty();
    }

    private static Intent getBaseIntent(Uri uri) {
        return new Intent(Intent.ACTION_VIEW, uri);
    }

    public static void invoke(Activity activity, Uri uri) {
        runIntent(activity, uri, getBaseIntent(uri));
    }

    public static void invoke(Activity activity, String uri) {
        invoke(activity, Uri.parse(uri.trim()));
    }

    /**
     * Opens a Gemini URI in a new window (Activity task).
     * <p>
     * <b>Security:</b> Enforces strict security checks via {@link #isSafeGeminiUri(Uri)} before launching.
     * If the URI is unsafe, an error dialog is shown instead.
     *
     * @param activity The current activity context.
     * @param uri The Gemini URI to open.
     */
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
