package com.newuniverse.nusmp.heroweapon.weapon.whip;

/**
 * Why: the Whip moves bodies with a tether: Reel In pulls a foe toward the wielder, Tether Swing pulls the wielder toward a foe or a ledge. A velocity set by a technique is
 * the one place a bug can fling someone out of the world or into the ground, so the arithmetic is separate, pure and tested: the speeds grow with the distance but are clamped
 * (a pull never exceeds 1.5 blocks a tick, a swing 1.6), the lift is bounded, a foe that is already close is not pulled at all, and a degenerate direction gives no motion.
 */
public final class TetherMath {
    private TetherMath() {}

    /** A foe closer than this (plus a margin) is not pulled; the pull speed range (blocks a tick) and the lift given to the pulled body. */
    public static final double PULL_STOP = 2.4, PULL_MARGIN = 0.3, PULL_MIN = 0.45, PULL_MAX = 1.5, PULL_LIFT = 0.28;
    /** A swing needs a mark at least this far; the swing speed range and the lift range. */
    public static final double SWING_MIN_DISTANCE = 3.0, SWING_MIN = 0.6, SWING_MAX = 1.6, LIFT_MIN = 0.25, LIFT_MAX = 0.9;

    /** True when a foe at this horizontal distance is far enough to be worth pulling. */
    public static boolean needsPull(double distance) { return distance > PULL_STOP + PULL_MARGIN; }

    /** The speed to pull a body {@code distance} blocks away at (blocks a tick). */
    public static double pullSpeed(double distance) { return clamp(0.35 + 0.12 * Math.max(0.0, distance), PULL_MIN, PULL_MAX); }

    /** True when a mark at this distance is far enough to swing to. */
    public static boolean canSwing(double distance) { return distance >= SWING_MIN_DISTANCE; }

    /** The speed to swing {@code distance} blocks at (blocks a tick). */
    public static double swingSpeed(double distance) { return clamp(0.5 + 0.1 * Math.max(0.0, distance), SWING_MIN, SWING_MAX); }

    /** The upward speed of a swing toward a mark {@code dy} blocks above the wielder (negative below) and {@code distance} away. */
    public static double swingLift(double dy, double distance) { return clamp(0.3 + 0.08 * dy + 0.01 * Math.max(0.0, distance), LIFT_MIN, LIFT_MAX); }

    /** The horizontal velocity {x, z} of {@code speed} toward (dx, dz); {0, 0} when the direction has no length. */
    public static double[] toward(double dx, double dz, double speed) {
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-6) return new double[]{0.0, 0.0};
        return new double[]{dx / len * speed, dz / len * speed};
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}
