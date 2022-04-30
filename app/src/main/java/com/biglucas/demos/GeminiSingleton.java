package com.biglucas.demos;

public class GeminiSingleton {
    private static Gemini gemini;

    public static Gemini getGemini() {
        if (GeminiSingleton.gemini == null) {
            GeminiSingleton.gemini = new Gemini();
        }
        return GeminiSingleton.gemini;
    }
}
