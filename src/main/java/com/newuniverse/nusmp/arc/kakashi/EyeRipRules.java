package com.newuniverse.nusmp.arc.kakashi;

/**
 * Why (5.8.0, owner request): the eye-rip mini-game of the Kakashi fight. Every number and decision is here with no Minecraft type, so it is unit tested; {@link KakashiEyeRip} only reads the facts and
 * shows the result. The game: when Kakashi is stunned a player runs up and right-clicks him; a marker sweeps back and forth over a bar of {@link #WIDTH} cells with a zone of {@link #ZONE} cells; a right
 * click while the marker is in the zone is a clean pull, a click outside it is a bad pull (pain, and the pulls start again). Enough clean pulls rip the eye out. The marker is a pure function of the ticks
 * since the player began, so the server keeps no per-tick state for it.
 */
public final class EyeRipRules {
    private EyeRipRules() {}

    /** Kakashi opens his guard at each phase change: three windows a fight. */
    public static final int WINDOWS = 3;
    public static final int WIDTH = 21, ZONE = 6;
    /** Ticks for the marker to go across and back. */
    public static final int PERIOD = 48;
    /** The least ticks between two clicks of one player (a spammed click is ignored, not punished). */
    public static final int CLICK_GAP = 6;
    /** How near a player must stand (blocks) to grip the eye. */
    public static final double REACH = 4.0;

    /** True for the phases that open an eye window: phase 1, 2 and 3 (the first phase is the start of the fight). */
    public static boolean windowFor(int phaseEntered) { return phaseEntered >= 1 && phaseEntered <= WINDOWS; }

    /** The cell of the marker {@code ticks} after the player began: 0 to {@link #WIDTH} - 1 and back again. */
    public static int marker(long ticks) {
        long t = Math.floorMod(Math.max(0L, ticks), (long) PERIOD);
        int half = PERIOD / 2;
        long pos = t <= half ? t : PERIOD - t;
        return (int) Math.round(pos * (WIDTH - 1) / (double) half);
    }

    private static long mix(long x) {
        x += 0x9E3779B97F4A7C15L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }

    /** The first cell of the zone for this seed and this pull (a different place each time, the same for a given seed). */
    public static int zoneStart(long seed, int pull) { return (int) Math.floorMod(mix(seed * 31L + pull), (long) (WIDTH - ZONE + 1)); }

    public static boolean inZone(int marker, int zoneStart) { return marker >= zoneStart && marker < zoneStart + ZONE; }

    /** The result of one click: the new count of clean pulls, whether it was clean, and whether the eye is out. */
    public record Pull(int progress, boolean clean, boolean ripped) {}

    public static Pull pull(int marker, int zoneStart, int progress, int need) {
        if (!inZone(marker, zoneStart)) return new Pull(0, false, false);
        int p = progress + 1;
        return new Pull(p, true, p >= Math.max(1, need));
    }

    /** One click is accepted only after the gap and only while the window is open. */
    public static boolean clickAllowed(long now, long lastClick) { return lastClick <= 0 || now - lastClick >= CLICK_GAP; }

    /** Who may begin: the setting on, a window open, a player who does not carry the eye yet, and a free place at the table. */
    public static boolean canBegin(boolean enabled, boolean windowOpen, boolean hasSkill, int sessions, int cap, boolean already) {
        return enabled && windowOpen && !hasSkill && (already || sessions < Math.max(1, cap));
    }

    /** The bar the player sees: '=' the zone, '|' the marker, '-' the rest. */
    public static String bar(int marker, int zoneStart) {
        StringBuilder sb = new StringBuilder(WIDTH);
        for (int i = 0; i < WIDTH; i++) sb.append(i == marker ? '|' : inZone(i, zoneStart) ? '=' : '-');
        return sb.toString();
    }
}
