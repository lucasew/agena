package com.biglucas.agena.protocol.gemini;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GemtextParserTest {

    @Test
    public void emptyInputYieldsNoElements() {
        assertTrue(GemtextParser.parse(null).isEmpty());
        assertTrue(GemtextParser.parse(Collections.emptyList()).isEmpty());
    }

    @Test
    public void plainLinesPassThrough() {
        List<GemtextParser.Element> elements = GemtextParser.parse(Arrays.asList(
                "# Heading",
                "Hello",
                "=> /link Label"
        ));
        assertEquals(3, elements.size());
        assertTrue(elements.get(0) instanceof GemtextParser.Line);
        assertEquals("# Heading", ((GemtextParser.Line) elements.get(0)).raw);
        assertEquals("Hello", ((GemtextParser.Line) elements.get(1)).raw);
        assertEquals("=> /link Label", ((GemtextParser.Line) elements.get(2)).raw);
    }

    @Test
    public void closedPreformattedBlockIsCollectedWithoutLeadingNewline() {
        List<GemtextParser.Element> elements = GemtextParser.parse(Arrays.asList(
                "before",
                "```",
                "line1",
                "line2",
                "```",
                "after"
        ));
        assertEquals(3, elements.size());
        assertEquals("before", ((GemtextParser.Line) elements.get(0)).raw);
        assertTrue(elements.get(1) instanceof GemtextParser.Preformatted);
        assertEquals("line1\nline2", ((GemtextParser.Preformatted) elements.get(1)).text);
        assertEquals("after", ((GemtextParser.Line) elements.get(2)).raw);
    }

    @Test
    public void unclosedPreformattedBlockIsStillEmitted() {
        // Bug: old GeminiPageContentFragment dropped body when the closing fence was missing.
        List<GemtextParser.Element> elements = GemtextParser.parse(Arrays.asList(
                "intro",
                "```",
                "code a",
                "code b"
        ));
        assertEquals(2, elements.size());
        assertEquals("intro", ((GemtextParser.Line) elements.get(0)).raw);
        assertTrue(elements.get(1) instanceof GemtextParser.Preformatted);
        assertEquals("code a\ncode b", ((GemtextParser.Preformatted) elements.get(1)).text);
    }

    @Test
    public void emptyPreformattedBlockIsEmitted() {
        List<GemtextParser.Element> elements = GemtextParser.parse(Arrays.asList(
                "```",
                "```"
        ));
        assertEquals(1, elements.size());
        assertTrue(elements.get(0) instanceof GemtextParser.Preformatted);
        assertEquals("", ((GemtextParser.Preformatted) elements.get(0)).text);
    }

    @Test
    public void fenceToggleIgnoresAltTextOnOpeningLine() {
        List<GemtextParser.Element> elements = GemtextParser.parse(Arrays.asList(
                "```python",
                "print(1)",
                "```"
        ));
        assertEquals(1, elements.size());
        assertEquals("print(1)", ((GemtextParser.Preformatted) elements.get(0)).text);
    }
}
