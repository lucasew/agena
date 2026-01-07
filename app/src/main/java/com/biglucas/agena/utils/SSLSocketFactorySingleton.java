package com.biglucas.agena.utils;

import android.annotation.SuppressLint;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Provides SSL/TLS socket factory for Gemini protocol connections.
 * <p>
 * Gemini Protocol TLS Requirements (from specification):
 * - TLS version 1.2 or higher is mandatory
 * - SNI (Server Name Indication) must be used
 * - TOFU (Trust On First Use) model is recommended for certificate validation
 * - Self-signed certificates are considered legitimate in Gemini ecosystem
 * - Servers must use TLS close_notify to terminate connections
 * <p>
 * Current Implementation:
 * - Uses "TLS" context which supports TLS 1.2+ (exact version negotiated by SSL handshake)
 * - Conscrypt provider ensures modern TLS support on older Android devices
 * - SNI is enabled by default in Android's SSLSocket implementation
 * - Currently accepts all certificates (similar to TOFU but without persistence)
 * <p>
 * Note: This implementation uses a permissive trust manager that accepts all certificates.
 * This is appropriate for Gemini's TOFU model but means ALL certificates are trusted.
 * A full TOFU implementation would persist first-seen certificates and alert on changes.
 */
public class SSLSocketFactorySingleton {
    private static volatile SSLSocketFactory factory = null;

    /**
     * Returns a singleton SSLSocketFactory configured for Gemini protocol.
     * The factory uses TLS 1.2+ and accepts all certificates per Gemini's TOFU model.
     */
    public static SSLSocketFactory getSSLSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
        if (factory == null) {
            synchronized (SSLSocketFactorySingleton.class) {
                if (factory == null) {
                    // Add Conscrypt security provider for modern TLS support on older Android versions
                    SecurityProvider.addConscryptIfAvailable();

                    // Use "TLS" context which supports TLS 1.2+ (specific version negotiated during handshake)
                    // Note: On modern Android (API 20+), this defaults to TLS 1.2 or higher
                    SSLContext sslContext = SSLContext.getInstance("TLS");

                    // Initialize with permissive trust manager (TOFU-style, accepts all certificates)
                    X509TrustManager[] trustManagers = {new GeminiTrustManager()};
                    sslContext.init(null, trustManagers, null);

                    SSLSocketFactorySingleton.factory = sslContext.getSocketFactory();
                }
            }
        }
        return factory;
    }
}
