package com.biglucas.demos;

import java.security.Security;
import org.conscrypt.Conscrypt;

public class SecurityProvider {
    public static void addConscryptIfAvailable() {
        Security.insertProviderAt(Conscrypt.newProvider(), 1);
    }
}
