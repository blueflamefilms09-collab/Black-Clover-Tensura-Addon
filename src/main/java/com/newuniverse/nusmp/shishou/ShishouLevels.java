package com.newuniverse.nusmp.shishou;

/**
 * Why: Shishou grows by eating monsters, so the curve from experience to level is one pure table: tested, capped, and the same on the server and in the docs. Level 1..{@link #MAX_LEVEL}. Experience is a
 * total ({@code long}); {@link #totalFor} is the total at which a level starts. Tier thresholds: 21 and 50 ({@link ShishouSkill#tierOf}). Levels above 50 only add stats, up to the cap.
 */
public final class ShishouLevels {
    private ShishouLevels() {}

    public static final int MAX_LEVEL = 100;

    /** The version of the curve: 1 was the 2.0.0 curve ({@link #legacyToNext}), 2 (2.1.0) is gentler. A saved record carries the version it was written with so the migration only runs once. */
    public static final int CURVE_VERSION = 2;

    /** The experience that takes level {@code level} to the next (0 at the cap): {@code 18 + 3.5 L + 0.2 L^2}. 73 at level 10, 258 at 30, 693 at 50, 2324 at 99 (the 2.0.0 curve needed 1200 at 50 and 4049 at 99). */
    public static long toNext(int level) {
        if (level >= MAX_LEVEL) return 0L;
        int l = Math.max(1, level);
        return Math.round(18.0 + 3.5 * l + 0.2 * l * l);
    }

    /** The 2.0.0 curve (31 at level 1, 285 at 20, 1200 at 50, 4049 at 99): kept only to read old saves, so no saved Shishou ever loses a level. */
    public static long legacyToNext(int level) {
        if (level >= MAX_LEVEL) return 0L;
        int l = Math.max(1, level);
        return Math.round(25.0 + 6.0 * l + 0.35 * l * l);
    }

    /** The level an experience total had on the 2.0.0 curve (1..MAX_LEVEL). */
    public static int legacyLevelFor(long totalExp) {
        long t = Math.max(0L, totalExp);
        int level = 1;
        while (level < MAX_LEVEL && t >= legacyToNext(level)) {
            t -= legacyToNext(level);
            level++;
        }
        return level;
    }

    /** The total experience at which {@code level} starts (level 1 starts at 0). */
    public static long totalFor(int level) {
        long sum = 0L;
        for (int l = 1; l < Math.min(level, MAX_LEVEL); l++) sum += toNext(l);
        return sum;
    }

    /** The level a total experience is worth (1..MAX_LEVEL). */
    public static int levelFor(long totalExp) {
        long t = Math.max(0L, totalExp);
        int level = 1;
        while (level < MAX_LEVEL && t >= toNext(level)) {
            t -= toNext(level);
            level++;
        }
        return level;
    }

    /** The total experience of the cap: nothing above it is kept. */
    public static long capTotal() { return totalFor(MAX_LEVEL); }

    /** The share of the way from the current level to the next, 0..1 (1 at the cap). */
    public static double progress(long totalExp) {
        int level = levelFor(totalExp);
        if (level >= MAX_LEVEL) return 1.0;
        return Math.min(1.0, (double) (Math.max(0L, totalExp) - totalFor(level)) / toNext(level));
    }
}
