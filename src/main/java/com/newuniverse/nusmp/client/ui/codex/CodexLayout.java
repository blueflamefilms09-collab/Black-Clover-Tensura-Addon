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
}
