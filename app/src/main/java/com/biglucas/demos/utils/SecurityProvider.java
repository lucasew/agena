package com.biglucas.demos.utils;

import org.conscrypt.Conscrypt;

import java.security.Security;

public class SecurityProvider {
    public static void addConscryptIfAvailable() {
        Security.insertProviderAt(Conscrypt.newProvider(), 1);
    }
}
