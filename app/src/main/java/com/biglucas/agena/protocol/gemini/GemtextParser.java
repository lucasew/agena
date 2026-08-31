package com.biglucas.agena.protocol.gemini;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Parses {@code text/gemini} source lines into structural elements.
 * <p>
 * Keeps preformatted-block state and line-type classification out of the Android
 * UI layer so the rules can be unit-tested. An unclosed preformatted fence at
 * end-of-input is treated as a complete block (common on partial or poorly
 * authored pages). Empty {@code =>} lines (no URL token) are omitted.
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
     * A {@code =>} link line.
     */
    public static final class Link extends Element {
        public final String target;
        public final String label;

        public Link(String target, String label) {
            this.target = target;
            this.label = label;
        }
    }

    /**
     * A heading line ({@code #} … {@code ####+}).
     */
    public static final class Heading extends Element {
        public final int level;
        public final String text;

        public Heading(int level, String text) {
            this.level = level;
            this.text = text;
        }
    }

    /**
     * A list item line ({@code *}).
     */
    public static final class ListItem extends Element {
        public final String text;

        public ListItem(String text) {
            this.text = text;
        }
    }

    /**
     * A non-preformatted, non-link, non-heading, non-list source line.
     */
    public static final class Text extends Element {
        public final String raw;

        public Text(String raw) {
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

            Element classified = classifyLine(item);
            if (classified != null) {
                out.add(classified);
            }
        }

        // Unclosed fence: still emit what was collected so content is not dropped.
        if (preformatted != null) {
            out.add(new Preformatted(preformatted.toString()));
        }

        return Collections.unmodifiableList(out);
    }

    /**
     * Classifies a non-preformatted source line. Returns {@code null} for an
     * empty {@code =>} line so the UI does not render a dead control.
     */
    static Element classifyLine(String item) {
        if (item.startsWith("=>")) {
            return parseLink(item);
        }

        int headingLevels = 0;
        for (int i = 0; i < item.length(); i++) {
            if (item.charAt(i) != '#') {
                break;
            }
            headingLevels++;
        }
        if (headingLevels > 0) {
            return new Heading(headingLevels, item.substring(headingLevels).trim());
        }
        if (item.startsWith("*")) {
            return new ListItem(item.substring(1).trim());
        }
        return new Text(item);
    }

    private static Link parseLink(String item) {
        StringTokenizer tokenizer = new StringTokenizer(item.substring(2));
        if (!tokenizer.hasMoreTokens()) {
            return null;
        }
        String target = tokenizer.nextToken().trim();
        StringBuilder label = new StringBuilder();
        while (tokenizer.hasMoreTokens()) {
            if (label.length() > 0) {
                label.append(' ');
            }
            label.append(tokenizer.nextToken());
        }
        String labelText = label.toString().trim();
        if (labelText.isEmpty()) {
            labelText = target;
        }
        return new Link(target, labelText);
    }
}
