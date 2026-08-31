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
    public void plainLinesAreClassified() {
        List<GemtextParser.Element> elements = GemtextParser.parse(Arrays.asList(
                "# Heading",
                "Hello",
                "=> /link Label"
        ));
        assertEquals(3, elements.size());
        assertTrue(elements.get(0) instanceof GemtextParser.Heading);
        assertEquals(1, ((GemtextParser.Heading) elements.get(0)).level);
        assertEquals("Heading", ((GemtextParser.Heading) elements.get(0)).text);
        assertTrue(elements.get(1) instanceof GemtextParser.Text);
        assertEquals("Hello", ((GemtextParser.Text) elements.get(1)).raw);
        assertTrue(elements.get(2) instanceof GemtextParser.Link);
        assertEquals("/link", ((GemtextParser.Link) elements.get(2)).target);
        assertEquals("Label", ((GemtextParser.Link) elements.get(2)).label);
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
        assertEquals("before", ((GemtextParser.Text) elements.get(0)).raw);
        assertTrue(elements.get(1) instanceof GemtextParser.Preformatted);
        assertEquals("line1\nline2", ((GemtextParser.Preformatted) elements.get(1)).text);
        assertEquals("after", ((GemtextParser.Text) elements.get(2)).raw);
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
        assertEquals("intro", ((GemtextParser.Text) elements.get(0)).raw);
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

    @Test
    public void linkJoinsMultiWordLabel() {
        List<GemtextParser.Element> elements = GemtextParser.parse(
                Collections.singletonList("=> /path  Foo   Bar"));
        GemtextParser.Link link = (GemtextParser.Link) elements.get(0);
        assertEquals("/path", link.target);
        assertEquals("Foo Bar", link.label);
    }

    @Test
    public void headingLevelCountsLeadingHashes() {
        List<GemtextParser.Element> elements = GemtextParser.parse(Arrays.asList(
                "## Two",
                "#### Four"
        ));
        assertEquals(2, ((GemtextParser.Heading) elements.get(0)).level);
        assertEquals("Two", ((GemtextParser.Heading) elements.get(0)).text);
        assertEquals(4, ((GemtextParser.Heading) elements.get(1)).level);
        assertEquals("Four", ((GemtextParser.Heading) elements.get(1)).text);
    }

    @Test
    public void linkWithoutLabelUsesTarget() {
        List<GemtextParser.Element> elements = GemtextParser.parse(Collections.singletonList("=> /only"));
        assertEquals(1, elements.size());
        GemtextParser.Link link = (GemtextParser.Link) elements.get(0);
        assertEquals("/only", link.target);
        assertEquals("/only", link.label);
    }

    @Test
    public void emptyLinkLineIsOmitted() {
        List<GemtextParser.Element> elements = GemtextParser.parse(Arrays.asList("=>", "=>   ", "kept"));
        assertEquals(1, elements.size());
        assertEquals("kept", ((GemtextParser.Text) elements.get(0)).raw);
    }

    @Test
    public void listItemStripsMarker() {
        List<GemtextParser.Element> elements = GemtextParser.parse(Collections.singletonList("* item"));
        assertEquals(1, elements.size());
        assertEquals("item", ((GemtextParser.ListItem) elements.get(0)).text);
    }

    @Test
    public void headingHashInPreformattedIsNotAHeading() {
        List<GemtextParser.Element> elements = GemtextParser.parse(Arrays.asList(
                "```",
                "# not a heading",
                "```"
        ));
        assertEquals(1, elements.size());
        assertEquals("# not a heading", ((GemtextParser.Preformatted) elements.get(0)).text);
    }
}
