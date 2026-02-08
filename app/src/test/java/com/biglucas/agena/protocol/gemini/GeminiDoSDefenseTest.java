package com.biglucas.agena.protocol.gemini;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

public class GeminiDoSDefenseTest {

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

        Method readLineMethod = Gemini.class.getDeclaredMethod("readLineFromStream", InputStream.class);
        readLineMethod.setAccessible(true);

        try {
            readLineMethod.invoke(gemini, input);
            fail("Should have thrown GeminiResponseTooLarge exception");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            assertTrue("Expected GeminiResponseTooLarge, got " + cause.getClass().getSimpleName(),
                       cause instanceof FailedGeminiRequestException.GeminiResponseTooLarge);
        }
    }
}
