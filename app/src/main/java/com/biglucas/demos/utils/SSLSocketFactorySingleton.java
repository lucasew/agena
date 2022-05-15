package com.biglucas.demos.utils;

import android.os.Build;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

public class SSLSocketFactorySingleton {
    private static SSLSocketFactory factory = null;

    public static SSLSocketFactory getSSLSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
        if (factory != null) {
            return factory;
        }
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            SecurityProvider.addConscryptIfAvailable();
        }
        SSLContext sslContext = SSLContext.getInstance("TLS");
        X509TrustManager[] trustManagers = {new SSLSocketFactorySingleton.DummyTrustManager()};
        sslContext.init(null, trustManagers, null);
        SSLSocketFactorySingleton.factory = sslContext.getSocketFactory();
        return factory;
    }

    private static class DummyTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
