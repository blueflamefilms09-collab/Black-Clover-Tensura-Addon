package com.newuniverse.nusmp.relics.tier;

/**
 * The act-power multiplier of the relic act that is running right now. {@code RelicEngine.activate} sets it around the act body, and {@code Fx} damage
 * multiplies its raw amount by it before the balance law. Server thread only (acts run there), and always restored in a finally, so it is 1.0 outside an act;
 * nested acts restore the outer value. The balance law's EP scaling, the 400 raw cap and the per-hit caps on players apply after it, unchanged.
 */
public final class ActPower {
    private ActPower() {}

    private static double current = 1.0;

    /** The multiplier now (1.0 outside an act). */
    public static double current() { return current; }

    /** Sets the multiplier for the act that starts; returns the previous value to hand to {@link #end}. */
    public static double begin(double multiplier) {
        double previous = current;
        current = multiplier > 0 ? multiplier : 1.0;
        return previous;
    }

    public static void end(double previous) { current = previous; }
}
