package com.newuniverse.nusmp.particle.rukh;

/**
 * Why: the per-call caps and the geometry of the Rukh emitters (ring placement for the Magi gathering, the velocity that carries a
 * particle from A to B in a fixed flight time) are plain arithmetic, so they live here without Minecraft classes and can be unit tested;
 * {@code RukhFx} and {@code RukhClientFx} only translate the results into packets / addParticle calls. Keeping the numbers in one class also
 * guarantees the server twin and the client twin never drift apart.
 */
public final class RukhMath {
    private RukhMath() {}

    public static final int MAX_BURST = 12;
    public static final int MAX_STREAM = 6;
    public static final int MAX_GATHER = 12;
    public static final int MAX_AURA = 2;
    /** Blocks per tick no Rukh may exceed at spawn (the particle class clamps again while it flies). */
    public static final double MAX_SPEED = 0.6;
    /** Flight time used for streams and gathers; about the middle of the 15..25 tick lifetime. */
    public static final int FLIGHT_TICKS = 18;
    public static final double RING_MIN = 2.5;
    public static final double RING_MAX = 4.0;

    public static int clamp(int n, int max) {
        return Math.max(0, Math.min(n, max));
    }

    /** Velocity (dx,dy,dz per tick) that covers (ddx,ddy,ddz) in {@code ticks} ticks, length limited to {@link #MAX_SPEED}. */
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

    /** Horizontal ring point for the gathering: {@code unit} in [0,1) picks the radius between RING_MIN and RING_MAX, {@code angle} in radians. */
    public static double[] ring(double unit, double angle) {
        double u = Math.max(0, Math.min(0.999999, unit));
        double r = RING_MIN + (RING_MAX - RING_MIN) * u;
        return new double[] {Math.cos(angle) * r, Math.sin(angle) * r};
    }

    /** Evenly spread angle for particle i of n plus a jitter in [0,1) of one slot. */
    public static double slotAngle(int i, int n, double jitter) {
        return (i + Math.max(0, Math.min(1, jitter))) * (Math.PI * 2 / Math.max(1, n));
    }

    /** Smooth scale curve of a Rukh over its life: ease in over the first 20 %, hold, ease out over the last 30 %. Result in 0..1. */
    public static float scaleCurve(float lifeFraction) {
        float f = Math.max(0f, Math.min(1f, lifeFraction));
        float in = Math.min(1f, f / 0.2f);
        float out = Math.min(1f, (1f - f) / 0.3f);
        float k = Math.min(in, out);
        return k * k * (3f - 2f * k);
    }

    /** Alpha curve: fully visible for the first 55 % of life, then a gentle fade to 0. */
    public static float alphaCurve(float lifeFraction) {
        float f = Math.max(0f, Math.min(1f, lifeFraction));
        if (f <= 0.55f) return 1f;
        float k = (1f - f) / 0.45f;
        return k * k * (3f - 2f * k);
    }

    /** Clamps one velocity component to +-MAX_SPEED. */
    public static double clampSpeed(double v) {
        return Math.max(-MAX_SPEED, Math.min(MAX_SPEED, v));
    }
}
