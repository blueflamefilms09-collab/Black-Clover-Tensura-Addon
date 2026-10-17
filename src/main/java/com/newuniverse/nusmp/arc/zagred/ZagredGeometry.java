package com.newuniverse.nusmp.arc.zagred;

import java.util.Random;

/**
 * Why: Zagred's attacks are shapes on the floor (a band round him, a lane, a wave ring, a field of marks, a curve of letters) and the telegraph must draw
 * exactly the shape the blow later uses, or "step out of the ring" would be a lie. The geometry is flat arithmetic on x and z, so it is kept here with no
 * game types: the boss class asks "is this foe in the band", "how far is this foe from the lane", the tests prove the answers, and the telegraph and the
 * damage use the same call. All distances are in blocks on the horizontal plane.
 */
public final class ZagredGeometry {
    private ZagredGeometry() {}

    /** The unit vector of (dx, dz); (0, 1) when it has no length. */
    public static double[] unit(double dx, double dz) {
        double len = Math.sqrt(dx * dx + dz * dz);
        return len < 1e-6 ? new double[]{0, 1} : new double[]{dx / len, dz / len};
    }

    /** (dx, dz) turned by {@code radians} (counter-clockwise seen from above). */
    public static double[] rotate(double dx, double dz, double radians) {
        double c = Math.cos(radians), s = Math.sin(radians);
        return new double[]{dx * c - dz * s, dx * s + dz * c};
    }

    /**
     * The letters of a sentence, as {@code [i][x, z]}: a curve from the caster (sx, sz) towards (dx, dz), the first letter three blocks out and each next
     * one 2.6 further, swaying to one side by up to 2.2 blocks (the sign of {@code bend} says which side, its size how far: 1 and -1 are the usual).
     */
    public static double[][] curve(double sx, double sz, double dx, double dz, int n, double bend) {
        double[] d = unit(dx, dz);
        double sideX = -d[1], sideZ = d[0];
        double[][] out = new double[Math.max(0, n)][2];
        for (int i = 0; i < out.length; i++) {
            double along = 3 + i * 2.6, sway = Math.sin(i * 0.7) * 2.2 * bend;
            out[i][0] = sx + d[0] * along + sideX * sway;
            out[i][1] = sz + d[1] * along + sideZ * sway;
        }
        return out;
    }

    /** Is a foe at {@code dist} from the centre inside the band {@code inner <= dist <= outer} (the Halt band: bound between the safe core and the ring)? */
    public static boolean inBand(double dist, double inner, double outer) { return dist >= inner && dist <= outer; }

    /** Is a foe at {@code dist} from the centre on the wave ring of radius {@code ring} (within {@code halfBand} of it)? */
    public static boolean onRing(double dist, double ring, double halfBand) { return Math.abs(dist - ring) <= halfBand; }

    /** The distance from the point (px, pz) to the segment (ax, az)-(bx, bz). */
    public static double distanceToSegment(double px, double pz, double ax, double az, double bx, double bz) {
        double vx = bx - ax, vz = bz - az, len2 = vx * vx + vz * vz;
        double t = len2 < 1e-9 ? 0 : ((px - ax) * vx + (pz - az) * vz) / len2;
        t = Math.max(0, Math.min(1, t));
        double cx = ax + vx * t - px, cz = az + vz * t - pz;
        return Math.sqrt(cx * cx + cz * cz);
    }

    /**
     * {@code count} impact points scattered round (cx, cz) within {@code spread} blocks, kept at least {@code gap} apart where the field has room (the
     * last try is kept when it has not, so the count is always met). The same seed always gives the same field.
     */
    public static double[][] spots(double cx, double cz, int count, double spread, double gap, long seed) {
        Random rng = new Random(seed);
        double[][] out = new double[Math.max(0, count)][2];
        for (int i = 0; i < out.length; i++) {
            double bx = cx, bz = cz;
            for (int attempt = 0; attempt < 25; attempt++) {
                double a = rng.nextDouble() * Math.PI * 2, r = Math.sqrt(rng.nextDouble()) * spread;
                bx = cx + Math.cos(a) * r;
                bz = cz + Math.sin(a) * r;
                boolean clear = true;
                for (int j = 0; j < i && clear; j++) {
                    double ox = out[j][0] - bx, oz = out[j][1] - bz;
                    if (ox * ox + oz * oz < gap * gap) clear = false;
                }
                if (clear) break;
            }
            out[i][0] = bx;
            out[i][1] = bz;
        }
        return out;
    }
}
