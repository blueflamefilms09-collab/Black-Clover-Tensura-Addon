package com.newuniverse.nusmp.shishou;

/**
 * Why: a very rare natural appearance of the one Shishou, decided without ever scanning the world: ONE roll at a day boundary (the caller compares the day number once a tick, an integer division), with a
 * small chance per day, only while no Shishou exists, near a random online player, never loading a chunk. This is the pure part: when a roll is due, whether it succeeds, and where (an offset in a ring
 * around the player). Defaults: a roll every day at a chance of 0.4% (about once in 250 days).
 */
public final class ShishouNatural {
    private ShishouNatural() {}

    public static final double DEFAULT_CHANCE = 0.004;
    public static final int MIN_DISTANCE = 40, MAX_DISTANCE = 80, ATTEMPTS = 3, HEIGHT_ABOVE_GROUND = 6;

    /** The day number of a game time. */
    public static long day(long gameTime) { return Math.floorDiv(gameTime, (long) ShishouRules.DAY_TICKS); }

    /** Is a roll due: a new day began since the last roll? */
    public static boolean due(long gameTime, long lastRollDay) { return day(gameTime) > lastRollDay; }

    /** Does a roll in [0, 1) succeed at the daily {@code chance}? */
    public static boolean succeeds(double roll, double chance) { return chance > 0.0 && roll >= 0.0 && roll < Math.min(1.0, chance); }

    /** An offset (dx, dz) in the ring between {@link #MIN_DISTANCE} and {@link #MAX_DISTANCE} from two rolls in [0, 1). */
    public static double[] offset(double angleRoll, double distRoll) {
        double a = angleRoll * Math.PI * 2.0;
        double d = MIN_DISTANCE + (MAX_DISTANCE - MIN_DISTANCE) * Math.max(0.0, Math.min(1.0, distRoll));
        return new double[]{Math.cos(a) * d, Math.sin(a) * d};
    }
}
