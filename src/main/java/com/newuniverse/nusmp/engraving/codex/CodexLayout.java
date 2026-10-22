package com.newuniverse.nusmp.engraving.codex;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Why: the Engravement Codex screen lays out every entry once (when it opens or is resized) into lines of text with their heights; the pages are then only a matter of adding heights
 * ({@link CodexFilter#paginate}). The word wrap and the heights live here, pure and unit tested, with the pixel width of a string given as a function (the game's font in the screen, a
 * stand-in font in the headless preview), so the screen draws ready-made strings and never splits text while drawing. No Minecraft types.
 */
public final class CodexLayout {
    private CodexLayout() {}

    /** Height of a line of text and the vertical gaps, in pixels (the game's font is 9 pixels tall). */
    public static final int LINE = 10, NAME_H = 11, GAP_AFTER_SCOPE = 4, GAP_BLOCK = 3;

    /** The most lines each block of an entry may take (a longer text is cut with "..."). */
    public static final int MAX_TEXT = 10, MAX_FACTS = 9, MAX_FOUND = 3;

    /** The words the layout needs that are translated: the "Found on" label, the "+%d more" form and the "none yet" phrase (null = English). */
    public record Words(String foundLabel, String more, String none, String ellipsis) {
        public static final Words ENGLISH = new Words(null, null, null, "...");
    }

    /**
     * One laid-out entry. {@code name} is the heading, {@code groupLine} the source (franchise / mod / Tensura); {@code scopeInline} is the plain-words scope drawn on the same line, {@code scopeX}
     * pixels to the right of the source, when both fit one line (then {@code scope} is empty), else {@code scopeInline} is empty and {@code scope} holds the scope's own lines. {@code text}
     * is the effect text, {@code facts} the bullet lines (cooldowns, caps) with {@code factCap} saying for each line whether it belongs to a cap line (drawn green), {@code found} the
     * "Found on" lines, {@code colour} the source's colour (RGB). {@code height} is the total in pixels; {@code cut} says a block was shortened to fit a page. Everything is ready to draw.
     */
    public record Laid(int entry, String name, String groupLine, String scopeInline, int scopeX, int colour, List<String> scope, List<String> text, List<String> facts, boolean[] factCap,
                       List<String> found, int height, boolean cut) {}

    /**
     * The size of the entry text for a page {@code pageHeight} pixels tall: full size on a tall page, a little smaller on a shorter one (a small window, a big GUI scale), so that about two
     * entries fit a page. The screen lays the entries out in the scaled space ({@code width / scale}, {@code pageHeight / scale}) and draws them with that scale.
     */
    public static float textScale(int pageHeight) {
        if (pageHeight >= 300) return 1.0f;
        if (pageHeight >= 200) return 0.85f;
        return 0.75f;
    }

    /** The longest prefix of {@code s} that is at most {@code width} pixels wide. */
    public static String fit(String s, int width, ToIntFunction<String> measure) {
        String out = s == null ? "" : s;
        while (out.length() > 1 && measure.applyAsInt(out) > width) out = out.substring(0, out.length() - 1);
        return out;
    }

    private static boolean[] capFlags(List<String> bulletLines) {
        boolean[] out = new boolean[bulletLines.size()];
        boolean cap = false;
        for (int i = 0; i < out.length; i++) {
            String s = bulletLines.get(i);
            if (s.startsWith("- ")) cap = CodexFacts.isCap(s.substring(2));                   // a continuation line keeps the colour of its fact
            out[i] = cap;
        }
        return out;
    }

    /** Greedy word wrap of {@code s} into lines no wider than {@code width} pixels; a word wider than the line is broken at characters. Always at least one line (empty text gives one empty line). */
    public static List<String> wrap(String s, int width, ToIntFunction<String> measure) {
        List<String> out = new ArrayList<>();
        if (s == null) s = "";
        int max = Math.max(8, width);
        StringBuilder line = new StringBuilder();
        for (String word : s.split("\\s+")) {
            if (word.isEmpty()) continue;
            String tryLine = line.length() == 0 ? word : line + " " + word;
            if (measure.applyAsInt(tryLine) <= max) {
                line.setLength(0);
                line.append(tryLine);
                continue;
            }
            if (line.length() > 0) {
                out.add(line.toString());
                line.setLength(0);
            }
            String rest = word;
            while (measure.applyAsInt(rest) > max && rest.length() > 1) {            // a word wider than the line
                int cut = rest.length() - 1;
                while (cut > 1 && measure.applyAsInt(rest.substring(0, cut)) > max) cut--;
                out.add(rest.substring(0, cut));
                rest = rest.substring(cut);
            }
            line.append(rest);
        }
        if (line.length() > 0 || out.isEmpty()) out.add(line.toString());
        return out;
    }

    /** Bullet lines: "- " in front of the first line of each fact and two spaces in front of its continuation lines. */
    private static List<String> bullets(List<String> facts, int width, ToIntFunction<String> measure) {
        int bullet = Math.max(measure.applyAsInt("- "), measure.applyAsInt("  "));
        List<String> out = new ArrayList<>();
        for (String f : facts) {
            boolean first = true;
            for (String l : wrap(f, width - bullet, measure)) {
                out.add((first ? "- " : "  ") + l);
                first = false;
            }
        }
        return out;
    }

    /**
     * The first {@code max} lines; when lines were cut the last kept line ends with the ellipsis, shortened so that it still fits {@code width} (nothing is kept for max 0).
     */
    private static List<String> limit(List<String> lines, int max, String ellipsis, int width, ToIntFunction<String> measure) {
        if (lines.size() <= max) return lines;
        if (max <= 0) return List.of();
        List<String> out = new ArrayList<>(lines.subList(0, max));
        String last = out.get(max - 1);
        while (last.length() > 1 && measure.applyAsInt(last + ellipsis) > width) last = last.substring(0, last.length() - 1);
        out.set(max - 1, last + ellipsis);
        return out;
    }

    /** The total height of the blocks, in pixels. */
    public static int heightOf(int scope, int text, int facts, int found) {
        int h = NAME_H + LINE + scope * LINE + GAP_AFTER_SCOPE + text * LINE;
        if (facts > 0) h += GAP_BLOCK + facts * LINE;
        if (found > 0) h += GAP_BLOCK + found * LINE;
        return h;
    }

    /**
     * Lays out one entry for a page {@code width} pixels wide and {@code pageHeight} pixels tall. The blocks are limited to {@link #MAX_TEXT} / {@link #MAX_FACTS} / {@link #MAX_FOUND} lines, then
     * facts, found-on lines and text lines are cut (in that order, never below one line of each) until the entry fits the page. {@code foundMax} is how many item names the "Found on" line lists.
     */
    public static Laid lay(int index, CodexEntries.Entry e, int width, int pageHeight, int foundMax, Words words, ToIntFunction<String> measure) {
        String ell = words == null || words.ellipsis() == null ? "..." : words.ellipsis();
        Words w = words == null ? Words.ENGLISH : words;
        String group = fit(e.groupName(), width, measure);
        List<String> scope = wrap(e.scope(), width, measure);
        String inline = "";
        int scopeX = 0;
        int sep = measure.applyAsInt("  -  ");
        if (scope.size() == 1 && measure.applyAsInt(group) + sep + measure.applyAsInt(scope.get(0)) <= width) {            // the source and the scope share one line
            inline = scope.get(0);
            scopeX = measure.applyAsInt(group) + sep;
            scope = List.of();
        }
        List<String> text = wrap(e.text(), width, measure);
        List<String> facts = bullets(e.facts(), width, measure);
        int nt = Math.min(text.size(), MAX_TEXT), nf = Math.min(facts.size(), MAX_FACTS), nj = MAX_FOUND;
        List<String> found = foundLines(e.foundOn(), foundMax, nj, w, width, measure);
        boolean cut = nt < text.size() || nf < facts.size();
        while (heightOf(scope.size(), nt, nf, found.size()) > pageHeight && nf > 0) { nf--; cut = true; }            // facts first: the numbers are also in the text
        while (heightOf(scope.size(), nt, nf, found.size()) > pageHeight && nj > 2) {                                  // then fewer names (two lines stay, so the "+M more" always stays)
            nj--;
            found = foundLines(e.foundOn(), foundMax, nj, w, width, measure);
            cut = true;
        }
        while (heightOf(scope.size(), nt, nf, found.size()) > pageHeight && nt > 2) { nt--; cut = true; }
        List<String> outText = limit(text, nt, ell, width, measure), outFacts = limit(facts, nf, ell, width, measure);
        return new Laid(index, fit(e.name(), width, measure), inline.isEmpty() ? group : group + "  -  ", inline, scopeX, e.colour(), scope, outText, outFacts, capFlags(outFacts), found,
                heightOf(scope.size(), outText.size(), outFacts.size(), found.size()), cut);
    }

    /**
     * The "Found on" lines: as many names as fit in {@code maxLines} lines (at most {@code foundMax}), so that the "+M more" at the end is never cut off. If even one name does not fit the
     * first {@code maxLines} lines of it are kept.
     */
    private static List<String> foundLines(List<String> names, int foundMax, int maxLines, Words w, int width, ToIntFunction<String> measure) {
        List<String> lines = List.of();
        for (int shown = Math.max(1, Math.min(foundMax, names.size())); shown >= 1; shown--) {
            lines = wrap(CodexEntries.foundOnLine(names, shown, w.foundLabel(), w.more(), w.none()), width, measure);
            if (lines.size() <= maxLines) return lines;
        }
        return lines.size() <= maxLines + 1 ? lines : new ArrayList<>(lines.subList(0, Math.max(1, maxLines)));    // one extra line rather than losing the "+M more"
    }
}
