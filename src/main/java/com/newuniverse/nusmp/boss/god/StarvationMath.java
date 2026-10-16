package com.newuniverse.nusmp.boss.god;

/**
 * Why: Magicule Starvation must make ultimate-skill spam expensive without ever being a way to kill a player: it only ever lowers the
 * magicule pool (the pool the Black Clover mana and Tensura magicules share), never touches health, and stops at a floor that is above
 * the mana law's black-out line (0.5% of the pool), so the drain alone can never trigger the exhaustion shock or collapse that
 * {@code ManaLaw.exhaust} applies to a player who <i>spends</i> the last of their mana. A player's own casts can still take the pool lower
 * than the floor; the arena never does. Pure arithmetic, unit tested.
 */
public final class StarvationMath {
    private StarvationMath() {}

    /** The pool is never drained below this share of its maximum (1%): above the 0.5% black-out line of the mana law. */
    public static final double FLOOR_FRACTION = 0.01;
    public static final int TICKS_PER_SECOND = 20;

    /** The amount drained each tick for 'pctPerSecond' percent of the maximum per second (0 for a non-positive percent). */
    public static double perTick(double max, double pctPerSecond) {
        if (!(max > 0.0) || !(pctPerSecond > 0.0)) return 0.0;
        return max * pctPerSecond / 100.0 / TICKS_PER_SECOND;
    }

    /** The amount one pulse drains: 'pulsePct' percent of the maximum. */
    public static double pulse(double max, double pulsePct) {
        if (!(max > 0.0) || !(pulsePct > 0.0)) return 0.0;
        return max * pulsePct / 100.0;
    }

    /**
     * The pool after a drain of 'amount': lower by that much, but not under the floor, and never higher than it was (a pool already
     * under the floor is left as it is, it is not raised).
     */
    public static double apply(double current, double max, double amount) {
        if (!(max > 0.0) || !(amount > 0.0) || Double.isNaN(current)) return current;
        double floor = max * FLOOR_FRACTION;
        if (current <= floor) return current;
        return Math.max(floor, current - amount);
    }
}
