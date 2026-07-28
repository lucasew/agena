package com.biglucas.agena.protocol.gemini;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link GeminiDownloader#extensionFromPath(String)}.
 */
public class GeminiDownloaderExtensionTest {

    @Test
    public void extensionFromSimpleFilename() {
        assertEquals("pdf", GeminiDownloader.extensionFromPath("/docs/manual.pdf"));
    }

    @Test
    public void extensionFromCompoundFilename() {
        assertEquals("gz", GeminiDownloader.extensionFromPath("/archive.tar.gz"));
    }

    @Test
    public void extensionDefaultsWhenPathHasNoDot() {
        // Old code split the full URI and would use host label "com" as the extension.
        assertEquals("bin", GeminiDownloader.extensionFromPath("/path/file"));
    }

    @Test
    public void extensionDefaultsForRootOrEmpty() {
        assertEquals("bin", GeminiDownloader.extensionFromPath("/"));
        assertEquals("bin", GeminiDownloader.extensionFromPath(""));
        assertEquals("bin", GeminiDownloader.extensionFromPath(null));
    }

    @Test
    public void extensionIgnoresLeadingDotNames() {
        assertEquals("bin", GeminiDownloader.extensionFromPath("/.gitignore"));
    }

    @Test
    public void extensionIsLowercased() {
        assertEquals("png", GeminiDownloader.extensionFromPath("/img/Logo.PNG"));
    }

    @Test
    public void extensionFromTrailingSlashDirectoryLookingPath() {
        assertEquals("pdf", GeminiDownloader.extensionFromPath("/docs/manual.pdf/"));
    }
}
