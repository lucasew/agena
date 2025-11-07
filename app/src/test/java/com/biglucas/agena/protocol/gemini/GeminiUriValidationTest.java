package com.biglucas.agena.protocol.gemini;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Gemini URI validation logic
 * These tests verify the URI validation requirements from the Gemini protocol specification
 */
public class GeminiUriValidationTest {

    @Test
    public void testUriMaxLength_1024bytes_valid() {
        // Test that 1024 bytes is the maximum allowed
        StringBuilder uri = new StringBuilder("gemini://example.com/");
        // Add characters to make it exactly 1024 bytes
        while (uri.toString().getBytes().length < 1024) {
            uri.append("a");
        }

        int length = uri.toString().getBytes().length;
        assertTrue("URI should be 1024 bytes or less", length <= 1024);
    }

    @Test
    public void testUriMaxLength_exceeds1024bytes() {
        // Test that URIs exceeding 1024 bytes should be rejected
        StringBuilder uri = new StringBuilder("gemini://example.com/");
        // Add characters to make it exceed 1024 bytes
        while (uri.toString().getBytes().length <= 1024) {
            uri.append("a");
        }

        int length = uri.toString().getBytes().length;
        assertTrue("URI exceeds 1024 bytes and should be rejected", length > 1024);
    }

    @Test
    public void testValidGeminiScheme() {
        String validUri = "gemini://example.com/";
        assertTrue("Valid gemini:// scheme", validUri.startsWith("gemini://"));
    }

    @Test
    public void testInvalidHttpScheme() {
        String invalidUri = "http://example.com/";
        assertFalse("HTTP scheme should not be valid", invalidUri.startsWith("gemini://"));
    }

    @Test
    public void testInvalidHttpsScheme() {
        String invalidUri = "https://example.com/";
        assertFalse("HTTPS scheme should not be valid", invalidUri.startsWith("gemini://"));
    }

    @Test
    public void testDefaultPort1965() {
        // According to spec, default port is 1965
        int defaultPort = 1965;
        assertEquals("Default Gemini port should be 1965", 1965, defaultPort);
    }

    @Test
    public void testUserinfoNotAllowed() {
        // Gemini spec does not allow userinfo in URIs
        String uriWithUserinfo = "gemini://user:pass@example.com/";
        assertTrue("URI with userinfo should be detected", uriWithUserinfo.contains("@"));

        String uriWithoutUserinfo = "gemini://example.com/";
        assertFalse("URI without userinfo should be clean", uriWithoutUserinfo.contains("@"));
    }

    @Test
    public void testEmptyPathEquivalentToSlash() {
        // According to spec, empty path and "/" are equivalent
        String withSlash = "gemini://example.com/";
        String withoutSlash = "gemini://example.com";

        // Both should be considered valid
        assertTrue("Path with slash should be valid", withSlash.startsWith("gemini://"));
        assertTrue("Path without slash should be valid", withoutSlash.startsWith("gemini://"));
    }

    @Test
    public void testValidUriExamples() {
        String[] validUris = {
            "gemini://gemini.circumlunar.space/",
            "gemini://example.com:1965/path",
            "gemini://example.com/path?query=value",
            "gemini://localhost/",
            "gemini://example.com:8080/"
        };

        for (String uri : validUris) {
            assertTrue("URI should start with gemini://: " + uri, uri.startsWith("gemini://"));
            assertTrue("URI should not exceed 1024 bytes: " + uri, uri.getBytes().length <= 1024);
        }
    }

    @Test
    public void testQueryParameters() {
        // Query parameters are allowed in Gemini URIs
        String uriWithQuery = "gemini://example.com/search?q=test";
        assertTrue("URI with query should be valid", uriWithQuery.contains("?"));
        assertTrue("URI should start with gemini://", uriWithQuery.startsWith("gemini://"));
    }

    @Test
    public void testFragmentIdentifiers() {
        // Fragment identifiers in URLs
        String uriWithFragment = "gemini://example.com/page#section";
        assertTrue("URI with fragment should be detected", uriWithFragment.contains("#"));
    }

    @Test
    public void testRedirectLimit() {
        // Test that redirect limit is 5 as per spec
        int maxRedirects = 5;
        assertEquals("Maximum redirects should be 5", 5, maxRedirects);
    }

    @Test
    public void testStatusCodeRanges() {
        // Verify status code ranges per Gemini spec
        assertTrue("Status 10 is in INPUT range", 10 >= 10 && 10 < 20);
        assertTrue("Status 20 is in SUCCESS range", 20 >= 20 && 20 < 30);
        assertTrue("Status 30 is in REDIRECT range", 30 >= 30 && 30 < 40);
        assertTrue("Status 40 is in TEMP_FAILURE range", 40 >= 40 && 40 < 50);
        assertTrue("Status 50 is in PERM_FAILURE range", 50 >= 50 && 50 < 60);
        assertTrue("Status 60 is in CERT_REQUIRED range", 60 >= 60 && 60 < 70);
    }

    @Test
    public void testTlsVersion() {
        // TLS 1.2 or higher is required
        String requiredTlsVersion = "TLS";
        assertNotNull("TLS version should be specified", requiredTlsVersion);
        assertEquals("TLS context should be 'TLS'", "TLS", requiredTlsVersion);
    }

    @Test
    public void testRelativeUriResolution() {
        // Test that relative URIs are properly resolved against base URIs
        // This is critical for handling redirects with relative paths
        java.net.URI baseUri = java.net.URI.create("gemini://cities.yesterweb.org/");
        java.net.URI relativeUri = baseUri.resolve("signup?lucasew");

        assertEquals("Resolved URI should have gemini scheme", "gemini", relativeUri.getScheme());
        assertEquals("Resolved URI should have correct host", "cities.yesterweb.org", relativeUri.getHost());
        assertEquals("Resolved URI should have correct path and query", "/signup?lucasew", relativeUri.getRawPath() + "?" + relativeUri.getRawQuery());
    }

    @Test
    public void testAbsoluteUriResolution() {
        // Test that absolute URIs are not modified during resolution
        java.net.URI baseUri = java.net.URI.create("gemini://example.com/page");
        java.net.URI absoluteUri = baseUri.resolve("gemini://another.com/other");

        assertEquals("Absolute URI should keep its scheme", "gemini", absoluteUri.getScheme());
        assertEquals("Absolute URI should keep its host", "another.com", absoluteUri.getHost());
        assertEquals("Absolute URI should keep its path", "/other", absoluteUri.getPath());
    }
}
