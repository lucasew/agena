package com.biglucas.agena.protocol.gemini;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling Gemini URIs.
 * <p>
 * This class provides methods for resolving relative URIs and sanitizing malformed URIs commonly found in Gemini pages.
 * It uses standard Java {@link URI} to be platform-independent and unit-testable.
 */
public class GeminiUriHelper {

    /**
     * Resolves a target URI against a base URI, handling malformed target URIs gracefully.
     * <p>
     * This method attempts to resolve the target URI using standard {@link URI#resolve(String)}.
     * If that fails due to {@link IllegalArgumentException} (common with malformed characters in Gemini links),
     * it attempts to sanitize the target URI by removing invalid characters and retries resolution.
     *
     * @param baseUriString The base URI string (e.g., the current page URL).
     * @param target The target URI string (relative or absolute).
     * @return The resolved absolute URI string.
     */
    public static String resolve(String baseUriString, String target) {
        URI base;
        try {
            base = URI.create(baseUriString.trim());
        } catch (IllegalArgumentException e) {
             // If base URI is invalid, we can't resolve against it. Return target or throw?
             // Original code assumed base was valid (it came from android.net.Uri).
             // Let's assume base is valid or return target if not relative.
             return target;
        }

        String path = base.getPath();
        // Preserve original logic: if path doesn't end in / or .gmi, append /
        // This forces directory semantics for paths that don't look like files
        if (path != null && !path.endsWith("/") && !path.endsWith(".gmi")) {
             baseUriString = String.format("%s/", baseUriString);
        }

        try {
            return URI.create(baseUriString.trim()).resolve(target.trim()).toString();
        } catch (IllegalArgumentException e) {
            // Fallback for malformed URIs
            // Some sites have invalid characters in links. We strip them and retry.
            final String regex = "[^a-zA-Z0-9:/.-]*";
            final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            final Matcher matcher = pattern.matcher(target);
            final String sanitizedTarget = matcher.replaceAll("");

            // Retry resolution with sanitized target
            try {
                return URI.create(baseUriString.trim()).resolve(sanitizedTarget).toString();
            } catch (IllegalArgumentException ex) {
                // If still fails, return original target or empty string to avoid crash
                return target;
            }
        }
    }
}
