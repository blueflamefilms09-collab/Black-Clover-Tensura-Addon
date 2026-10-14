package com.newuniverse.nusmp.client.ui.theme;

import java.util.ArrayList;
import java.util.List;

/**
 * The pure half of the Great-Sage text block: greedy word wrapping to a pixel width (a {@link Metrics} stands in for the font) and the
 * typewriter bookkeeping (how many characters of each wrapped line are visible after N characters were typed).
 */
public final class SageLayout {
    private SageLayout() {}

    /** The width in pixels of a string at scale 1. */
    public interface Metrics { int width(String s); }

    /** Wraps to 'maxWidth'; a word wider than the line is cut; "\n" forces a break; always at least one line. */
    public static String[] wrap(String text, Metrics m, int maxWidth) {
        List<String> out = new ArrayList<>();
        int max = Math.max(8, maxWidth);
        for (String para : (text == null ? "" : text).split("\n", -1)) {
            if (para.isBlank()) { out.add(""); continue; }
            StringBuilder line = new StringBuilder();
            for (String word : para.trim().split("\\s+")) {
                String w = word;
                while (m.width(w) > max && w.length() > 1) {
                    int cut = w.length() - 1;
                    while (cut > 1 && m.width(w.substring(0, cut)) > max) cut--;
                    if (line.length() > 0) { out.add(line.toString()); line.setLength(0); }
                    out.add(w.substring(0, cut));
                    w = w.substring(cut);
                }
                if (line.length() == 0) line.append(w);
                else if (m.width(line + " " + w) <= max) line.append(' ').append(w);
                else { out.add(line.toString()); line.setLength(0); line.append(w); }
            }
            if (line.length() > 0) out.add(line.toString());
        }
        if (out.isEmpty()) out.add("");
        return out.toArray(new String[0]);
    }

    /** All characters of all lines (what the typewriter has to type). */
    public static int totalChars(String[] lines) {
        int n = 0;
        for (String l : lines) n += l.length();
        return n;
    }

    /** How many characters of line 'index' are visible once 'typed' characters of the whole block have been typed. */
    public static int visibleInLine(String[] lines, int index, int typed) {
        int before = 0;
        for (int i = 0; i < index && i < lines.length; i++) before += lines[i].length();
        int len = index < lines.length ? lines[index].length() : 0;
        return Math.max(0, Math.min(len, typed - before));
    }

    /** The line the cursor sits on (the first line not fully typed; the last line when all is typed). */
    public static int cursorLine(String[] lines, int typed) {
        int before = 0;
        for (int i = 0; i < lines.length; i++) {
            before += lines[i].length();
            if (typed < before) return i;
        }
        return Math.max(0, lines.length - 1);
    }

    /**
     * The text shortened to at most 'maxLines' wrapped lines: when it is longer, the last allowed line is cut and ends in "...". Returns the
     * text itself when it fits.
     */
    public static String clamp(String text, Metrics m, int width, int maxLines) {
        String t = text == null ? "" : text;
        String[] lines = wrap(t, m, width);
        int max = Math.max(1, maxLines);
        if (lines.length <= max) return t;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < max - 1; i++) b.append(lines[i]).append(' ');
        String last = lines[max - 1];
        while (!last.isEmpty() && m.width(last + "...") > width) last = last.substring(0, last.length() - 1);
        return b.append(last.stripTrailing()).append("...").toString();
    }
}
