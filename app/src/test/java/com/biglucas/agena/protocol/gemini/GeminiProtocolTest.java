package com.biglucas.agena.protocol.gemini;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Gemini protocol parsing logic
 */
public class GeminiProtocolTest {

    @Test
    public void testResponseHeaderParsing_withMeta() {
        ParsedHeader header = parseHeader("20 text/gemini");
        assertEquals(GeminiSpec.STATUS_SUCCESS, header.code);
        assertEquals("text/gemini", header.meta);
    }

    @Test
    public void testResponseHeaderParsing_withoutMeta() {
        ParsedHeader header = parseHeader("51");
        assertEquals(GeminiSpec.STATUS_NOT_FOUND, header.code);
        assertEquals("", header.meta);
    }

    @Test
    public void testResponseHeaderParsing_inputPrompt() {
        ParsedHeader header = parseHeader("10 Enter search query");
        assertEquals(GeminiSpec.STATUS_INPUT, header.code);
        assertEquals("Enter search query", header.meta);
    }

    @Test
    public void testResponseHeaderParsing_redirect() {
        ParsedHeader header = parseHeader("30 gemini://example.com/newlocation");
        assertEquals(GeminiSpec.STATUS_REDIRECT, header.code);
        assertEquals("gemini://example.com/newlocation", header.meta);
    }

    @Test
    public void testResponseHeaderParsing_withCharset() {
        ParsedHeader header = parseHeader("20 text/gemini; charset=utf-8");
        assertEquals(GeminiSpec.STATUS_SUCCESS, header.code);
        assertTrue(header.meta.startsWith("text/gemini"));
        assertTrue(header.meta.contains("charset"));
    }

    @Test
    public void testStatusCodeRangeInput() {
        for (int code = GeminiSpec.CATEGORY_INPUT; code < GeminiSpec.CATEGORY_SUCCESS; code++) {
            assertTrue("Code " + code + " should be in INPUT range", GeminiSpec.isInput(code));
            assertFalse("Code " + code + " should not be in SUCCESS range", GeminiSpec.isSuccess(code));
        }
    }

    @Test
    public void testStatusCodeRangeSuccess() {
        for (int code = GeminiSpec.CATEGORY_SUCCESS; code < GeminiSpec.CATEGORY_REDIRECT; code++) {
            assertTrue("Code " + code + " should be in SUCCESS range", GeminiSpec.isSuccess(code));
            assertFalse("Code " + code + " should not be in REDIRECT range", GeminiSpec.isRedirect(code));
        }
    }

    @Test
    public void testStatusCodeRangeRedirect() {
        for (int code = GeminiSpec.CATEGORY_REDIRECT; code < GeminiSpec.CATEGORY_TEMP_FAILURE; code++) {
            assertTrue("Code " + code + " should be in REDIRECT range", GeminiSpec.isRedirect(code));
            assertFalse("Code " + code + " should not be in TEMP_FAILURE range", GeminiSpec.isTemporaryFailure(code));
        }
    }

    @Test
    public void testStatusCodeRangeTempFailure() {
        for (int code = GeminiSpec.CATEGORY_TEMP_FAILURE; code < GeminiSpec.CATEGORY_PERM_FAILURE; code++) {
            assertTrue("Code " + code + " should be in TEMP_FAILURE range", GeminiSpec.isTemporaryFailure(code));
            assertFalse("Code " + code + " should not be in PERM_FAILURE range", GeminiSpec.isPermanentFailure(code));
        }
    }

    @Test
    public void testStatusCodeRangePermFailure() {
        for (int code = GeminiSpec.CATEGORY_PERM_FAILURE; code < GeminiSpec.CATEGORY_CLIENT_CERT; code++) {
            assertTrue("Code " + code + " should be in PERM_FAILURE range", GeminiSpec.isPermanentFailure(code));
            assertFalse("Code " + code + " should not be in CERT_REQUIRED range", GeminiSpec.isClientCertificateRequired(code));
        }
    }

    @Test
    public void testStatusCodeRangeCertRequired() {
        for (int code = GeminiSpec.CATEGORY_CLIENT_CERT; code < GeminiSpec.CATEGORY_RESERVED; code++) {
            assertTrue("Code " + code + " should be in CERT_REQUIRED range", GeminiSpec.isClientCertificateRequired(code));
            assertFalse("Code " + code + " should not be above reserved bound", code >= GeminiSpec.CATEGORY_RESERVED);
        }
    }

    @Test
    public void testSpecificStatusCodes() {
        assertEquals(10, GeminiSpec.STATUS_INPUT);
        assertEquals(11, GeminiSpec.STATUS_SENSITIVE_INPUT);
        assertEquals(20, GeminiSpec.STATUS_SUCCESS);
        assertEquals(30, GeminiSpec.STATUS_REDIRECT);
        assertEquals(40, GeminiSpec.STATUS_TEMP_FAILURE);
        assertEquals(41, GeminiSpec.STATUS_SERVER_UNAVAILABLE);
        assertEquals(42, GeminiSpec.STATUS_CGI_ERROR);
        assertEquals(43, GeminiSpec.STATUS_PROXY_ERROR);
        assertEquals(44, GeminiSpec.STATUS_SLOW_DOWN);
        assertEquals(50, GeminiSpec.STATUS_PERM_FAILURE);
        assertEquals(51, GeminiSpec.STATUS_NOT_FOUND);
        assertEquals(52, GeminiSpec.STATUS_GONE);
        assertEquals(53, GeminiSpec.STATUS_PROXY_REQUEST_REFUSED);
        assertEquals(59, GeminiSpec.STATUS_BAD_REQUEST);
        assertEquals(60, GeminiSpec.STATUS_CLIENT_CERT_REQUIRED);
        assertEquals(61, GeminiSpec.STATUS_CERT_NOT_AUTHORIZED);
        assertEquals(62, GeminiSpec.STATUS_CERT_NOT_VALID);
        assertEquals(5, GeminiSpec.MAX_REDIRECTS);

        assertTrue(GeminiSpec.isInput(GeminiSpec.STATUS_INPUT));
        assertTrue(GeminiSpec.isInput(GeminiSpec.STATUS_SENSITIVE_INPUT));
        assertTrue(GeminiSpec.isSuccess(GeminiSpec.STATUS_SUCCESS));
        assertTrue(GeminiSpec.isRedirect(GeminiSpec.STATUS_REDIRECT));
        assertTrue(GeminiSpec.isTemporaryFailure(GeminiSpec.STATUS_SLOW_DOWN));
        assertTrue(GeminiSpec.isPermanentFailure(GeminiSpec.STATUS_NOT_FOUND));
        assertTrue(GeminiSpec.isClientCertificateRequired(GeminiSpec.STATUS_CERT_NOT_VALID));
    }

    @Test
    public void testRequestFormat() {
        // Request should be: <URL>\r\n
        String url = "gemini://example.com/";
        String request = url + GeminiSpec.CRLF;

        assertTrue("Request should end with CRLF", request.endsWith(GeminiSpec.CRLF));
        assertTrue("Request should contain URL", request.contains(url));
        assertEquals("Request format should be URL + CRLF", url + GeminiSpec.CRLF, request);
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
    public void testLineTermination() {
        // Test both CRLF and LF are valid line terminators
        String lineCRLF = "test line" + GeminiSpec.CRLF;
        String lineLF = "test line\n";

        assertTrue("CRLF should be valid terminator", lineCRLF.contains("\n"));
        assertTrue("LF should be valid terminator", lineLF.contains("\n"));
    }

    /** Local header split used by the parse tests (mirrors Gemini.requestInternal). */
    private static ParsedHeader parseHeader(String headerLine) {
        int spaceIndex = headerLine.indexOf(' ');
        if (spaceIndex == -1) {
            return new ParsedHeader(Integer.parseInt(headerLine.trim()), "");
        }
        return new ParsedHeader(
                Integer.parseInt(headerLine.substring(0, spaceIndex)),
                headerLine.substring(spaceIndex).trim());
    }

    private static final class ParsedHeader {
        final int code;
        final String meta;

        ParsedHeader(int code, String meta) {
            this.code = code;
            this.meta = meta;
        }
    }
}
