package com.newuniverse.nusmp.boss.arc;

/**
 * Why: in act 1 Lucius levitates three blocks above the ground. Turning gravity off and nudging his height is easy to get wrong in
 * two ways that matter in a boss fight: drifting away upward when the ground disappears (a pit, the void, a roof) and never coming
 * back down when the act ends. This pure controller (no Minecraft types, unit tested) turns "how far is the ground below me" into a
 * bounded vertical speed, and says when the hover must switch itself off (no ground within the probe), so the entity glue stays a few
 * lines: probe, ask, apply.
 */
public final class HoverMath {
    private HoverMath() {}

    /** The hover height above the ground in blocks, and the longest ground probe (past it he simply falls, as any mob would). */
    public static final double HEIGHT = 3.0, PROBE = 12.0;
    /** The fastest he rises or sinks per tick. */
    public static final double MAX_RISE = 0.14, MAX_SINK = 0.20;
    /** The fastest he drifts sideways per tick when the glue steers him, and the distance band he likes in act 1. */
    public static final double MAX_DRIFT = 0.12, BAND_LOW = 9.0, BAND_HIGH = 15.0;

    /** Does a ground distance (NaN = nothing below within the probe) allow a hover? */
    public static boolean canHover(double groundDistance) {
        return !Double.isNaN(groundDistance) && groundDistance >= -0.01 && groundDistance <= PROBE;
    }

    /** The vertical speed that brings him to {@link #HEIGHT} above the ground: proportional, clamped both ways. */
    public static double verticalSpeed(double groundDistance) {
        if (!canHover(groundDistance)) return 0.0;
        double error = HEIGHT - groundDistance;
        return Math.max(-MAX_SINK, Math.min(MAX_RISE, error * 0.22));
    }

    /**
     * The horizontal drift along the line to his target: negative backs away, positive closes in, zero inside the band. Never more than
     * {@link #MAX_DRIFT}, and smaller the nearer the edge of the band.
     */
    public static double drift(double distanceToTarget) {
        if (Double.isNaN(distanceToTarget)) return 0.0;
        if (distanceToTarget < BAND_LOW) return -Math.min(MAX_DRIFT, (BAND_LOW - distanceToTarget) * 0.05 + 0.02);
        if (distanceToTarget > BAND_HIGH) return Math.min(MAX_DRIFT, (distanceToTarget - BAND_HIGH) * 0.03 + 0.02);
        return 0.0;
    }
}
