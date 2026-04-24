package com.biglucas.agena.protocol.gemini;

/**
 * Provides a global, thread-safe access point for the {@link Gemini} protocol client.
 * <p>
 * Ensures that only a single instance of the Gemini client is instantiated throughout
 * the application's lifecycle, reducing overhead and centralizing network handling logic.
 */
public class GeminiSingleton {
    private static Gemini gemini;

    /**
     * Retrieves the singleton instance of the Gemini client.
     * <p>
     * This method is synchronized to guarantee thread safety during initialization,
     * preventing race conditions when accessed concurrently from multiple threads.
     *
     * @return The global {@link Gemini} instance.
     */
    public static synchronized Gemini getGemini() {
        if (GeminiSingleton.gemini == null) {
            GeminiSingleton.gemini = new Gemini();
        }
        return GeminiSingleton.gemini;
    }
}
