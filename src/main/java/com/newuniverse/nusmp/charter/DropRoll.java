package com.newuniverse.nusmp.charter;

import java.util.function.DoubleSupplier;

/**
 * The decision table of the first-kill drop (pure). The order is the owner's: the world's lock first (a claimed grimoire never rolls again),
 * then whether anyone eligible killed the boss, then the roll. The roll is only drawn when it can matter.
 */
public final class DropRoll {
    private DropRoll() {}

    public enum Result {
        /** The system is switched off in the config: nothing happens and nothing is locked. */
        DISABLED,
        /** The grimoire of this boss was already awarded in this world: nothing rolls. */
        LOCKED,
        /** No eligible player killed it (a mob or the world did, or the killer was in creative): nothing rolls and the lock is untouched. */
        NO_KILLER,
        /** The roll failed: the lock is untouched, the next first kill rolls again. */
        MISS,
        /** The grimoire drops for the killer and the lock is set. */
        DROP
    }

    /** The base chance times the config multiplier, kept in 0 to 1. */
    public static double chance(double base, double multiplier) {
        double c = base * multiplier;
        if (Double.isNaN(c) || c <= 0.0) return 0.0;
        return Math.min(1.0, c);
    }

    /** {@code roll} gives a number in [0, 1); it is not called unless the table gets that far. */
    public static Result decide(boolean enabled, boolean locked, boolean hasKiller, double chance, DoubleSupplier roll) {
        if (!enabled) return Result.DISABLED;
        if (locked) return Result.LOCKED;
        if (!hasKiller) return Result.NO_KILLER;
        if (chance <= 0.0) return Result.MISS;
        if (chance >= 1.0) return Result.DROP;
        return roll.getAsDouble() < chance ? Result.DROP : Result.MISS;
    }
}
