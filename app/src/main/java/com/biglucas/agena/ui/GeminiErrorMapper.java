package com.biglucas.agena.ui;

import android.content.Context;
import com.biglucas.agena.R;
import com.biglucas.agena.protocol.gemini.FailedGeminiRequestException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * Maps Gemini and network exceptions to user-friendly error messages.
 */
public final class GeminiErrorMapper {

    private GeminiErrorMapper() {
        // Private constructor to prevent instantiation
    }

    /**
     * Returns a user-friendly error message for the given exception.
     * Returns null if the exception is not a known/specific error type.
     *
     * @param context The context used to retrieve string resources.
     * @param e The exception to map.
     * @return The error message, or null if unhandled.
     */
    public static String getErrorMessage(Context context, Exception e) {
        if (e instanceof UnknownHostException) {
            return context.getString(R.string.error_unable_to_resolve_host);
        } else if (e instanceof SocketTimeoutException) {
            return context.getString(R.string.error_connection_timeout);

        // Temporary failures (40-49)
        } else if (e instanceof FailedGeminiRequestException.GeminiSlowDown) {
            return context.getString(R.string.error_slow_down) + ": " + e.getMessage();
        } else if (e instanceof FailedGeminiRequestException.GeminiServerUnavailable) {
            return context.getString(R.string.error_server_unavailable);
        } else if (e instanceof FailedGeminiRequestException.GeminiCGIError) {
            return Objects.requireNonNull(e.getMessage()).replaceFirst("^CGI error: CGI [Ee]rror: ", "CGI error: ");
        } else if (e instanceof FailedGeminiRequestException.GeminiProxyError) {
            return context.getString(R.string.error_proxy_error);
        } else if (e instanceof FailedGeminiRequestException.GeminiTemporaryFailure) {
            return context.getString(R.string.error_temporary_failure);

        // Permanent failures (50-59)
        } else if (e instanceof FailedGeminiRequestException.GeminiNotFound) {
            return context.getString(R.string.error_gemini_not_found);
        } else if (e instanceof FailedGeminiRequestException.GeminiGone) {
            return context.getString(R.string.error_gone);
        } else if (e instanceof FailedGeminiRequestException.GeminiProxyRequestRefused) {
            return context.getString(R.string.error_proxy_request_refused);
        } else if (e instanceof FailedGeminiRequestException.GeminiBadRequest) {
            return context.getString(R.string.error_bad_request);
        } else if (e instanceof FailedGeminiRequestException.GeminiPermanentFailure) {
            return context.getString(R.string.error_permanent_failure);

        // Client certificate errors (60-69)
        } else if (e instanceof FailedGeminiRequestException.GeminiClientCertificateRequired) {
            return context.getString(R.string.error_client_certificate_required);
        } else if (e instanceof FailedGeminiRequestException.GeminiCertificateNotAuthorized) {
            return context.getString(R.string.error_certificate_not_authorized);
        } else if (e instanceof FailedGeminiRequestException.GeminiCertificateNotValid) {
            return context.getString(R.string.error_certificate_not_valid);

        // Redirect errors
        } else if (e instanceof FailedGeminiRequestException.GeminiTooManyRedirects) {
            return context.getString(R.string.error_too_many_redirects);

        // URI validation errors
        } else if (e instanceof FailedGeminiRequestException.GeminiInvalidUri) {
            return context.getString(R.string.error_invalid_uri) + ": " + e.getMessage();

        // Other Gemini errors
        } else if (e instanceof FailedGeminiRequestException.GeminiInvalidResponse) {
            return context.getString(R.string.error_gemini_invalid_response);
        } else if (e instanceof FailedGeminiRequestException.GeminiUnimplementedCase) {
            return context.getString(R.string.error_gemini_unimplemented);
        }

        return null;
    }
}
