package com.newuniverse.nusmp.client.mega.system;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Why: a quest title is data (a datapack may write any sentence), and the window has a fixed width, so the title has to be cut into lines that fit, on spaces
 * where possible, never overflowing and never taking more lines than the window has room for. The game's own splitter works on formatted components and needs the
 * client font, which would tie this to the game; this is the same greedy word wrap over a plain width function (the font's width in the game, a fixed number in the
 * tests and in the preview script), so every rule (long words, a text that is too long, an empty text) is pinned by a unit test. A word wider than a whole line is cut
 * between characters; text that does not fit in {@code maxLines} ends with "..." on the last line. Pure.
 */
public final class TextWrap {
    private TextWrap() {}

    /**
     * Cuts {@code text} into lines no wider than {@code maxWidth}.
     *
     * @param text     the text; runs of spaces count as one and the ends are trimmed
     * @param maxWidth the widest a line may be
     * @param width    the width of a string
     * @param maxLines the most lines to return (at least 1)
     * @return the lines, at most {@code maxLines}; empty for a blank text
     */
    public static List<String> wrap(String text, int maxWidth, ToIntFunction<String> width, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null) return lines;
        int limit = Math.max(1, maxLines);
        int room = Math.max(1, maxWidth);
        String line = "";
        for (String word : text.trim().split("\\s+")) {
            if (word.isEmpty()) continue;
            String joined = line.isEmpty() ? word : line + " " + word;
            if (width.applyAsInt(joined) <= room) {
                line = joined;
                continue;
            }
            if (!line.isEmpty()) lines.add(line);
            line = "";
            if (width.applyAsInt(word) <= room) {
                line = word;
                continue;
            }
            StringBuilder piece = new StringBuilder();                       // a word wider than a line is cut between characters
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if (piece.length() > 0 && width.applyAsInt(piece.toString() + c) > room) {
                    lines.add(piece.toString());
                    piece.setLength(0);
                }
                piece.append(c);
            }
            line = piece.toString();
        }
        if (!line.isEmpty()) lines.add(line);
        if (lines.size() <= limit) return lines;
        List<String> kept = new ArrayList<>(lines.subList(0, limit));
        kept.set(limit - 1, ellipsis(kept.get(limit - 1), room, width));
        return kept;
    }

    /** The line with its end replaced by "..." so that it still fits. */
    static String ellipsis(String line, int maxWidth, ToIntFunction<String> width) {
        String s = line;
        while (!s.isEmpty() && width.applyAsInt(s + "...") > maxWidth) s = s.substring(0, s.length() - 1);
        return s.stripTrailing() + "...";
    }
}
