package com.newuniverse.nusmp.relics.wave;

import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

/**
 * Wave 6: the charge pools of the armours and robes ("10 charges, regains 1d8+2 at dawn", "3 uses a day"), as pure maths. A pool is a charge count and the
 * day number it was last refilled on; the Minecraft glue ({@link Wave6Store}) keeps it on the relic stack. Dice are passed in as
 * {@code roll(sides) -> 0..sides-1} so the maths can be tested with a fixed roller.
 */
public final class Wave6Pool {
    private Wave6Pool() {}

    /** At most this many missed dawns are paid out at once (a relic left in a chest for a month is just full). */
    public static final int MAX_CATCHUP_DAYS = 4;

    /** The pool: charges now, and the day number of the last dawn refill. */
    public record State(int charges, long day) {
        public State { if (charges < 0) charges = 0; }

        public static State fresh(int max, long today) { return new State(max, today); }
    }

    /** The dawn refill: each missed dawn adds {@code gain} charges (rolled each time), never above {@code max}; a clock that runs backwards changes nothing. */
    public static State rollDay(State s, long today, int max, IntSupplier gain) {
        if (today <= s.day()) return s;
        long days = Math.min(MAX_CATCHUP_DAYS, today - s.day());
        int c = Math.min(max, s.charges());
        for (long i = 0; i < days && c < max; i++) c = Math.min(max, c + Math.max(0, gain.getAsInt()));
        return new State(c, today);
    }

    public static boolean canSpend(State s, int n) { return n > 0 && s.charges() >= n; }

    /** Spends {@code n} charges, or returns the pool unchanged when it cannot pay. */
    public static State spend(State s, int n) { return canSpend(s, n) ? new State(s.charges() - n, s.day()) : s; }

    public static State add(State s, int n, int max) { return new State(Math.min(max, s.charges() + Math.max(0, n)), s.day()); }

    /** {@code count} dice of {@code sides} sides; {@code roll(sides)} answers 0..sides-1 like RandomSource.nextInt. */
    public static int dice(int count, int sides, IntUnaryOperator roll) {
        int t = 0;
        for (int i = 0; i < count; i++) t += 1 + Math.max(0, Math.min(sides - 1, roll.applyAsInt(sides)));
        return t;
    }
}
