package com.newuniverse.nusmp.client.ui.codex;

/**
 * Why: the two-column flow of the generated Codex pages as arithmetic, so it is tested and the same numbers serve {@code contentHeight} and
 * {@code render} (a page whose height and drawing disagree clips its last entry or scrolls into empty paper). Each block goes to the shorter
 * column (ties: the left one), which keeps the two halves of the spread balanced however long an entry is; a block is never split, so a
 * magic's spells and its header stay together. Pure.
 */
public final class CodexLayout {
    private CodexLayout() {}

    /** column[i] is 0 or 1, y[i] the top of block i inside its column, total the height of the taller column. */
    public record Placement(int[] column, int[] y, int total) {}

    public static Placement place(int[] heights, int gap) {
        int n = heights.length;
        int[] col = new int[n], y = new int[n];
        int[] used = {0, 0};
        for (int i = 0; i < n; i++) {
            int c = used[1] < used[0] ? 1 : 0;
            col[i] = c;
            y[i] = used[c];
            used[c] += Math.max(0, heights[i]) + gap;
        }
        return new Placement(col, y, Math.max(used[0], used[1]));
    }

    /** 4.80.0: the table of contents is a two-column list read down the left column, then down the right one. Rows per column for n entries. */
    public static int tocRows(int n) { return (Math.max(0, n) + 1) / 2; }

    /** The entry under a point of the contents list (x, y relative to the list's top-left, below its header), or -1. col1X is where the right column starts, lineH one line's height. */
    public static int tocIndexAt(int n, int x, int y, int col1X, int lineH) {
        if (n <= 0 || x < 0 || y < 0 || lineH <= 0 || col1X <= 0) return -1;
        int rows = tocRows(n);
        int r = y / lineH;
        if (r >= rows) return -1;
        int i = (x >= col1X ? rows : 0) + r;
        return i < n ? i : -1;
    }
}
