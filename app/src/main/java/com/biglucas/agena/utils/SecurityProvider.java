package com.biglucas.agena.utils;

import org.conscrypt.Conscrypt;

import java.security.Security;

public final class SecurityProvider {
    private SecurityProvider() {
        // This is a utility class and should not be instantiated.
        throw new AssertionError("This class is not meant to be instantiated.");
    }

    public static void addConscryptIfAvailable() {
        if (Security.getProvider("Conscrypt") == null) {
            Security.insertProviderAt(Conscrypt.newProvider(), 1);
        }
    }
}
