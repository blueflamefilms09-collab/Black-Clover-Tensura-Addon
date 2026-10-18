package com.newuniverse.nusmp.labyrinth.mega.mechanics;

/**
 * Why: every number of a mechanic scales with depth, and the scaling must be the same in the Director, the generator's signature guard and the tests. The dungeon has five
 * tiers of 200 floors ({@code DndRules.tierOf}); a parameter is written in the data as {@code [a, b]} and means {@code a + b * s}, where {@code s} runs smoothly from 0 at floor 1
 * to just under 5 at floor 1000 (so a number grows inside a tier too, not in five visible steps). Also here: the numeric BUCKET used by the behaviour signature (a ladder of
 * thresholds roughly 1.5 apart, so 12 and 14 are "the same number" but 2 and 3 are not) and the config difficulty multiplier. Pure.
 */
public final class Scaling {
    private Scaling() {}

    public static final int FLOORS_PER_TIER = 200, TIERS = 5, MAX_FLOOR = 1000;

    /** The tier 1..5 of a (signed) floor; floor 0 and anything unknown is tier 1. */
    public static int tierOf(int floor) { return Math.min(TIERS, (Math.max(1, Math.abs(floor)) - 1) / FLOORS_PER_TIER + 1); }

    /** The smooth depth coordinate: 0 at floor 1, 4.995 at floor 1000, clamped. */
    public static double s(int floor) {
        int f = Math.min(MAX_FLOOR, Math.max(1, Math.abs(floor)));
        return (f - 1) / (double) FLOORS_PER_TIER;
    }

    /** {@code a + b * s(floor)}. */
    public static double value(double a, double b, int floor) { return a + b * s(floor); }

    private static final double[] LADDER = {0.0, 0.25, 0.5, 1, 1.5, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64, 96, 128, 192, 256, 384, 512, 768, 1024, 2048, 4096};

    /** The bucket of a number for the behaviour signature: the number of ladder steps it has reached, signed. 0 is bucket 0; tiny and huge values saturate. */
    public static int bucket(double x) {
        double m = Math.abs(x);
        int n = 0;
        for (double t : LADDER) if (m > t) n++; else break;
        return x < 0 ? -n : n;
    }

    /** The difficulty multiplier from the config, kept in the range the engine supports. */
    public static double clampDifficulty(double d) { return Math.max(0.25, Math.min(4.0, d)); }

    /** An amount (damage, a count) scaled by the difficulty multiplier, never below {@code min} when the base is positive. */
    public static double byDifficulty(double amount, double difficulty, double min) {
        if (amount <= 0) return amount;
        return Math.max(min, amount * clampDifficulty(difficulty));
    }
}
