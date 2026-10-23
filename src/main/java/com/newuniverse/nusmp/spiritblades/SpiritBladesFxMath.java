package com.newuniverse.nusmp.spiritblades;

/**
 * Why: the drifting leaf sparkles round Spirit of Zephyr and Spirit of Boreas in hand are placed by pure maths (no game class), so a test can pin the caps and the shape of the motion and the client
 * painter ({@code client/SpiritBladesFx}) only draws what this says. At most {@link #MAX_LEAVES} leaves exist at once; each lives {@link #PERIOD_TICKS} ticks (a leaf is born on the weapon, drifts out
 * in a slow spiral toward the tip and fades in and out), then is reborn somewhere else along the weapon. Positions are fractions of the weapon's own length, so the same maths fits a sword and a
 * bow. A leaf is a pure function of (index, time): nothing is stored, nothing allocated (the caller passes the output array).
 */
public final class SpiritBladesFxMath {
    private SpiritBladesFxMath() {}

    /** Leaves alive at once on one weapon in hand; the lifetime of a leaf in ticks; the farthest a leaf drifts from the weapon's axis, as a share of the weapon's length. */
    public static final int MAX_LEAVES = 6;
    public static final float PERIOD_TICKS = 56f, MAX_RADIUS = 0.17f;
    /** Floats written by {@link #leaf}: along (0..1 of the length), across x, across z (shares of the length), size (share of the length), alpha (0..1), spin (radians). */
    public static final int FIELDS = 6;

    private static float frac(double v) { return (float) (v - Math.floor(v)); }

    private static float smooth(float a, float b, float x) {
        float t = Math.max(0f, Math.min(1f, (x - a) / (b - a)));
        return t * t * (3f - 2f * t);
    }

    /**
     * Leaf {@code i} (0 .. {@link #MAX_LEAVES} - 1) at time {@code time} (ticks, fractional) into {@code out} (length {@link #FIELDS}): {along, x, z, size, alpha, spin}.
     */
    public static void leaf(int i, float time, float[] out) {
        double t = time / PERIOD_TICKS + (double) i / MAX_LEAVES;
        double cycle = Math.floor(t);
        float phase = frac(t);
        float u0 = frac(Math.sin(cycle * 12.9898 + i * 78.233) * 43758.5453);
        float ang = (float) (i * 2.1 + cycle * 1.7 + phase * 4.2);
        float r = 0.02f + (MAX_RADIUS - 0.02f) * phase;
        out[0] = 0.12f + 0.76f * u0 + 0.10f * phase;
        out[1] = r * (float) Math.cos(ang);
        out[2] = r * (float) Math.sin(ang);
        out[3] = 0.030f + 0.012f * (float) Math.sin(i * 1.7 + cycle);
        out[4] = smooth(0f, 0.15f, phase) * (1f - smooth(0.6f, 1f, phase));
        out[5] = ang * 1.3f;
    }

    /** The most leaf quads one weapon draws in a frame: leaves times two crossed planes (each plane is drawn on both faces, as two quads). */
    public static int worstQuads() { return MAX_LEAVES * 4; }
}
