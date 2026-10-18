package com.newuniverse.nusmp.elite.forge.rules;

/**
 * Why: the Hammer Timing minigame, a timing bar. A cursor sweeps back and forth along a bar; for each strike a target zone is shown at one place on the bar and the
 * player clicks when the cursor is in it. All of it is a function of the seed: the cursor is a triangle wave of time with a seeded phase, and the target of strike
 * {@code i} is a seeded place in 20%..80% of the bar. The input is the list of strike times in milliseconds since the step started. Rules (this mod's own design):
 * {@code 3 + difficulty} strikes; a full sweep takes {@code 1800 - 200 x difficulty} ms; the zone's half width is {@code 0.14 - 0.015 x difficulty} of the bar; a strike
 * in the centre quarter of the zone scores 1, at the zone's edge 0.6, at twice the half width 0, in between linearly. A strike cannot come before 250 ms or less than
 * 180 ms after the one before (an earlier one is rejected: no hand does that), and every strike must fall inside the step's time limit. The time the input claims is
 * the time of the last strike. No Minecraft classes.
 */
public final class HammerTiming implements Minigame {
    public static final int FIRST_MS = 250, GAP_MS = 180;

    @Override public MinigameType type() { return MinigameType.HAMMER_TIMING; }

    /** Number of strikes. */
    public static int strikes(int d) { return 3 + Minigame.clampDifficulty(d); }

    /** Milliseconds for the cursor to go across and back. */
    public static int periodMillis(int d) { return 1800 - 200 * Minigame.clampDifficulty(d); }

    /** Half width of the target zone as a fraction of the bar. */
    public static double halfWidth(int d) { return 0.14 - 0.015 * Minigame.clampDifficulty(d); }

    /** The cursor position 0..1 at {@code tMillis} after the step started. */
    public static double cursor(long seed, int d, double tMillis) {
        double x = tMillis / periodMillis(d) + ForgeRng.unit(seed, 1, 0);
        x -= Math.floor(x);
        return x < 0.5 ? 2.0 * x : 2.0 - 2.0 * x;
    }

    /** The centre of the target zone of strike {@code i}, 0.2..0.8. */
    public static double target(long seed, int i) { return 0.2 + 0.6 * ForgeRng.unit(seed, 2, i); }

    /** How well one strike did, 0..1. */
    public static double accuracy(double distance, double halfWidth) {
        double a = Math.abs(distance);
        if (a <= 0.25 * halfWidth) return 1.0;
        if (a <= halfWidth) return 1.0 - 0.4 * (a - 0.25 * halfWidth) / (0.75 * halfWidth);
        if (a <= 2.0 * halfWidth) return 0.6 * (1.0 - (a - halfWidth) / halfWidth);
        return 0.0;
    }

    @Override public int limitMillis(int d) { return strikes(d) * 3000 + 4000; }

    @Override public int maxInputLength(int d) { return strikes(d); }

    @Override
    public String validate(long seed, int d, int[] input) {
        if (input == null) return "no input";
        if (input.length > strikes(d)) return "too many strikes";
        int prev = Integer.MIN_VALUE;
        for (int i = 0; i < input.length; i++) {
            int t = input[i];
            if (t < FIRST_MS) return "a strike before " + FIRST_MS + " ms";
            if (t > limitMillis(d)) return "a strike after the time limit";
            if (i > 0 && t - prev < GAP_MS) return "strikes closer than " + GAP_MS + " ms";
            prev = t;
        }
        return null;
    }

    @Override
    public int score(long seed, int d, int[] input) {
        if (validate(seed, d, input) != null) return 0;
        double sum = 0.0, w = halfWidth(d);
        for (int i = 0; i < input.length; i++) sum += accuracy(cursor(seed, d, input[i]) - target(seed, i), w);
        return (int) Math.round(Minigame.MAX_SCORE * sum / strikes(d));
    }

    @Override
    public long claimedMillis(long seed, int d, int[] input) { return input == null || input.length == 0 ? 0L : Math.max(0, input[input.length - 1]); }

    @Override
    public int[] perfectInput(long seed, int d) {
        int n = strikes(d);
        int[] out = new int[n];
        double w = halfWidth(d);
        for (int i = 0; i < n; i++) {
            int t = i == 0 ? FIRST_MS : out[i - 1] + GAP_MS;
            while (t < limitMillis(d) && Math.abs(cursor(seed, d, t) - target(seed, i)) > 0.1 * w) t++;
            out[i] = t;
        }
        return out;
    }
}
