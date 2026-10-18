package com.newuniverse.nusmp.heroweapon.weapon.sword;

import java.util.Random;

/**
 * Why: the Sword's techniques are shapes on the ground (a fan of crescents, a ring of falling stars, a rain of blades over a circle, a fissure in a line, a stab in
 * the back), and a shape that is a little wrong is a technique that visibly misses or hits what it should not. The arithmetic of the shapes is kept here, with no
 * Minecraft types, so that it is tested alone: the directions of a fan, the points of a ring, the scatter of a rain, the position along a line, and whether one
 * fighter stands in the back half of another. All angles are degrees; the plane is x/z; a zero direction counts as south (+z).
 */
public final class SwordGeo {
    private SwordGeo() {}

    /** The unit direction (dx, dz), or south for a zero direction. Returns {x, z}. */
    public static double[] unit(double dx, double dz) {
        double len = Math.sqrt(dx * dx + dz * dz);
        return len < 1.0E-9 ? new double[]{0.0, 1.0} : new double[]{dx / len, dz / len};
    }

    /** The direction (dx, dz) turned by {@code deg} degrees, as a unit vector (positive: x towards z). */
    public static double[] turn(double dx, double dz, double deg) {
        double[] u = unit(dx, dz);
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        return new double[]{u[0] * c - u[1] * s, u[0] * s + u[1] * c};
    }

    /** {@code count} unit directions spread evenly over {@code spreadDeg} either side of (dx, dz): one is straight ahead for an odd count. */
    public static double[][] fan(double dx, double dz, int count, double spreadDeg) {
        int n = Math.max(1, count);
        double[][] out = new double[n][];
        for (int i = 0; i < n; i++) {
            double t = n == 1 ? 0.0 : (i / (double) (n - 1)) * 2.0 - 1.0;            // -1 .. 1
            out[i] = turn(dx, dz, t * spreadDeg);
        }
        return out;
    }

    /** {@code n} points evenly round a circle of radius {@code r} about (cx, cz), the first at angle {@code phaseDeg}. Each is {x, z}. */
    public static double[][] ring(double cx, double cz, double r, int n, double phaseDeg) {
        int k = Math.max(1, n);
        double[][] out = new double[k][];
        for (int i = 0; i < k; i++) {
            double a = Math.toRadians(phaseDeg) + i * 2.0 * Math.PI / k;
            out[i] = new double[]{cx + Math.cos(a) * r, cz + Math.sin(a) * r};
        }
        return out;
    }

    /** {@code n} points scattered over the disc of radius {@code r} about (cx, cz), the same for the same seed. Each is {x, z}. */
    public static double[][] scatter(double cx, double cz, double r, int n, long seed) {
        Random rnd = new Random(seed);
        int k = Math.max(0, n);
        double[][] out = new double[k][];
        for (int i = 0; i < k; i++) {
            double a = rnd.nextDouble() * 2.0 * Math.PI, d = Math.sqrt(rnd.nextDouble()) * r;     // the square root makes the density even
            out[i] = new double[]{cx + Math.cos(a) * d, cz + Math.sin(a) * d};
        }
        return out;
    }

    /** How far along the direction (dx, dz) from the origin the point lies (negative: behind). */
    public static double along(double ox, double oz, double dx, double dz, double px, double pz) {
        double[] u = unit(dx, dz);
        return (px - ox) * u[0] + (pz - oz) * u[1];
    }

    /** The sideways distance of the point from the line through the origin along (dx, dz), always positive. */
    public static double aside(double ox, double oz, double dx, double dz, double px, double pz) {
        double[] u = unit(dx, dz);
        return Math.abs(-(px - ox) * u[1] + (pz - oz) * u[0]);
    }

    /** True if the point is on the strip of the given length and half width that starts at the origin and runs along (dx, dz). */
    public static boolean onStrip(double ox, double oz, double dx, double dz, double length, double halfWidth, double px, double pz) {
        double a = along(ox, oz, dx, dz, px, pz);
        return a >= 0 && a <= length && aside(ox, oz, dx, dz, px, pz) <= halfWidth;
    }

    /**
     * True if the attacker stands in the back half of the defender: the defender looks along (lookX, lookZ) and the attacker is at (toX, toZ) from the defender; they
     * are behind when the angle between the look and the way to the attacker is more than about 100 degrees. A defender with no facing (zero look) has no back.
     */
    public static boolean behind(double lookX, double lookZ, double toX, double toZ) {
        double ll = Math.sqrt(lookX * lookX + lookZ * lookZ), tl = Math.sqrt(toX * toX + toZ * toZ);
        if (ll < 1.0E-9 || tl < 1.0E-9) return false;
        return (lookX * toX + lookZ * toZ) / (ll * tl) < -0.17;
    }

    /** The yaw in degrees (Minecraft's convention: 0 faces +z, 90 faces -x) of a player who looks along (dx, dz). */
    public static float yawOf(double dx, double dz) { return (float) (Math.toDegrees(Math.atan2(-dx, dz))); }

    /** The most a dash may travel: up to {@code max} blocks, but not past a point {@code gap} before something {@code blockedAt} blocks ahead (negative: nothing). */
    public static double dashLength(double max, double blockedAt, double gap) {
        if (blockedAt < 0) return Math.max(0.0, max);
        return Math.max(0.0, Math.min(max, blockedAt - Math.max(0.0, gap)));
    }
}
