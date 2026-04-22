package com.biglucas.agena.protocol.gemini;

/**
 * Base exception for all protocol-level errors encountered during a Gemini request.
 * <p>
 * This class hierarchy maps directly to the Gemini protocol status codes (10-69).
 * Subclasses encapsulate the specific error condition and any associated meta-information
 * provided by the server.
 */
public class FailedGeminiRequestException extends Exception {
    FailedGeminiRequestException(String reason) {
        super(reason);
    }

    /**
     * Represents status codes in the 1x range (Input Required).
     * Indicates the server requires user input to proceed.
     */
    public static class GeminiInputRequired extends FailedGeminiRequestException {
        private final String prompt;
        private final boolean sensitive;

        public GeminiInputRequired(String prompt, boolean sensitive) {
            super(sensitive ? "Sensitive input required" : "Input required");
            this.prompt = prompt;
            this.sensitive = sensitive;
        }

        public String getPrompt() {
            return prompt;
        }

        public boolean isSensitive() {
            return sensitive;
        }
    }

    // Success (20-29) - not exceptions

    /**
     * Represents a local enforcement of the maximum redirect limit.
     * While 3x status codes are redirects, the protocol client limits consecutive
     * redirects to prevent infinite loops (usually a max of 5).
     */
    public static class GeminiTooManyRedirects extends FailedGeminiRequestException {
        public GeminiTooManyRedirects() {
            super("Too many redirects (max 5)");
        }
    }

    /**
     * Represents status codes in the 4x range (Temporary Failure).
     * Indicates a failure that might be resolved by retrying the request later.
     */
    public static class GeminiTemporaryFailure extends FailedGeminiRequestException {
        public GeminiTemporaryFailure(String message) {
            super("Temporary failure: " + message);
        }
    }

    public static class GeminiServerUnavailable extends FailedGeminiRequestException {
        public GeminiServerUnavailable(String message) {
            super("Server unavailable: " + message);
        }
    }

    public static class GeminiCGIError extends FailedGeminiRequestException {
        public GeminiCGIError(String message) {
            super("CGI error: " + message);
        }
    }

    public static class GeminiProxyError extends FailedGeminiRequestException {
        public GeminiProxyError(String message) {
            super("Proxy error: " + message);
        }
    }

    public static class GeminiSlowDown extends FailedGeminiRequestException {
        private final String message;

        public GeminiSlowDown(String message) {
            super("Slow down: " + message);
            this.message = message;
        }

        public String getWaitMessage() {
            return message;
        }
    }

    /**
     * Represents status codes in the 5x range (Permanent Failure).
     * Indicates a persistent error; retrying the exact same request will likely fail again.
     */
    public static class GeminiPermanentFailure extends FailedGeminiRequestException {
        public GeminiPermanentFailure(String message) {
            super("Permanent failure: " + message);
        }
    }

    public static class GeminiNotFound extends FailedGeminiRequestException {
        public GeminiNotFound() {
            super("Content not found");
        }
    }

    public static class GeminiGone extends FailedGeminiRequestException {
        public GeminiGone() {
            super("Gone");
        }
    }

    public static class GeminiProxyRequestRefused extends FailedGeminiRequestException {
        public GeminiProxyRequestRefused(String message) {
            super("Proxy request refused: " + message);
        }
    }

    public static class GeminiBadRequest extends FailedGeminiRequestException {
        public GeminiBadRequest(String message) {
            super("Bad request: " + message);
        }
    }

    /**
     * Represents status codes in the 6x range (Client Certificate Required).
     * Indicates the requested resource requires mutual TLS authentication.
     */
    public static class GeminiClientCertificateRequired extends FailedGeminiRequestException {
        public GeminiClientCertificateRequired(String message) {
            super("Client certificate required: " + message);
        }
    }

    public static class GeminiCertificateNotAuthorized extends FailedGeminiRequestException {
        public GeminiCertificateNotAuthorized(String message) {
            super("Certificate not authorized: " + message);
        }
    }

    public static class GeminiCertificateNotValid extends FailedGeminiRequestException {
        public GeminiCertificateNotValid(String message) {
            super("Certificate not valid: " + message);
        }
    }

    /**
     * Thrown when the server's response header is completely malformed or unparseable.
     */
    public static class GeminiInvalidResponse extends FailedGeminiRequestException {
        GeminiInvalidResponse() {
            super("Invalid response");
        }
    }

    /**
     * Thrown when the provided URI does not conform to the Gemini specification
     * (e.g., exceeds max length, contains userinfo, or invalid scheme).
     */

    public static class GeminiInvalidUri extends FailedGeminiRequestException {
        public GeminiInvalidUri(String message) {
            super("Invalid URI: " + message);
        }
    }

    public static class GeminiUnimplementedCase extends FailedGeminiRequestException {
        GeminiUnimplementedCase() {
            super("Unimplemented case");
        }
    }
}
