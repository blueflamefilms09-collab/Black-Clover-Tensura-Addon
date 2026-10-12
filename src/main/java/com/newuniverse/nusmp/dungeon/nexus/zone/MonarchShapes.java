package com.newuniverse.nusmp.dungeon.nexus.zone;

import java.util.ArrayList;
import java.util.List;

/**
 * The geometry of the Monarch's attacks, with no Minecraft in it: where the points of a ring are, when a spreading ring passes over a body,
 * how far a body is from a drawn line, which way the lines of a fan point, and where the circles of the Shadow Rain go. Pure, so the rules
 * of what hits and what misses are tested, not hoped for.
 */
public final class MonarchShapes {
    private MonarchShapes() {}

    /** n points {x, z} on a circle of radius r round (cx, cz), the first at angle `start` (radians). */
    public static double[][] ring(double cx, double cz, double r, int n, double start) {
        double[][] out = new double[n][2];
        for (int i = 0; i < n; i++) {
            double a = start + Math.PI * 2 * i / n;
            out[i][0] = cx + Math.cos(a) * r;
            out[i][1] = cz + Math.sin(a) * r;
        }
        return out;
    }

    /** How far a front that spreads at `speed` blocks a tick has got, `ticks` after it was loosed. */
    public static double front(int ticks, double speed) { return Math.max(0, ticks) * speed; }

    /**
     * Did a spreading ring pass over a body at horizontal distance `dist` in the tick that took its front from `prev` to `now`? The band of
     * the ring is `half` on either side of the front, so a body is never skipped however fast the front moves.
     */
    public static boolean passes(double dist, double prev, double now, double half) { return dist + half >= prev && dist - half <= now; }

    /**
     * Does a shockwave that runs along the floor at height ringY hurt a body at height bodyY? Only one standing on the floor within `reach` of
     * the ring's height: one that has jumped (or flies, or falls) passes over it, and so does one on another level of the hall.
     */
    public static boolean ringHurts(boolean onGround, double bodyY, double ringY, double reach) { return onGround && Math.abs(bodyY - ringY) <= reach; }

    /** The distance from the point (px, pz) to the segment (ax, az)-(bx, bz). */
    public static double distToSegment(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az, len2 = dx * dx + dz * dz;
        double t = len2 <= 1e-12 ? 0 : ((px - ax) * dx + (pz - az) * dz) / len2;
        t = Math.max(0, Math.min(1, t));
        double qx = ax + dx * t - px, qz = az + dz * t - pz;
        return Math.sqrt(qx * qx + qz * qz);
    }

    /** Does a line from (ax, az) to (bx, bz), `half` wide on either side, reach a body standing at (px, pz) with feet at py, when the line is drawn at height ay? */
    public static boolean beamHits(double px, double py, double pz, double ax, double ay, double az, double bx, double bz, double half, double height) {
        return distToSegment(px, pz, ax, az, bx, bz) <= half && py >= ay - 1.0 && py <= ay + height;
    }

    /** The end of a line of length `len` loosed from (ax, az) at `angle` (radians, 0 along +x). */
    public static double[] end(double ax, double az, double angle, double len) { return new double[]{ax + Math.cos(angle) * len, az + Math.sin(angle) * len}; }

    /** The angle of the direction from (ax, az) to (bx, bz). */
    public static double angle(double ax, double az, double bx, double bz) { return Math.atan2(bz - az, bx - ax); }

    /** The headings of a fan of `count` lines about `base`, `spread` apart: one straight, two either side, three straight and either side. */
    public static List<Double> fan(double base, int count, double spread) {
        List<Double> out = new ArrayList<>();
        for (int i = 0; i < count; i++) out.add(base + spread * (i - (count - 1) / 2.0));
        return out;
    }

    /**
     * The centres of the circles of one Shadow Rain, {x, z}. The first of them lie under the foes (so staying put is punished), the rest
     * are scattered over the floor round `centre` out to `arena`; every one lies inside the arena and no two are closer than `gap`.
     * Deterministic for a seed. Fewer than asked for are returned if the floor has no room.
     */
    public static List<double[]> rain(long seed, List<double[]> foes, int count, double cx, double cz, double arena, double gap) {
        MonarchRng r = new MonarchRng(seed);
        List<double[]> out = new ArrayList<>();
        for (double[] f : foes) {
            if (out.size() >= count) break;
            if (fits(out, f[0], f[1], cx, cz, arena, gap)) out.add(new double[]{f[0], f[1]});
        }
        for (int attempt = 0; attempt < count * 40 && out.size() < count; attempt++) {
            double a = r.unit() * Math.PI * 2, d = Math.sqrt(r.unit()) * arena;
            double x = cx + Math.cos(a) * d, z = cz + Math.sin(a) * d;
            if (!foes.isEmpty() && r.chance(60)) {                           // near a foe, not on it: the rain follows the fight
                double[] f = foes.get(r.below(foes.size()));
                double b = r.unit() * Math.PI * 2, e = 3 + r.unit() * 5;
                x = f[0] + Math.cos(b) * e;
                z = f[1] + Math.sin(b) * e;
            }
            if (fits(out, x, z, cx, cz, arena, gap)) out.add(new double[]{x, z});
        }
        return out;
    }

    private static boolean fits(List<double[]> placed, double x, double z, double cx, double cz, double arena, double gap) {
        if ((x - cx) * (x - cx) + (z - cz) * (z - cz) > arena * arena) return false;
        for (double[] p : placed) if ((x - p[0]) * (x - p[0]) + (z - p[1]) * (z - p[1]) < gap * gap) return false;
        return true;
    }

    /** Is a body at (px, pz) inside a circle of radius r about (cx, cz)? */
    public static boolean inside(double px, double pz, double cx, double cz, double r) { return (px - cx) * (px - cx) + (pz - cz) * (pz - cz) <= r * r; }
}
