package com.newuniverse.nusmp.arc.spade;

/**
 * Why: the geometry of Morris's thread and of his closing cage is plain 2D arithmetic (a distance from a point to a segment, a speed that grows with
 * age), and a mistake in it would make the counter-play lie (an ally "on the thread" who is not, a pull that outruns a walking player). Keeping it free
 * of game types lets the tests pin it: the pull never beats walking, the middle of a thread is where it can be cut and its ends are not.
 */
public final class TetherMath {
    private TetherMath() {}

    /** A player walks 0.216 blocks a tick; the pull stays under this at every age, so a specimen can always walk away (and is dragged only while it stands or fights). */
    public static final double WALK = 0.216;
    /** The strongest pull, blocks per tick. */
    public static final double MAX_PULL = 0.13;
    /** Half the width of the thread in blocks: a foe closer than this to the line is on it. */
    public static final double THREAD_HALF_WIDTH = 1.15;

    /** Pull speed at {@code age} of a thread that lives {@code life} ticks: 0.03 at the start, {@link #MAX_PULL} at the end. */
    public static double pullSpeed(int age, int life) {
        double t = Math.max(0, Math.min(1, age / (double) Math.max(1, life)));
        return 0.03 + (MAX_PULL - 0.03) * t;
    }

    /** Distance from the point (px, pz) to the segment (ax, az)-(bx, bz). */
    public static double distToSegment(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az;
        double len2 = dx * dx + dz * dz;
        if (len2 < 1e-9) return Math.hypot(px - ax, pz - az);
        double t = ((px - ax) * dx + (pz - az) * dz) / len2;
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(px - (ax + dx * t), pz - (az + dz * t));
    }

    /** Where along the segment (0 at a, 1 at b) the point's nearest approach lies, clamped to 0..1. */
    public static double along(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az;
        double len2 = dx * dx + dz * dz;
        if (len2 < 1e-9) return 0;
        return Math.max(0, Math.min(1, ((px - ax) * dx + (pz - az) * dz) / len2));
    }

    /**
     * Is the point standing on the thread between a and b so that it can cut it: within {@link #THREAD_HALF_WIDTH} of the line AND in the middle part of
     * it (not within 2 blocks of either end, where a foe is simply next to the owner or the specimen)?
     */
    public static boolean cuts(double px, double pz, double ax, double az, double bx, double bz) {
        double len = Math.hypot(bx - ax, bz - az);
        if (len < 4.5) return false;
        if (distToSegment(px, pz, ax, az, bx, bz) > THREAD_HALF_WIDTH) return false;
        double t = along(px, pz, ax, az, bx, bz);
        return t * len >= 2.0 && (1 - t) * len >= 2.0;
    }
}
