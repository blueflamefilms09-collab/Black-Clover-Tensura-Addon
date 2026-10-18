package com.newuniverse.nusmp.heroweapon.weapon.bow;

/**
 * Why: a rain of arrows must look like a rain (spread over the circle, a different pattern each wave) and be the same every time it is computed, so the
 * effect can be tested and never depends on a random generator that differs between the server and a test. The points are laid out on a spiral by the
 * golden angle (the way seeds fill a sunflower): evenly spread, never two on top of each other, always inside the circle, and a salt turns the pattern so
 * the next wave falls in the gaps of the last.
 */
public final class Volley {
    private Volley() {}

    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    /** {@code n} points (x, z) inside a circle of {@code radius} round the origin, spread evenly; {@code salt} turns the pattern. */
    public static double[][] scatter(int n, double radius, int salt) {
        int count = Math.max(0, n);
        double[][] out = new double[count][2];
        for (int i = 0; i < count; i++) {
            double r = radius * Math.sqrt((i + 0.5) / count);
            double a = i * GOLDEN_ANGLE + salt * 0.9;
            out[i][0] = Math.cos(a) * r;
            out[i][1] = Math.sin(a) * r;
        }
        return out;
    }
}
