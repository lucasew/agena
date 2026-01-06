package com.biglucas.agena.utils;

import org.conscrypt.Conscrypt;

import java.security.Security;

public class SecurityProvider {
    private SecurityProvider() {
        // This is a utility class and should not be instantiated.
    }

    public static void addConscryptIfAvailable() {
        Security.insertProviderAt(Conscrypt.newProvider(), 1);
    }
}
