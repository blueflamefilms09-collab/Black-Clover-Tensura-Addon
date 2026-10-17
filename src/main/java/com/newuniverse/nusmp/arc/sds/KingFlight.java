package com.newuniverse.nusmp.arc.sds;

/**
 * Why: King hovers, and a hovering boss is easy to get wrong in the ways that matter in a fight: drifting up for ever when the ground disappears (a pit, the void,
 * a roof), never coming down when a move wants him low, and moving sideways too fast to be read. This pure controller turns "how far is the ground below me, how high
 * do I want to be" into a bounded vertical speed, the sideways drift into a bounded radial and tangential speed (so he closes in, backs off or circles his target
 * without ever out-running a sprinting player), and a dive into a path of eased points that ends exactly on the mark. The entity glue only probes, asks and applies.
 */
public final class KingFlight {
    private KingFlight() {}

    /** Hover heights above the ground, in blocks: high (out of a sword's reach even from a jump: a player's reach is 3 blocks from an eye 1.6 above their feet), low (a sword reaches his feet), landed. */
    public static final double HIGH = 6.0, LOW = 2.0, LANDED = 0.0;
    /** The longest ground probe (past it he simply falls, as any mob would). */
    public static final double PROBE = 14.0;
    /** The fastest he rises, sinks and drifts sideways, in blocks per tick (a sprinting player makes 0.28). */
    public static final double MAX_RISE = 0.16, MAX_SINK = 0.26, MAX_DRIFT = 0.19;
    /** The diving speed in blocks per tick and the fewest and most ticks a dive takes. */
    public static final double DIVE_SPEED = 1.5;
    public static final int DIVE_MIN = 6, DIVE_MAX = 18;

    /** Does a ground distance (NaN = nothing below within the probe) allow a hover? */
    public static boolean canHover(double groundDistance) { return !Double.isNaN(groundDistance) && groundDistance >= -0.01 && groundDistance <= PROBE; }

    /** The vertical speed that brings him to {@code want} blocks above the ground: proportional, clamped both ways; 0 when he must not hover. */
    public static double verticalSpeed(double groundDistance, double want) {
        if (!canHover(groundDistance)) return 0.0;
        double error = Math.max(0.0, want) - groundDistance;
        return Math.max(-MAX_SINK, Math.min(MAX_RISE, error * 0.22));
    }

    /** A slow bob (about plus or minus a third of a block, a period of 70 ticks) added to the high hover so he is never perfectly still. */
    public static double bob(long tick) { return 0.35 * Math.sin(tick * 0.09); }

    /**
     * The sideways drift as {radial, tangential} in blocks per tick: radial is positive towards the target (closing in) and negative away from it, zero inside the
     * band [lo, hi]; tangential is {@code strafeSign} times a gentle circling speed inside the band and zero outside it. Never more than {@link #MAX_DRIFT} in length.
     */
    public static double[] drift(double distance, double lo, double hi, int strafeSign) {
        if (Double.isNaN(distance)) return new double[]{0, 0};
        double radial = 0, tangent = 0;
        if (distance < lo) radial = -Math.min(MAX_DRIFT, (lo - distance) * 0.05 + 0.03);
        else if (distance > hi) radial = Math.min(MAX_DRIFT, (distance - hi) * 0.03 + 0.03);
        else tangent = (strafeSign >= 0 ? 1 : -1) * 0.07;
        double len = Math.hypot(radial, tangent);
        if (len > MAX_DRIFT) {
            radial = radial / len * MAX_DRIFT;
            tangent = tangent / len * MAX_DRIFT;
        }
        return new double[]{radial, tangent};
    }

    /** How many ticks a dive over {@code distance} blocks takes: the distance at the diving speed, at least {@link #DIVE_MIN} and at most {@link #DIVE_MAX}. */
    public static int diveTicks(double distance) {
        if (!(distance > 0)) return DIVE_MIN;
        return (int) Math.max(DIVE_MIN, Math.min(DIVE_MAX, Math.ceil(distance / DIVE_SPEED)));
    }

    /** The point after {@code k} of {@code n} ticks of a dive from {@code from} to {@code to} ({x, y, z}): eased in (slow, then fast), exactly {@code to} at the end. */
    public static double[] divePoint(double[] from, double[] to, int k, int n) {
        int steps = Math.max(1, n);
        double t = Math.max(0, Math.min(steps, k)) / (double) steps;
        double e = t * t;
        return new double[]{from[0] + (to[0] - from[0]) * e, from[1] + (to[1] - from[1]) * e, from[2] + (to[2] - from[2]) * e};
    }
}
