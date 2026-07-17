package com.biglucas.agena.protocol.gemini;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses {@code text/gemini} source lines into structural elements.
 * <p>
 * Keeps preformatted-block state out of the Android UI layer so the rules can be
 * unit-tested. An unclosed preformatted fence at end-of-input is treated as a
 * complete block (common on partial or poorly authored pages).
 */
public final class GemtextParser {
    private GemtextParser() {}

    /**
     * One structural piece of a gemtext document.
     */
    public abstract static class Element {
        private Element() {}
    }

    /**
     * A preformatted block body (without the surrounding fence lines).
     */
    public static final class Preformatted extends Element {
        public final String text;

        public Preformatted(String text) {
            this.text = text;
        }
    }

    /**
     * A non-preformatted source line (heading, link, list item, or plain text).
     */
    public static final class Line extends Element {
        public final String raw;

        public Line(String raw) {
            this.raw = raw;
        }
    }

    /**
     * Parses gemtext lines into ordered elements.
     *
     * @param lines source lines (may be empty; null entries are treated as empty strings)
     * @return unmodifiable list of elements in document order
     */
    public static List<Element> parse(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return Collections.emptyList();
        }

        List<Element> out = new ArrayList<>(lines.size());
        StringBuilder preformatted = null;

        for (String raw : lines) {
            String item = raw == null ? "" : raw;

            if (item.startsWith("```")) {
                if (preformatted != null) {
                    out.add(new Preformatted(preformatted.toString()));
                    preformatted = null;
                } else {
                    preformatted = new StringBuilder();
                }
                continue;
            }

            if (preformatted != null) {
                if (preformatted.length() > 0) {
                    preformatted.append('\n');
                }
                preformatted.append(item);
                continue;
            }

            out.add(new Line(item));
        }

        // Unclosed fence: still emit what was collected so content is not dropped.
        if (preformatted != null) {
            out.add(new Preformatted(preformatted.toString()));
        }

        return Collections.unmodifiableList(out);
    }
}
