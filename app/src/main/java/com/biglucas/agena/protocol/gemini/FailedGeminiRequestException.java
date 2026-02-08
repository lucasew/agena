package com.biglucas.agena.protocol.gemini;

public class FailedGeminiRequestException extends Exception {
    FailedGeminiRequestException(String reason) {
        super(reason);
    }

    // Input exceptions (10-19)
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

    // Redirects (30-39) - handled internally
    public static class GeminiTooManyRedirects extends FailedGeminiRequestException {
        public GeminiTooManyRedirects() {
            super("Too many redirects (max 5)");
        }
    }

    // Temporary failures (40-49)
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

    // Permanent failures (50-59)
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

    // Client certificate errors (60-69)
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

    // General errors
    public static class GeminiResponseTooLarge extends FailedGeminiRequestException {
        public GeminiResponseTooLarge(String message) {
            super("Response too large: " + message);
        }
    }

    public static class GeminiInvalidResponse extends FailedGeminiRequestException {
        GeminiInvalidResponse() {
            super("Invalid response");
        }
    }

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
