package com.newuniverse.nusmp.arc.danmachi;

/**
 * Why: a monster's telegraph must lock its aim ONCE, on the first call of the wind-up, and every later call must only refresh what is drawn, or the shape would follow the
 * target and a foe could never step out of it. The framework calls a move's telegraph every third tick of the wind-up and says only how many ticks are left, so "is this the
 * first call" is arithmetic on those two numbers; it is pure here so a test can walk every wind-up length the tables use and prove that exactly one call is the first.
 */
public final class TellClock {
    private TellClock() {}

    /** Ticks between two calls of a move's telegraph (the framework's rule: whenever the ticks left are a multiple of three). */
    public static final int STEP = 3;

    /** Is the call with {@code left} ticks of a {@code total}-tick wind-up the first one? (The first call is within the first {@link #STEP} ticks.) */
    public static boolean first(int left, int total) { return left >= total - STEP; }

    /** The ticks-left values at which the framework calls the telegraph of a wind-up of {@code total} ticks: the multiples of three below the total, down to three. */
    public static int[] calls(int total) {
        int n = Math.max(0, (total - 1) / STEP);
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = ((total - 1) / STEP - i) * STEP;
        return out;
    }
}
