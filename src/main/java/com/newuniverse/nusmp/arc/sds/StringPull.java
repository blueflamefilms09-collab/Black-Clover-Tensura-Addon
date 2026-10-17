package com.newuniverse.nusmp.arc.sds;

/**
 * Why: the Marionette Strings are the only effect of Gowther that moves a player, so its arithmetic is kept small, bounded and tested. A string pulls a foe along the
 * ground towards him, never faster than a cap (a block a tick: a yank, not a teleport), never past a stopping distance in front of him (so nobody is pulled into his body), and
 * only while the foe is inside the string's range. The glue applies the speed each tick for a few ticks and checks the rest of the rules (line of sight, anti-magic,
 * the balance law). Pure: positions are numbers.
 */
public final class StringPull {
    private StringPull() {}

    /** The fastest a foe is pulled, in blocks per tick, and the share of the remaining distance covered each tick. */
    public static final double MAX_SPEED = 1.0, SHARE = 0.35;

    /** Does the string reach: the foe is within {@code range} blocks of him? */
    public static boolean reaches(double distance, double range) { return distance > 0 && distance <= range; }

    /** The speed (blocks per tick) a foe at {@code distance} is pulled at: a share of what is left to cover to {@code stop}, capped; 0 once it is there. */
    public static double speed(double distance, double stop) {
        double left = distance - stop;
        if (!(left > 0)) return 0;
        return Math.min(MAX_SPEED, left * SHARE);
    }

    /** The ticks a pull needs to bring a foe from {@code distance} to within {@code stop} (rounded up, at most {@code limit}). */
    public static int ticksNeeded(double distance, double stop, int limit) {
        double d = distance;
        int n = 0;
        while (d - stop > 0.05 && n < limit) {
            d -= speed(d, stop);
            n++;
        }
        return n;
    }

    /** The point {@code stop} blocks from the boss along the line to the foe (the foe's own position when it is already closer); {x, z}. */
    public static double[] destination(double bossX, double bossZ, double foeX, double foeZ, double stop) {
        double dx = foeX - bossX, dz = foeZ - bossZ, d = Math.hypot(dx, dz);
        if (d <= stop || d < 1e-9) return new double[]{foeX, foeZ};
        return new double[]{bossX + dx / d * stop, bossZ + dz / d * stop};
    }
}
