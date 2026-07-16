package com.biglucas.agena.protocol.gemini;

import org.junit.Test;
import static org.junit.Assert.*;

public class GeminiUriHelperTest {

    @Test
    public void testResolveRelative() {
        String base = "gemini://example.com/foo";
        String target = "bar";
        // /foo doesn't end in / or .gmi, so / is appended -> /foo/bar
        assertEquals("gemini://example.com/foo/bar", GeminiUriHelper.resolve(base, target));
    }

    @Test
    public void testResolveRelativeWithSlash() {
        String base = "gemini://example.com/foo/";
        String target = "bar";
        assertEquals("gemini://example.com/foo/bar", GeminiUriHelper.resolve(base, target));
    }

    @Test
    public void testResolveAbsolute() {
        String base = "gemini://example.com/foo";
        String target = "gemini://other.com/baz";
        assertEquals("gemini://other.com/baz", GeminiUriHelper.resolve(base, target));
    }

    @Test
    public void testSanitizeMalformed() {
        String base = "gemini://example.com/";
        String badTarget = "foo bar"; // space -> URI.create fails
        // fallback removes space -> foobar
        assertEquals("gemini://example.com/foobar", GeminiUriHelper.resolve(base, badTarget));
    }

    @Test
    public void testGmiExtension() {
        String base = "gemini://example.com/file.gmi";
        String target = "other.gmi";
        // .gmi doesn't append slash -> relative to parent directory (replaces file)
        assertEquals("gemini://example.com/other.gmi", GeminiUriHelper.resolve(base, target));
    }

    @Test
    public void testSpecialCharsRemoval() {
        // Test removing special chars
        String base = "gemini://example.com/";
        String reallyBadTarget = "foo<bar>";
        // < > not allowed. URI.create throws IAE.
        // Regex removes < >. -> foobar.
        assertEquals("gemini://example.com/foobar", GeminiUriHelper.resolve(base, reallyBadTarget));
    }

    @Test
    public void testResolveRelativePreservesQueryDirectorySemantics() {
        // After a Gemini input (10) response the page URI often carries a query string.
        // Directory slash must apply to the path only, not the full string (which would corrupt the query).
        String base = "gemini://example.com/cgi?user-input";
        String target = "bar";
        assertEquals("gemini://example.com/cgi/bar", GeminiUriHelper.resolve(base, target));
    }

    @Test
    public void testResolveRelativeWithQueryAndExistingSlash() {
        String base = "gemini://example.com/cgi/?user-input";
        String target = "bar";
        assertEquals("gemini://example.com/cgi/bar", GeminiUriHelper.resolve(base, target));
    }

    @Test
    public void testResolveAbsoluteIgnoresBaseQuery() {
        String base = "gemini://example.com/cgi?user-input";
        String target = "gemini://other.com/baz";
        assertEquals("gemini://other.com/baz", GeminiUriHelper.resolve(base, target));
    }

    @Test
    public void testHostOnlyBaseGetsRootSlash() {
        String base = "gemini://example.com";
        String target = "index.gmi";
        assertEquals("gemini://example.com/index.gmi", GeminiUriHelper.resolve(base, target));
    }
}
