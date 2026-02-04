package com.biglucas.agena.protocol.gemini;

public class GeminiSingleton {
    private static Gemini gemini;

    public static synchronized Gemini getGemini() {
        if (GeminiSingleton.gemini == null) {
            GeminiSingleton.gemini = new Gemini();
        }
        return GeminiSingleton.gemini;
    }
}
