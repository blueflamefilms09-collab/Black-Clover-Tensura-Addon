package com.newuniverse.nusmp.dungeon.nexus.vault;

/**
 * The gravity pulses of the deep of a distorted pocket (docs/living_dungeon.md, "Spatial Warping"), for one player: while they stand in a
 * pocket of strength 0.6 or more, every few seconds the space round them gives a warning (a spiral of particles for a second and a half) and
 * then throws them up, or slams them down. A state machine on the game's clock; the glue supplies the strength, whether the player is
 * exempt (anchored, or not in play), whether there is room above them, and two random numbers 0 to 99. Anything that stops the pocket
 * from applying (leaving it, being exempt) resets it, so no pulse ever fires from an old warning. Pure.
 */
public final class GravityPulse {
    /** What the glue should do now. */
    public enum Act { NONE, TELEGRAPH, SPIRAL, LIFT, CRUSH }

    public static final double MIN_STRENGTH = 0.6;
    /** Ticks of warning, and the shortest and longest rest between two pulses. */
    public static final int WARNING = 30, MIN_GAP = 100, MAX_GAP = 240, FIRST = 60;

    private long nextAt = Long.MIN_VALUE, fireAt;
    private boolean armed, lift;

    public Act step(long now, double strength, boolean exempt, boolean headroom, int rollA, int rollB) {
        if (exempt || strength < MIN_STRENGTH) {
            armed = false;
            nextAt = Long.MIN_VALUE;
            return Act.NONE;
        }
        if (armed) {
            if (now < fireAt) return Act.SPIRAL;
            armed = false;
            nextAt = now + MIN_GAP + (long) clamp(rollA) * (MAX_GAP - MIN_GAP) / 99;
            return lift ? Act.LIFT : Act.CRUSH;
        }
        if (nextAt == Long.MIN_VALUE) {
            nextAt = now + FIRST + clamp(rollA);
            return Act.NONE;
        }
        if (now < nextAt) return Act.NONE;
        armed = true;
        fireAt = now + WARNING;
        lift = headroom && clamp(rollB) < 60;
        return Act.TELEGRAPH;
    }

    private static int clamp(int roll) { return Math.max(0, Math.min(99, roll)); }

    /** Is the pulse being warned of an upward one? */
    public boolean isLift() { return lift; }

    public boolean armed() { return armed; }

    /** How hard a pulse of this strength throws, in blocks per tick: 0.75 at the threshold up to 1.1 at the heart of a pocket. */
    public static double impulse(double strength) {
        double t = (strength - MIN_STRENGTH) / (1.0 - MIN_STRENGTH);
        return 0.75 + 0.35 * Math.max(0.0, Math.min(1.0, t));
    }
}
