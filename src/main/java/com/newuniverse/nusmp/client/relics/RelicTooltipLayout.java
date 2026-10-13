package com.newuniverse.nusmp.client.relics;

import java.util.ArrayList;
import java.util.List;

/**
 * The pure half of the relic tooltip: word wrapping and paging, so that a tooltip can never be wider or taller than the room it is given.
 * Text is wrapped to the width; if the wrapped height is too much the text scale shrinks (down to {@code minScale}, 0.75 in the game), and if
 * it still does not fit it is cut into pages (the mouse wheel turns them). No Minecraft types: the font is a {@link Metrics}.
 */
public final class RelicTooltipLayout {
    private RelicTooltipLayout() {}

    /** The width in pixels of a string at scale 1 (the game's font). */
    public interface Metrics { int width(String s); }

    /** One paragraph: its text, a tag the renderer maps to a colour, and an indent in pixels. */
    public record Para(String text, int tag, int indent) {
        public Para(String text, int tag) { this(text, tag, 0); }
    }

    /** One wrapped line. */
    public record Row(String text, int tag, int indent) {
        public boolean blank() { return text.isEmpty(); }
    }

    /**
     * @param scale   the text scale used (1.0 down to the minimum)
     * @param width   the widest row in screen pixels, at that scale (never above the room given)
     * @param rowH    the height of one row in screen pixels, at that scale
     * @param pages   the rows of every page (one page when it all fits)
     * @param pageRows the most rows on any page (the tooltip is that tall; with several pages one more row holds the footer)
     */
    public record Layout(float scale, int width, float rowH, List<List<Row>> pages, int pageRows) {
        public int pageCount() { return pages.size(); }

        /** The tooltip's height in screen pixels. */
        public int height() { return (int) Math.ceil((pageRows + (pageCount() > 1 ? 1 : 0)) * rowH); }
    }

    /** Greedy word wrap to {@code maxWidth}; a word wider than the line is cut; "\n" forces a break. Always at least one row. */
    public static List<String> wrap(String text, Metrics m, int maxWidth) {
        List<String> out = new ArrayList<>();
        int max = Math.max(8, maxWidth);
        for (String para : (text == null ? "" : text).split("\n", -1)) {
            if (para.isBlank()) { out.add(""); continue; }
            StringBuilder line = new StringBuilder();
            for (String word : para.trim().split("\\s+")) {
                String w = word;
                while (m.width(w) > max) {                                             // a single word wider than the line: cut it
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
        return out;
    }

    private static List<Row> rows(List<Para> paras, Metrics m, int wrapWidth) {
        List<Row> rows = new ArrayList<>();
        for (Para p : paras) {
            for (String line : wrap(p.text(), m, Math.max(8, wrapWidth - p.indent()))) rows.add(new Row(line, p.tag(), p.indent()));
        }
        return rows;
    }

    /**
     * Lays the paragraphs out in a box of {@code maxWidth} by {@code maxHeight} screen pixels.
     *
     * @param lineHeight the height of a row at scale 1 (the game's font is 9 plus a pixel of leading)
     * @param minScale   the smallest text scale (0.75 in the game)
     */
    public static Layout layout(List<Para> paras, Metrics m, int maxWidth, int maxHeight, int lineHeight, float minScale) {
        float min = Math.max(0.3f, Math.min(1f, minScale));
        List<Row> rows = null;
        float scale = 1f;
        for (float s = 1f; s >= min - 1e-4f; s -= 0.05f) {
            scale = Math.max(min, s);
            rows = rows(paras, m, (int) Math.floor(maxWidth / scale));
            if (rows.size() * lineHeight * scale <= maxHeight) return single(rows, m, scale, lineHeight);
        }
        scale = min;
        rows = rows(paras, m, (int) Math.floor(maxWidth / scale));
        float rowH = lineHeight * scale;
        int perPage = Math.max(1, (int) Math.floor(maxHeight / rowH) - 1);              // one row is kept for the "Page 1/3" footer
        List<List<Row>> pages = new ArrayList<>();
        int i = 0;
        while (i < rows.size()) {
            while (i < rows.size() && rows.get(i).blank() && !pages.isEmpty()) i++;    // a page does not start with a blank line
            if (i >= rows.size()) break;
            int end = Math.min(rows.size(), i + perPage);
            List<Row> page = new ArrayList<>(rows.subList(i, end));
            while (page.size() > 1 && page.get(page.size() - 1).blank()) page.remove(page.size() - 1);
            pages.add(page);
            i = end;
        }
        if (pages.isEmpty()) pages.add(List.of());
        int most = 0;
        for (List<Row> p : pages) most = Math.max(most, p.size());
        return new Layout(scale, widest(rows, m, scale, maxWidth), rowH, pages, most);
    }

    private static Layout single(List<Row> rows, Metrics m, float scale, int lineHeight) {
        return new Layout(scale, widest(rows, m, scale, Integer.MAX_VALUE), lineHeight * scale, List.of(rows), rows.size());
    }

    private static int widest(List<Row> rows, Metrics m, float scale, int cap) {
        int w = 0;
        for (Row r : rows) w = Math.max(w, r.indent() + m.width(r.text()));
        return Math.min(cap, (int) Math.ceil(w * scale));
    }

    /** The footer of a paged tooltip ("Page 2/4"). */
    public static String footer(int page, int pages) { return "Page " + (page + 1) + "/" + pages; }

    /** A page number kept inside the layout. */
    public static int clampPage(int page, int pages) { return Math.max(0, Math.min(Math.max(0, pages - 1), page)); }
}
