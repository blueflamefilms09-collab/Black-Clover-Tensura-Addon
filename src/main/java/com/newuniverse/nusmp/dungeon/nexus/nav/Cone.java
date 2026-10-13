package com.newuniverse.nusmp.dungeon.nexus.nav;

/**
 * The cone maths of the Ki-Resonance Lantern (docs/living_dungeon.md section 3): is something inside a cone of a few degrees round the look
 * direction, and a fan of rays that covers such a cone. Pure (plain doubles), so it is tested without the game.
 */
public final class Cone {
    private Cone() {}

    /** The angle in degrees between two vectors, 0 if either has no length. */
    public static double angleDeg(double ax, double ay, double az, double bx, double by, double bz) {
        double la = Math.sqrt(ax * ax + ay * ay + az * az), lb = Math.sqrt(bx * bx + by * by + bz * bz);
        if (la < 1.0e-9 || lb < 1.0e-9) return 0;
        double dot = (ax * bx + ay * by + az * bz) / (la * lb);
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
    }

    /** Is the vector b within {@code halfAngleDeg} of the vector a? Something with no offset at all (you stand on it) counts as ahead. */
    public static boolean within(double ax, double ay, double az, double bx, double by, double bz, double halfAngleDeg) {
        return angleDeg(ax, ay, az, bx, by, bz) <= halfAngleDeg;
    }

    /** Rays in a fan: the centre, then rings at a third, two thirds and the whole of the half angle holding 6, 12 and 18 rays. */
    public static final int FAN_SIZE = 1 + 6 + 12 + 18;

    /**
     * A fan of unit directions that covers the cone of half angle {@code halfAngleDeg} round the direction (fx, fy, fz): index 0 is the
     * direction itself. Every ray is within the half angle of it. A direction with no length gives straight ahead along +z.
     */
    public static double[][] fan(double fx, double fy, double fz, double halfAngleDeg) {
        double len = Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (len < 1.0e-9) { fx = 0; fy = 0; fz = 1; len = 1; }
        fx /= len;
        fy /= len;
        fz /= len;
        // two unit vectors at right angles to the direction and to each other: u = reference x forward, with "up" as the reference unless looking straight up or down
        double ux, uy, uz;
        if (Math.abs(fy) < 0.99) { ux = fz; uy = 0; uz = -fx; }
        else { ux = 0; uy = -fz; uz = fy; }
        double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul;
        uy /= ul;
        uz /= ul;
        double vx = fy * uz - fz * uy, vy = fz * ux - fx * uz, vz = fx * uy - fy * ux;
        double[][] out = new double[FAN_SIZE][];
        out[0] = new double[]{fx, fy, fz};
        int n = 1;
        for (int ring = 1; ring <= 3; ring++) {
            double theta = Math.toRadians(halfAngleDeg * ring / 3.0), ct = Math.cos(theta), st = Math.sin(theta);
            int rays = 6 * ring;
            for (int k = 0; k < rays; k++) {
                double phi = 2 * Math.PI * (k + 0.5 * (ring & 1)) / rays;                  // odd rings are turned half a step so the rays do not line up
                double cp = Math.cos(phi), sp = Math.sin(phi);
                out[n++] = new double[]{
                        fx * ct + (ux * cp + vx * sp) * st,
                        fy * ct + (uy * cp + vy * sp) * st,
                        fz * ct + (uz * cp + vz * sp) * st};
            }
        }
        return out;
    }
}
