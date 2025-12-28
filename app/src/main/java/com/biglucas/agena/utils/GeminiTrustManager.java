package com.biglucas.agena.utils;

import android.annotation.SuppressLint;

import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * Trust manager that accepts all certificates, implementing a simplified TOFU model.
 *
 * In the Gemini ecosystem, self-signed certificates are the norm, and the recommended
 * approach is TOFU (Trust On First Use). This implementation accepts all certificates,
 * which is appropriate for Gemini but would not be suitable for traditional HTTPS.
 *
 * A complete TOFU implementation would:
 * 1. Store certificate fingerprints on first connection
 * 2. Verify certificates match stored fingerprints on subsequent connections
 * 3. Alert users when certificates change
 *
 * This is left as a future enhancement.
 */
@SuppressLint("CustomX509TrustManager")
public class GeminiTrustManager implements X509TrustManager {
    @SuppressLint("TrustAllX509TrustManager")
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
        // Accept all client certificates (Gemini TOFU model)
    }

    @SuppressLint("TrustAllX509TrustManager")
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) {
        // Accept all server certificates (Gemini TOFU model)
        // In a full TOFU implementation, we would:
        // - Store the certificate fingerprint on first connection
        // - Verify it matches on subsequent connections
        // - Alert the user if it changes
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
