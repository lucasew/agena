package com.biglucas.agena.protocol.gemini;

/**
 * Defines constants and specifications for the Gemini protocol.
 * <p>
 * This class serves as the single source of truth for protocol-specific values,
 * such as status codes, default ports, and limits, replacing magic numbers
 * throughout the codebase.
 */
public final class GeminiSpec {
    private GeminiSpec() {} // Prevent instantiation

    // Network defaults
    public static final int DEFAULT_PORT = 1965;
    public static final int DEFAULT_TIMEOUT_MS = 5000;
    public static final int MAX_REDIRECTS = 5;
    public static final int MAX_URI_LENGTH_BYTES = 1024;

    // Security Limits
    public static final int MAX_RESPONSE_BODY_LENGTH_CHARS = 5 * 1024 * 1024; // 5M characters (~10MB UTF-16)
    public static final int MAX_LINE_LENGTH_BYTES = 4096; // 4KB

    // Status Code Categories (Ranges)
    public static final int CATEGORY_INPUT = 10;
    public static final int CATEGORY_SUCCESS = 20;
    public static final int CATEGORY_REDIRECT = 30;
    public static final int CATEGORY_TEMP_FAILURE = 40;
    public static final int CATEGORY_PERM_FAILURE = 50;
    public static final int CATEGORY_CLIENT_CERT = 60;
    public static final int CATEGORY_RESERVED = 70; // Upper bound for certs

    // Specific Status Codes
    public static final int STATUS_INPUT = 10;
    public static final int STATUS_SENSITIVE_INPUT = 11;

    public static final int STATUS_SUCCESS = 20;

    public static final int STATUS_REDIRECT = 30;

    public static final int STATUS_TEMP_FAILURE = 40;
    public static final int STATUS_SERVER_UNAVAILABLE = 41;
    public static final int STATUS_CGI_ERROR = 42;
    public static final int STATUS_PROXY_ERROR = 43;
    public static final int STATUS_SLOW_DOWN = 44;

    public static final int STATUS_PERM_FAILURE = 50;
    public static final int STATUS_NOT_FOUND = 51;
    public static final int STATUS_GONE = 52;
    public static final int STATUS_PROXY_REQUEST_REFUSED = 53;
    public static final int STATUS_BAD_REQUEST = 59;

    public static final int STATUS_CLIENT_CERT_REQUIRED = 60;
    public static final int STATUS_CERT_NOT_AUTHORIZED = 61;
    public static final int STATUS_CERT_NOT_VALID = 62;

    // Helper methods for ranges
    public static boolean isInput(int code) {
        return code >= CATEGORY_INPUT && code < CATEGORY_SUCCESS;
    }

    public static boolean isSuccess(int code) {
        return code >= CATEGORY_SUCCESS && code < CATEGORY_REDIRECT;
    }

    public static boolean isRedirect(int code) {
        return code >= CATEGORY_REDIRECT && code < CATEGORY_TEMP_FAILURE;
    }

    public static boolean isTemporaryFailure(int code) {
        return code >= CATEGORY_TEMP_FAILURE && code < CATEGORY_PERM_FAILURE;
    }

    public static boolean isPermanentFailure(int code) {
        return code >= CATEGORY_PERM_FAILURE && code < CATEGORY_CLIENT_CERT;
    }

    public static boolean isClientCertificateRequired(int code) {
        return code >= CATEGORY_CLIENT_CERT && code < CATEGORY_RESERVED;
    }
}
