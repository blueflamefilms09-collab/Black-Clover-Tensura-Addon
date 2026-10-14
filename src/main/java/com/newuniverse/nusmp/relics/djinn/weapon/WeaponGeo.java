package com.newuniverse.nusmp.relics.djinn.weapon;

/**
 * The shapes the weapon acts strike in, as plain maths (pure): who is inside a cone, a beam, a wall in front of the bearer. All distances are blocks,
 * the plane is x/z (a vertical tolerance is asked separately), directions need not be normalised.
 */
public final class WeaponGeo {
    private WeaponGeo() {}

    /** The direction (dx, dz) turned by {@code deg} degrees (positive: x towards z). Returns {x, z}. */
    public static double[] turn(double dx, double dz, double deg) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        return new double[]{dx * c - dz * s, dx * s + dz * c};
    }

    /** True if the point (px, pz) lies within {@code range} of the origin and within {@code halfAngleDeg} of the direction (dx, dz). The origin itself is inside. */
    public static boolean inCone(double ox, double oz, double dx, double dz, double px, double pz, double range, double halfAngleDeg) {
        double rx = px - ox, rz = pz - oz, dist = Math.sqrt(rx * rx + rz * rz), dl = Math.sqrt(dx * dx + dz * dz);
        if (dist > range) return false;
        if (dist < 1.0E-6) return true;
        if (dl < 1.0E-9) return false;
        double cos = (rx * dx + rz * dz) / (dist * dl);
        return cos >= Math.cos(Math.toRadians(Math.max(0.0, Math.min(180.0, halfAngleDeg)))) - 1.0E-12;
    }

    /** Distance from the point p to the segment a-b in 3D. A zero-length segment is a point. */
    public static double segmentDistance(double ax, double ay, double az, double bx, double by, double bz, double px, double py, double pz) {
        double vx = bx - ax, vy = by - ay, vz = bz - az, wx = px - ax, wy = py - ay, wz = pz - az;
        double len2 = vx * vx + vy * vy + vz * vz;
        double t = len2 < 1.0E-12 ? 0.0 : Math.max(0.0, Math.min(1.0, (wx * vx + wy * vy + wz * vz) / len2));
        double cx = ax + vx * t - px, cy = ay + vy * t - py, cz = az + vz * t - pz;
        return Math.sqrt(cx * cx + cy * cy + cz * cz);
    }

    /** How far along the direction (dx, dz) from the origin the point lies (negative: behind). The direction is normalised here. */
    public static double along(double ox, double oz, double dx, double dz, double px, double pz) {
        double dl = Math.sqrt(dx * dx + dz * dz);
        return dl < 1.0E-9 ? 0.0 : ((px - ox) * dx + (pz - oz) * dz) / dl;
    }

    /** The signed sideways distance from the line through the origin along (dx, dz) (positive to the left, the direction turned +90 degrees). */
    public static double side(double ox, double oz, double dx, double dz, double px, double pz) {
        double dl = Math.sqrt(dx * dx + dz * dz);
        return dl < 1.0E-9 ? 0.0 : (-(px - ox) * dz + (pz - oz) * dx) / dl;
    }

    /** A wall: {@code from} to {@code to} blocks ahead and at most {@code halfWidth} to either side. */
    public static boolean inWall(double ox, double oz, double dx, double dz, double px, double pz, double from, double to, double halfWidth) {
        double a = along(ox, oz, dx, dz, px, pz), s = side(ox, oz, dx, dz, px, pz);
        return a >= from && a <= to && Math.abs(s) <= halfWidth;
    }

    /** Two crescents: two cones centred {@code spreadDeg} either side of the direction, each {@code halfAngleDeg} wide. A target in both counts once (a plain "or"). */
    public static boolean inCrescents(double ox, double oz, double dx, double dz, double px, double pz, double range, double spreadDeg, double halfAngleDeg) {
        double[] l = turn(dx, dz, spreadDeg), r = turn(dx, dz, -spreadDeg);
        return inCone(ox, oz, l[0], l[1], px, pz, range, halfAngleDeg) || inCone(ox, oz, r[0], r[1], px, pz, range, halfAngleDeg);
    }

    /** The damage factor of a chain's i-th link (0 first) when each link keeps {@code keep} of the one before. */
    public static double chainFactor(int link, double keep) { return Math.pow(Math.max(0.0, Math.min(1.0, keep)), Math.max(0, link)); }
}
