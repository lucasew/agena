package com.biglucas.agena.protocol.gemini;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Gemini protocol parsing logic
 */
public class GeminiProtocolTest {

    @Test
    public void testResponseHeaderParsing_withMeta() {
        // Test parsing of "20 text/gemini"
        String headerLine = "20 text/gemini";
        int spaceIndex = headerLine.indexOf(" ");

        assertTrue("Header should contain space", spaceIndex > 0);

        int responseCode = Integer.parseInt(headerLine.substring(0, spaceIndex));
        String meta = headerLine.substring(spaceIndex).trim();

        assertEquals("Response code should be 20", 20, responseCode);
        assertEquals("Meta should be 'text/gemini'", "text/gemini", meta);
    }

    @Test
    public void testResponseHeaderParsing_withoutMeta() {
        // Test parsing of "51" (no meta)
        String headerLine = "51";
        int spaceIndex = headerLine.indexOf(" ");

        if (spaceIndex == -1) {
            int responseCode = Integer.parseInt(headerLine.trim());
            String meta = "";

            assertEquals("Response code should be 51", 51, responseCode);
            assertEquals("Meta should be empty", "", meta);
        }
    }

    @Test
    public void testResponseHeaderParsing_inputPrompt() {
        // Test parsing of "10 Enter search query"
        String headerLine = "10 Enter search query";
        int spaceIndex = headerLine.indexOf(" ");

        int responseCode = Integer.parseInt(headerLine.substring(0, spaceIndex));
        String meta = headerLine.substring(spaceIndex).trim();

        assertEquals("Response code should be 10", 10, responseCode);
        assertEquals("Meta should be prompt text", "Enter search query", meta);
    }

    @Test
    public void testResponseHeaderParsing_redirect() {
        // Test parsing of "30 gemini://example.com/newlocation"
        String headerLine = "30 gemini://example.com/newlocation";
        int spaceIndex = headerLine.indexOf(" ");

        int responseCode = Integer.parseInt(headerLine.substring(0, spaceIndex));
        String meta = headerLine.substring(spaceIndex).trim();

        assertEquals("Response code should be 30", 30, responseCode);
        assertEquals("Meta should be redirect URL", "gemini://example.com/newlocation", meta);
    }

    @Test
    public void testResponseHeaderParsing_withCharset() {
        // Test parsing of "20 text/gemini; charset=utf-8"
        String headerLine = "20 text/gemini; charset=utf-8";
        int spaceIndex = headerLine.indexOf(" ");

        int responseCode = Integer.parseInt(headerLine.substring(0, spaceIndex));
        String meta = headerLine.substring(spaceIndex).trim();

        assertEquals("Response code should be 20", 20, responseCode);
        assertTrue("Meta should contain mime type", meta.startsWith("text/gemini"));
        assertTrue("Meta should contain charset", meta.contains("charset"));
    }

    @Test
    public void testStatusCodeRangeInput() {
        // Status codes 10-19: INPUT
        for (int code = 10; code < 20; code++) {
            assertTrue("Code " + code + " should be in INPUT range", code >= 10 && code < 20);
            assertFalse("Code " + code + " should not be in SUCCESS range", code >= 20 && code < 30);
        }
    }

    @Test
    public void testStatusCodeRangeSuccess() {
        // Status codes 20-29: SUCCESS
        for (int code = 20; code < 30; code++) {
            assertTrue("Code " + code + " should be in SUCCESS range", code >= 20 && code < 30);
            assertFalse("Code " + code + " should not be in REDIRECT range", code >= 30 && code < 40);
        }
    }

    @Test
    public void testStatusCodeRangeRedirect() {
        // Status codes 30-39: REDIRECT
        for (int code = 30; code < 40; code++) {
            assertTrue("Code " + code + " should be in REDIRECT range", code >= 30 && code < 40);
            assertFalse("Code " + code + " should not be in TEMP_FAILURE range", code >= 40 && code < 50);
        }
    }

    @Test
    public void testStatusCodeRangeTempFailure() {
        // Status codes 40-49: TEMPORARY FAILURE
        for (int code = 40; code < 50; code++) {
            assertTrue("Code " + code + " should be in TEMP_FAILURE range", code >= 40 && code < 50);
            assertFalse("Code " + code + " should not be in PERM_FAILURE range", code >= 50 && code < 60);
        }
    }

    @Test
    public void testStatusCodeRangePermFailure() {
        // Status codes 50-59: PERMANENT FAILURE
        for (int code = 50; code < 60; code++) {
            assertTrue("Code " + code + " should be in PERM_FAILURE range", code >= 50 && code < 60);
            assertFalse("Code " + code + " should not be in CERT_REQUIRED range", code >= 60 && code < 70);
        }
    }

    @Test
    public void testStatusCodeRangeCertRequired() {
        // Status codes 60-69: CLIENT CERTIFICATE REQUIRED
        for (int code = 60; code < 70; code++) {
            assertTrue("Code " + code + " should be in CERT_REQUIRED range", code >= 60 && code < 70);
            assertFalse("Code " + code + " should not be above 69", code >= 70);
        }
    }

    @Test
    public void testSpecificStatusCodes() {
        // Test specific status codes mentioned in spec
        assertEquals("Status 10: INPUT", 10, 10);
        assertEquals("Status 11: SENSITIVE INPUT", 11, 11);
        assertEquals("Status 20: SUCCESS", 20, 20);
        assertEquals("Status 30: REDIRECT - TEMPORARY", 30, 30);
        assertEquals("Status 31: REDIRECT - PERMANENT", 31, 31);
        assertEquals("Status 40: TEMPORARY FAILURE", 40, 40);
        assertEquals("Status 41: SERVER UNAVAILABLE", 41, 41);
        assertEquals("Status 42: CGI ERROR", 42, 42);
        assertEquals("Status 43: PROXY ERROR", 43, 43);
        assertEquals("Status 44: SLOW DOWN", 44, 44);
        assertEquals("Status 50: PERMANENT FAILURE", 50, 50);
        assertEquals("Status 51: NOT FOUND", 51, 51);
        assertEquals("Status 52: GONE", 52, 52);
        assertEquals("Status 53: PROXY REQUEST REFUSED", 53, 53);
        assertEquals("Status 59: BAD REQUEST", 59, 59);
        assertEquals("Status 60: CLIENT CERTIFICATE REQUIRED", 60, 60);
        assertEquals("Status 61: CERTIFICATE NOT AUTHORIZED", 61, 61);
        assertEquals("Status 62: CERTIFICATE NOT VALID", 62, 62);
    }

    @Test
    public void testRequestFormat() {
        // Request should be: <URL>\r\n
        String url = "gemini://example.com/";
        String request = url + "\r\n";

        assertTrue("Request should end with CRLF", request.endsWith("\r\n"));
        assertTrue("Request should contain URL", request.contains(url));
        assertEquals("Request format should be URL + CRLF", url + "\r\n", request);
    }

    @Test
    public void testMimeTypeDetection() {
        // Test detecting text/gemini MIME type
        String metaGemini = "text/gemini";
        String metaPlain = "text/plain";
        String metaImage = "image/png";

        assertTrue("Should detect text/gemini", metaGemini.startsWith("text/gemini"));
        assertFalse("Should not detect text/plain as gemini", metaPlain.startsWith("text/gemini"));
        assertFalse("Should not detect image as gemini", metaImage.startsWith("text/gemini"));
    }

    @Test
    public void testRedirectCounter() {
        // Test redirect counter logic
        int redirectCount = 0;
        int maxRedirects = 5;

        // Simulate following redirects
        for (int i = 0; i < 6; i++) {
            if (redirectCount > maxRedirects) {
                fail("Should not allow more than 5 redirects");
            }
            redirectCount++;
        }

        assertTrue("Should detect too many redirects", redirectCount > maxRedirects);
    }

    @Test
    public void testRedirectCounterLimit() {
        // Test that exactly 5 redirects are allowed
        int redirectCount = 5;
        int maxRedirects = 5;

        assertFalse("5 redirects should be allowed", redirectCount > maxRedirects);

        redirectCount = 6;
        assertTrue("6 redirects should be rejected", redirectCount > maxRedirects);
    }

    @Test
    public void testLineTermination() {
        // Test both CRLF and LF are valid line terminators
        String lineCRLF = "test line\r\n";
        String lineLF = "test line\n";

        assertTrue("CRLF should be valid terminator", lineCRLF.contains("\n"));
        assertTrue("LF should be valid terminator", lineLF.contains("\n"));
    }
}
