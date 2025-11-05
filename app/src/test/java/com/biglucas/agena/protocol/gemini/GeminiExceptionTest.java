package com.biglucas.agena.protocol.gemini;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Gemini exception classes
 */
public class GeminiExceptionTest {

    @Test
    public void testGeminiInputRequired_regular() {
        String prompt = "Enter your search query";
        FailedGeminiRequestException.GeminiInputRequired exception =
                new FailedGeminiRequestException.GeminiInputRequired(prompt, false);

        assertEquals(prompt, exception.getPrompt());
        assertFalse(exception.isSensitive());
        assertEquals("Input required", exception.getMessage());
    }

    @Test
    public void testGeminiInputRequired_sensitive() {
        String prompt = "Enter your password";
        FailedGeminiRequestException.GeminiInputRequired exception =
                new FailedGeminiRequestException.GeminiInputRequired(prompt, true);

        assertEquals(prompt, exception.getPrompt());
        assertTrue(exception.isSensitive());
        assertEquals("Sensitive input required", exception.getMessage());
    }

    @Test
    public void testGeminiTooManyRedirects() {
        FailedGeminiRequestException.GeminiTooManyRedirects exception =
                new FailedGeminiRequestException.GeminiTooManyRedirects();

        assertEquals("Too many redirects (max 5)", exception.getMessage());
    }

    @Test
    public void testGeminiTemporaryFailure() {
        String message = "Server is down";
        FailedGeminiRequestException.GeminiTemporaryFailure exception =
                new FailedGeminiRequestException.GeminiTemporaryFailure(message);

        assertEquals("Temporary failure: " + message, exception.getMessage());
    }

    @Test
    public void testGeminiServerUnavailable() {
        String message = "Maintenance mode";
        FailedGeminiRequestException.GeminiServerUnavailable exception =
                new FailedGeminiRequestException.GeminiServerUnavailable(message);

        assertEquals("Server unavailable: " + message, exception.getMessage());
    }

    @Test
    public void testGeminiCGIError() {
        String message = "Script error";
        FailedGeminiRequestException.GeminiCGIError exception =
                new FailedGeminiRequestException.GeminiCGIError(message);

        assertEquals("CGI error: " + message, exception.getMessage());
    }

    @Test
    public void testGeminiProxyError() {
        String message = "Proxy timeout";
        FailedGeminiRequestException.GeminiProxyError exception =
                new FailedGeminiRequestException.GeminiProxyError(message);

        assertEquals("Proxy error: " + message, exception.getMessage());
    }

    @Test
    public void testGeminiSlowDown() {
        String message = "Wait 60 seconds";
        FailedGeminiRequestException.GeminiSlowDown exception =
                new FailedGeminiRequestException.GeminiSlowDown(message);

        assertEquals("Slow down: " + message, exception.getMessage());
        assertEquals(message, exception.getWaitMessage());
    }

    @Test
    public void testGeminiPermanentFailure() {
        String message = "Resource deleted";
        FailedGeminiRequestException.GeminiPermanentFailure exception =
                new FailedGeminiRequestException.GeminiPermanentFailure(message);

        assertEquals("Permanent failure: " + message, exception.getMessage());
    }

    @Test
    public void testGeminiNotFound() {
        FailedGeminiRequestException.GeminiNotFound exception =
                new FailedGeminiRequestException.GeminiNotFound();

        assertEquals("Content not found", exception.getMessage());
    }

    @Test
    public void testGeminiGone() {
        FailedGeminiRequestException.GeminiGone exception =
                new FailedGeminiRequestException.GeminiGone();

        assertEquals("Gone", exception.getMessage());
    }

    @Test
    public void testGeminiProxyRequestRefused() {
        String message = "Proxy denied";
        FailedGeminiRequestException.GeminiProxyRequestRefused exception =
                new FailedGeminiRequestException.GeminiProxyRequestRefused(message);

        assertEquals("Proxy request refused: " + message, exception.getMessage());
    }

    @Test
    public void testGeminiBadRequest() {
        String message = "Invalid URI format";
        FailedGeminiRequestException.GeminiBadRequest exception =
                new FailedGeminiRequestException.GeminiBadRequest(message);

        assertEquals("Bad request: " + message, exception.getMessage());
    }

    @Test
    public void testGeminiClientCertificateRequired() {
        String message = "Certificate needed";
        FailedGeminiRequestException.GeminiClientCertificateRequired exception =
                new FailedGeminiRequestException.GeminiClientCertificateRequired(message);

        assertEquals("Client certificate required: " + message, exception.getMessage());
    }

    @Test
    public void testGeminiCertificateNotAuthorized() {
        String message = "Certificate rejected";
        FailedGeminiRequestException.GeminiCertificateNotAuthorized exception =
                new FailedGeminiRequestException.GeminiCertificateNotAuthorized(message);

        assertEquals("Certificate not authorized: " + message, exception.getMessage());
    }

    @Test
    public void testGeminiCertificateNotValid() {
        String message = "Certificate expired";
        FailedGeminiRequestException.GeminiCertificateNotValid exception =
                new FailedGeminiRequestException.GeminiCertificateNotValid(message);

        assertEquals("Certificate not valid: " + message, exception.getMessage());
    }

    @Test
    public void testGeminiInvalidUri() {
        String message = "URI too long";
        FailedGeminiRequestException.GeminiInvalidUri exception =
                new FailedGeminiRequestException.GeminiInvalidUri(message);

        assertEquals("Invalid URI: " + message, exception.getMessage());
    }

    @Test
    public void testGeminiInvalidResponse() {
        FailedGeminiRequestException.GeminiInvalidResponse exception =
                new FailedGeminiRequestException.GeminiInvalidResponse();

        assertEquals("Invalid response", exception.getMessage());
    }

    @Test
    public void testGeminiUnimplementedCase() {
        FailedGeminiRequestException.GeminiUnimplementedCase exception =
                new FailedGeminiRequestException.GeminiUnimplementedCase();

        assertEquals("Unimplemented case", exception.getMessage());
    }
}
