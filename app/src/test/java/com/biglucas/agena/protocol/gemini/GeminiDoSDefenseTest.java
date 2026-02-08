package com.biglucas.agena.protocol.gemini;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for Denial of Service (DoS) defenses in the Gemini client.
 */
public class GeminiDoSDefenseTest {

    /**
     * Verifies that `readLineFromStream` throws an exception when a line exceeds the maximum allowed length.
     * This prevents OOM attacks via infinitely long lines.
     */
    @Test
    public void testReadLineFromStream_ExceedsMaxLineLength() throws Exception {
        // Create a very long line > 4096 bytes
        int length = GeminiSpec.MAX_LINE_LENGTH_BYTES + 100;
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append('a');
        }
        sb.append('\n');

        InputStream input = new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
        Gemini gemini = new Gemini();

        try {
            // Using package-private access to avoid reflection
            gemini.readLineFromStream(input);
            fail("Should have thrown GeminiResponseTooLarge exception");
        } catch (FailedGeminiRequestException.GeminiResponseTooLarge e) {
            // Success
            assertTrue(e.getMessage().contains("Line length exceeded limit"));
        }
    }
}
