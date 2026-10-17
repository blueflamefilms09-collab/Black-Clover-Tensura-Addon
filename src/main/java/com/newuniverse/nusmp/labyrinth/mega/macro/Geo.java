package com.newuniverse.nusmp.labyrinth.mega.macro;

/**
 * Why: the Spire and the Chasm are bodies of revolution (tapering rings, spirals, angular sectors), so both models do the same few pieces of arithmetic: a
 * smooth blend, a linear blend, the angle of a column round an axis, the index of the angular sector it lies in. They are collected here so the two models
 * cannot drift apart, and so every transcendental call goes through {@link StrictMath}: {@code Math.atan2} is allowed to differ in the last bit between
 * JVMs and CPUs, and a one-bit difference in the angle of a column on a sector border would put a block in a different tile on another machine, which a world
 * that is generated chunk by chunk (maybe over several server moves) must never do. Pure: no Minecraft import.
 */
final class Geo {
    private Geo() {}

    static final double TWO_PI = 2.0 * Math.PI;

    /** Smoothstep: 0 below {@code a}, 1 above {@code b}, an S curve between (zero slope at both ends). */
    static double smoothstep(double a, double b, double v) {
        if (b == a) return v < a ? 0.0 : 1.0;
        double t = (v - a) / (b - a);
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return t * t * (3.0 - 2.0 * t);
    }

    static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    static int floorToInt(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    /** The angle of the offset (dx, dz) in (-pi, pi], bit-identical on every JVM. */
    static double angle(double dx, double dz) { return StrictMath.atan2(dz, dx); }

    /** {@code a} wrapped into [-pi, pi). */
    static double wrapPi(double a) {
        double r = a - TWO_PI * Math.floor((a + Math.PI) / TWO_PI);
        return r >= Math.PI ? r - TWO_PI : r;
    }

    /**
     * The angular sector of a position angle when the ring is cut into {@code n} equal sectors and sector 0 is CENTRED on {@code centre}: sector k covers
     * the angles {@code centre + (k - 0.5 .. k + 0.5) * 2pi / n}. Why centred: the guaranteed doors and landings sit on a known angle, and a door that
     * is the middle of a sector (not on the border of two) cannot be cut in half by the grid.
     */
    static int sector(double angle, double centre, int n) {
        double rel = wrapPi(angle - centre) / TWO_PI * n;
        int k = floorToInt(rel + 0.5);
        k %= n;
        return k < 0 ? k + n : k;
    }

    /** Where inside its sector the angle lies: 0 .. 1 from the sector's low edge to its high edge. */
    static double sectorFraction(double angle, double centre, int n) {
        double rel = wrapPi(angle - centre) / TWO_PI * n;
        double f = rel + 0.5 - Math.floor(rel + 0.5);
        return f < 0 ? 0 : (f >= 1 ? 0.999999 : f);
    }
}
