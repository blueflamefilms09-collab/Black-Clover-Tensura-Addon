package com.newuniverse.nusmp.companion.talk;

/**
 * Why: the limits of speech bubbles in one pure class. A bubble is a short line over a head: at most {@link #MAX_CHARS} characters, shown for {@link #lifeTicks} ticks, sent only to the players tracking
 * the speaker, and never more often than every {@link #MIN_GAP} ticks per speaker and {@link #GLOBAL_PER_TICK} per tick on the whole server. The client keeps at most {@link #MAX_SHOWN} at a time
 * and draws only those within {@link #DRAW_RANGE} blocks.
 */
public final class BubbleRules {
    private BubbleRules() {}

    public static final int MAX_CHARS = 140, MIN_GAP = 40, GLOBAL_PER_TICK = 8, MAX_SHOWN = 24;
    public static final double DRAW_RANGE = 24.0;
    public static final int MIN_LIFE = 80, MAX_LIFE = 240;

    /** How long a line of this length stays up: time to read it, between 4 and 12 seconds. */
    public static int lifeTicks(int chars) { return Math.max(MIN_LIFE, Math.min(MAX_LIFE, 60 + 3 * Math.max(0, chars))); }

    /** The line cut to {@link #MAX_CHARS} (with an ellipsis); never null. */
    public static String cut(String s) {
        if (s == null) return "";
        String t = s.strip();
        return t.length() <= MAX_CHARS ? t : t.substring(0, MAX_CHARS - 1) + "…";
    }

    /** May this speaker say something now (its last bubble was at {@code last}, now is {@code now})? */
    public static boolean mayFor(long last, long now) { return now < last || now - last >= MIN_GAP; }

    public static boolean inDrawRange(double distanceSqr) { return distanceSqr <= DRAW_RANGE * DRAW_RANGE; }
}
