package com.newuniverse.nusmp.engraving.codex;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: the Engravement Codex screen filters its entries by group and by the search box, and packs them into pages. Both run only when the group or the text CHANGES (never per frame),
 * and both live here, pure and unit tested, so the screen only draws. No Minecraft types.
 */
public final class CodexFilter {
    private CodexFilter() {}

    /** The longest query that is looked at (a pasted wall of text is cut). */
    public static final int MAX_QUERY = 64;

    /** The words of a query: lower case, split on spaces, at most {@link #MAX_QUERY} characters in all, no empty words. */
    public static String[] words(String query) {
        if (query == null) return new String[0];
        String q = query.strip();
        if (q.length() > MAX_QUERY) q = q.substring(0, MAX_QUERY);
        q = q.toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return new String[0];
        List<String> out = new ArrayList<>();
        for (String w : q.split("\\s+")) if (!w.isEmpty()) out.add(w);
        return out.toArray(new String[0]);
    }

    /**
     * The indexes (into {@code entries}) of the entries to show: those of the group ({@code groupId} null or empty = every group) whose search text holds every word of the query.
     * Entries whose NAME holds all the words come first; both halves keep the book's order.
     */
    public static int[] select(List<CodexEntries.Entry> entries, String groupId, String query) {
        String[] words = words(query);
        boolean anyGroup = groupId == null || groupId.isEmpty();
        int[] byName = new int[entries.size()], rest = new int[entries.size()];
        int nn = 0, nr = 0;
        for (int i = 0; i < entries.size(); i++) {
            CodexEntries.Entry e = entries.get(i);
            if (!anyGroup && !groupId.equals(e.groupId())) continue;
            if (words.length == 0) { rest[nr++] = i; continue; }
            if (!containsAll(e.search(), words)) continue;
            if (containsAll(e.name().toLowerCase(Locale.ROOT), words)) byName[nn++] = i;
            else rest[nr++] = i;
        }
        int[] out = new int[nn + nr];
        System.arraycopy(byName, 0, out, 0, nn);
        System.arraycopy(rest, 0, out, nn, nr);
        return out;
    }

    private static boolean containsAll(String text, String[] words) {
        for (String w : words) if (!text.contains(w)) return false;
        return true;
    }

    /**
     * Packs items of the given heights into pages of {@code pageHeight} pixels with {@code gap} pixels between items. An item that does not fit goes to the next page; an item taller than a
     * page gets a page of its own (the screen clips it). Returns, for each page, the index of its first item and the count (a page is a run of consecutive items). Always at least one
     * page (an empty one when there are no items).
     */
    public static List<int[]> paginate(int[] heights, int pageHeight, int gap) {
        List<int[]> pages = new ArrayList<>();
        int start = 0, used = 0;
        for (int i = 0; i < heights.length; i++) {
            int h = Math.max(0, heights[i]);
            int add = h + (i == start ? 0 : gap);
            if (i > start && used + add > pageHeight) {
                pages.add(new int[]{start, i - start});
                start = i;
                used = h;
            } else {
                used += add;
            }
        }
        if (start < heights.length || pages.isEmpty()) pages.add(new int[]{start, heights.length - start});
        return pages;
    }

    /** How many two-page spreads {@code pages} pages make (at least one). */
    public static int spreads(int pages) { return Math.max(1, (pages + 1) / 2); }
}
