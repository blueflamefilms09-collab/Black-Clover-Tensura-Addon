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

    // ---------------------------------------------------------------- the fast arts
    /** An angle brought into (-pi, pi]. */
    public static double wrap(double a) {
        double w = a % (Math.PI * 2);
        if (w > Math.PI) w -= Math.PI * 2;
        else if (w <= -Math.PI) w += Math.PI * 2;
        return w;
    }

    /** The smallest difference between two headings, 0 to pi. */
    public static double angleBetween(double a, double b) { return Math.abs(wrap(a - b)); }

    /**
     * The two lines of a cross centred on (cx, cz): the first along `angle`, the second at a right angle to it, each `halfLen` either side of the
     * centre. Each is {ax, az, bx, bz}.
     */
    public static double[][] crossLines(double cx, double cz, double angle, double halfLen) {
        double[][] out = new double[2][];
        for (int i = 0; i < 2; i++) {
            double a = angle + i * Math.PI / 2, dx = Math.cos(a) * halfLen, dz = Math.sin(a) * halfLen;
            out[i] = new double[]{cx - dx, cz - dz, cx + dx, cz + dz};
        }
        return out;
    }

    /** Does a body at (px, pz) stand on either line of the cross? `halfWidth` is half the width of a line, with whatever allowance for the body the caller adds. */
    public static boolean crossHits(double px, double pz, double cx, double cz, double angle, double halfLen, double halfWidth) {
        for (double[] l : crossLines(cx, cz, angle, halfLen)) if (distToSegment(px, pz, l[0], l[1], l[2], l[3]) <= halfWidth) return true;
        return false;
    }

    /**
     * The points {x, z} that draw a strip between (ax, az) and (bx, bz), `halfWidth` either side: its two edges and its middle, evenly spaced about
     * `spacing` apart along each, never more than `maxPerRow` to a row (so a long lane costs no more than a short one past the cap). Edges first
     * (left, then right), the middle last.
     */
    public static List<double[]> strip(double ax, double az, double bx, double bz, double halfWidth, double spacing, int maxPerRow) {
        List<double[]> out = new ArrayList<>();
        double dx = bx - ax, dz = bz - az, len = Math.hypot(dx, dz);
        if (len < 1e-9 || maxPerRow < 2) return out;
        double nx = -dz / len, nz = dx / len;
        int n = (int) Math.max(2, Math.min(maxPerRow, Math.floor(len / Math.max(0.1, spacing)) + 1));
        for (int row = 0; row < 3; row++) {
            double off = row == 0 ? -halfWidth : row == 1 ? halfWidth : 0.0;
            for (int i = 0; i < n; i++) {
                double t = (double) i / (n - 1);
                out.add(new double[]{ax + dx * t + nx * off, az + dz * t + nz * off});
            }
        }
        return out;
    }

    /**
     * Does a body at (px, pz) stand in the fan centred on (cx, cz) that opens `half` either side of `heading` out to `radius`? A body counts by
     * its edge: the radius is stretched by `margin` (the body's half-width), and a body that close to the centre is in whatever way it lies.
     */
    public static boolean inFan(double px, double pz, double cx, double cz, double heading, double half, double radius, double margin) {
        double d = Math.hypot(px - cx, pz - cz);
        if (d > radius + margin) return false;
        if (d <= margin) return true;
        return angleBetween(Math.atan2(pz - cz, px - cx), heading) <= half + Math.asin(Math.min(1.0, margin / d));
    }

    /**
     * Where the blade of a cut has got to, as an angle from the heading, at the end of tick `tick` (0 is the first) of a cut that takes `total`
     * ticks: it sweeps from -half to +half, or back from +half to -half when `reverse`. After the last tick it has reached the far edge exactly.
     */
    public static double bladeAt(int tick, int total, double half, boolean reverse) {
        double f = Math.max(0, Math.min(total, tick + 1)) / (double) Math.max(1, total);
        double a = -half + 2.0 * half * f;
        return reverse ? -a : a;
    }

    /**
     * Did the blade, in the tick that took it from the angle a0 to the angle a1 (from the heading), pass over a body at (px, pz)? The union of
     * the slices of a cut is the whole fan, with no body skipped and none outside it.
     */
    public static boolean inSlice(double px, double pz, double cx, double cz, double heading, double a0, double a1, double radius, double margin) {
        double d = Math.hypot(px - cx, pz - cz);
        if (d > radius + margin) return false;
        if (d <= margin) return true;
        double rel = wrap(Math.atan2(pz - cz, px - cx) - heading), slack = Math.asin(Math.min(1.0, margin / d));
        return rel >= Math.min(a0, a1) - slack && rel <= Math.max(a0, a1) + slack;
    }

    /** The points {x, z} of an arc of radius r about (cx, cz), from `heading + from` to `heading + to`, about `spacing` apart, at least two and at most `max`. */
    public static List<double[]> arc(double cx, double cz, double heading, double from, double to, double r, double spacing, int max) {
        List<double[]> out = new ArrayList<>();
        int n = (int) Math.max(2, Math.min(Math.max(2, max), Math.ceil(Math.abs(to - from) * r / Math.max(0.1, spacing)) + 1));
        for (int i = 0; i < n; i++) {
            double a = heading + from + (to - from) * i / (n - 1);
            out.add(new double[]{cx + Math.cos(a) * r, cz + Math.sin(a) * r});
        }
        return out;
    }
}
