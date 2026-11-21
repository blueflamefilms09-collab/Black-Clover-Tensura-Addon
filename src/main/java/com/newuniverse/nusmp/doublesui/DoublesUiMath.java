package com.newuniverse.nusmp.doublesui;

/**
 * Why: the Real Double HUD (docs/doubles_ui.md) has two jobs that are plain arithmetic, so they live here without any Minecraft class and are tested on their own: which double is
 * selected after a key press (the cycle wraps, and one extra stop means "none chosen: trade with the nearest", the old behaviour) and where each card of the stack goes so that two
 * cards never overlap and more than the shown number collapse into one "+N more" line. Used by the server (the cycle) and by the client overlay (the layout). All sizes are GUI pixels.
 */
public final class DoublesUiMath {
    private DoublesUiMath() {}

    /** The most doubles one player can have in the roster (hard cap of the packet and of the screen). */
    public static final int MAX_ROWS = 16;
    public static final int W = 116, HEADER_H = 10, ACTIVE_H = 28, ROW_H = 16, GAP = 2, MORE_H = 10;

    /** No explicit choice: a sneak-cast trades with the nearest double, as it always did. */
    public static final int AUTO = -1;

    /**
     * The next selection when the key is pressed. {@code cur} is {@link #AUTO} or an index below {@code n}; the stops are AUTO, 0, 1 ... n-1 and then AUTO again (so the
     * cycle can always return to the old behaviour). {@code dir} is +1 or -1. With nothing to choose from the answer is AUTO.
     */
    public static int step(int cur, int dir, int n) {
        if (n <= 0) return AUTO;
        int stops = n + 1;
        int at = (cur < 0 || cur >= n) ? 0 : cur + 1;
        int next = Math.floorMod(at + (dir >= 0 ? 1 : -1), stops);
        return next - 1;
    }

    /** How many cards are drawn out of {@code n} when at most {@code maxShown} fit (at least one when there is any). */
    public static int shown(int n, int maxShown) {
        if (n <= 0) return 0;
        return Math.min(Math.min(n, MAX_ROWS), Math.max(1, maxShown));
    }

    /** How many cards fold into the "+N more" line. */
    public static int hidden(int n, int maxShown) {
        return Math.max(0, Math.min(n, MAX_ROWS) - shown(n, maxShown));
    }

    /**
     * Which double the {@code slot}-th card shows: with a selection (index {@code active}) slot 0 is that double and the others follow in their own order; with none (AUTO) the
     * slots are the doubles in their own order.
     */
    public static int source(int slot, int active, int n) {
        if (active < 0 || active >= n) return slot;
        if (slot == 0) return active;
        return slot <= active ? slot - 1 : slot;
    }

    /** Height of the card in {@code slot}: the selected one is taller. */
    public static int cardHeight(int slot, boolean hasActive) { return hasActive && slot == 0 ? ACTIVE_H : ROW_H; }

    /** Top of the card in {@code slot}, below the header; slot {@code shown} is where the "+N more" line starts. */
    public static int cardTop(int slot, boolean hasActive) {
        int y = HEADER_H + GAP;
        for (int i = 0; i < slot; i++) y += cardHeight(i, hasActive) + GAP;
        return y;
    }

    /** Height of the whole stack: header, the shown cards and the "+N more" line when there is one. */
    public static int totalHeight(int n, int maxShown, boolean hasActive) {
        int s = shown(n, maxShown);
        if (s == 0) return 0;
        int bottom = cardTop(s, hasActive) - GAP;
        return hidden(n, maxShown) > 0 ? bottom + GAP + MORE_H : bottom;
    }

    /** Fill of a bar from 0 to 1; a NaN, negative or missing maximum reads as empty. */
    public static float frac(float v, float max) {
        if (!(max > 0f) || Float.isNaN(v)) return 0f;
        return Math.max(0f, Math.min(1f, v / max));
    }

    /** Ticks left on a double now, counted down from the value the server sent at {@code syncTick}. */
    public static int lifeLeft(int atSync, long syncTick, long nowTick) {
        long gone = Math.max(0L, nowTick - syncTick);
        return (int) Math.max(0L, atSync - gone);
    }
}
