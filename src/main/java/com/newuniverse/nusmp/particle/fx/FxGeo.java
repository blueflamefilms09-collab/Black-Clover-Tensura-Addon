package com.newuniverse.nusmp.particle.fx;

/**
 * Why: the geometry and the per-call caps of the facade's directed emitters (a stream from A to B, a gathering ring around a target) are plain arithmetic, so they live here without game
 * classes and are unit-tested; {@link Fx} only turns the results into packets or {@code addParticle} calls. The caps are the ones the Light and Dark Rukh emitters had (12 per burst, 6 per
 * stream tick, 12 per gather, 2 per aura pulse) so that swapping a Rukh call for a themed call does not change how many particles an ability shows.
 */
public final class FxGeo {
    private FxGeo() {}

    public static final int MAX_BURST = 12;
    public static final int MAX_STREAM = 6;
    public static final int MAX_GATHER = 12;
    public static final int MAX_AURA = 2;
    /** Blocks per tick a directed particle may be given at spawn. */
    public static final double MAX_SPEED = 0.6;
    /** Flight time used for streams and gathers. */
    public static final int FLIGHT_TICKS = 18;
    public static final double RING_MIN = 2.5;
    public static final double RING_MAX = 4.0;

    public static int clamp(int n, int max) {
        return Math.max(0, Math.min(n, max));
    }

    /** Velocity (per tick) that covers (ddx, ddy, ddz) in {@code ticks} ticks, its length limited to {@link #MAX_SPEED}. */
    public static double[] toward(double ddx, double ddy, double ddz, int ticks) {
        double t = Math.max(1, ticks);
        double vx = ddx / t, vy = ddy / t, vz = ddz / t;
        double len = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (len > MAX_SPEED) {
            double k = MAX_SPEED / len;
            vx *= k;
            vy *= k;
            vz *= k;
        }
        return new double[] {vx, vy, vz};
    }

    /** Horizontal ring point for a gathering: {@code unit} in [0, 1) picks the radius between RING_MIN and RING_MAX, {@code angle} in radians. */
    public static double[] ring(double unit, double angle) {
        double u = Math.max(0, Math.min(0.999999, unit));
        double r = RING_MIN + (RING_MAX - RING_MIN) * u;
        return new double[] {Math.cos(angle) * r, Math.sin(angle) * r};
    }

    /** Evenly spread angle for particle i of n plus a jitter in [0, 1) of one slot. */
    public static double slotAngle(int i, int n, double jitter) {
        return (i + Math.max(0, Math.min(1, jitter))) * (Math.PI * 2 / Math.max(1, n));
    }

    public static double clampSpeed(double v) {
        return Math.max(-MAX_SPEED, Math.min(MAX_SPEED, v));
    }

    /**
     * How the count of a themed send is shared over the (up to four) particles of a theme: every particle gets one while the count lasts, the rest is split 40, 30, 20, 10 percent
     * (renormalised for fewer particles), the last one taking the rounding remainder. The shares always add up to the count.
     */
    public static int[] shares(int count, int particles) {
        int k = Math.max(1, Math.min(4, particles));
        int[] out = new int[k];
        int n = Math.max(0, count);
        if (n < k) {
            for (int i = 0; i < n; i++) out[i] = 1;
            return out;
        }
        double[] w = {0.4, 0.3, 0.2, 0.1};
        double sum = 0;
        for (int i = 0; i < k; i++) sum += w[i];
        int rem = n - k;
        for (int i = 0; i < k; i++) {
            int s = i == k - 1 ? rem : (int) Math.round((n - k) * w[i] / sum);
            s = Math.max(0, Math.min(rem, s));
            out[i] = 1 + s;
            rem -= s;
        }
        return out;
    }
}
