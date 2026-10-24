package com.newuniverse.nusmp.star;

import java.util.ArrayList;
import java.util.List;

/**
 * Star Magic nodes: the pure geometry (docs/vfx_remake/star_ingame.md). Points, the quartet / quintet ordering, the look pick of Conjunction, the oriented pane of Quartile Scutum, the chain of the whip and
 * the lance. No Minecraft class, no allocation per tick: every function is called once per cast on at most eight points.
 */
public final class StarMath {
    private StarMath() {}

    public record P(double x, double y, double z) {
        public P add(P o) { return new P(x + o.x, y + o.y, z + o.z); }
        public P sub(P o) { return new P(x - o.x, y - o.y, z - o.z); }
        public P scale(double k) { return new P(x * k, y * k, z * k); }
        public double dot(P o) { return x * o.x + y * o.y + z * o.z; }
        public double len() { return Math.sqrt(dot(this)); }
        public double dist(P o) { return sub(o).len(); }
        public P norm() { double l = len(); return l < 1e-9 ? new P(0, 0, 0) : scale(1 / l); }
        public P lerp(P o, double t) { return new P(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t); }
    }

    public static P centroid(List<P> pts) {
        double x = 0, y = 0, z = 0;
        for (P p : pts) { x += p.x; y += p.y; z += p.z; }
        int n = Math.max(1, pts.size());
        return new P(x / n, y / n, z / n);
    }

    // ---------------------------------------------------------------------------------------------- the quartet
    /**
     * The newest {@code n} stars within {@code range} of the caster ({@code recentOldestFirst} is the list of live stars, oldest first). Fewer than {@code n} in range returns what there is (the caller
     * decides what to do). The result is in placement order, not yet a loop; use {@link #orderAround} for a quadrilateral.
     */
    public static List<P> newest(List<P> recentOldestFirst, P caster, int n, double range) {
        List<P> out = new ArrayList<>(n);
        for (int i = recentOldestFirst.size() - 1; i >= 0 && out.size() < n; i--) {
            P p = recentOldestFirst.get(i);
            if (p.dist(caster) <= range) out.add(0, p);
        }
        return out;
    }

    /**
     * The same points in the order of a loop round their centroid, so lines between neighbours make a convex-looking polygon: the plane is the one the points are flattest in (the axis of the least
     * spread is dropped), the order is by angle round the centroid in the other two. Stable for equal angles by input order.
     */
    public static List<P> orderAround(List<P> pts) {
        if (pts.size() < 3) return new ArrayList<>(pts);
        P c = centroid(pts);
        double vx = 0, vy = 0, vz = 0;
        for (P p : pts) { vx += (p.x - c.x) * (p.x - c.x); vy += (p.y - c.y) * (p.y - c.y); vz += (p.z - c.z) * (p.z - c.z); }
        int drop = vy <= vx && vy <= vz ? 1 : vx <= vz ? 0 : 2;                 // the axis the points vary least in is the plane's normal
        List<double[]> keyed = new ArrayList<>();
        for (int i = 0; i < pts.size(); i++) {
            P p = pts.get(i);
            double a, b;
            switch (drop) {
                case 0 -> { a = p.y - c.y; b = p.z - c.z; }
                case 1 -> { a = p.x - c.x; b = p.z - c.z; }
                default -> { a = p.x - c.x; b = p.y - c.y; }
            }
            keyed.add(new double[]{Math.atan2(b, a), i});
        }
        keyed.sort((u, v) -> u[0] != v[0] ? Double.compare(u[0], v[0]) : Double.compare(u[1], v[1]));
        List<P> out = new ArrayList<>(pts.size());
        for (double[] k : keyed) out.add(pts.get((int) k[1]));
        return out;
    }

    /** The chain the whip follows: from {@code start} always to the nearest star not yet used. */
    public static List<P> chain(P start, List<P> pts) {
        List<P> left = new ArrayList<>(pts), out = new ArrayList<>(pts.size());
        P at = start;
        while (!left.isEmpty()) {
            int best = 0;
            for (int i = 1; i < left.size(); i++) if (left.get(i).dist(at) < left.get(best).dist(at)) best = i;
            at = left.remove(best);
            out.add(at);
        }
        return out;
    }

    // ---------------------------------------------------------------------------------------------- Conjunction's pick
    /**
     * The star the look vector points at: the one with the smallest angle to {@code look} (a unit vector from {@code eye}) that is within {@code maxAngleDeg} and {@code range} of the eye. Returns its index
     * in {@code nodes}, or -1 when none. O(n), n at most eight.
     */
    public static int pickByLook(List<P> nodes, P eye, P look, double maxAngleDeg, double range) {
        double cosMax = Math.cos(Math.toRadians(maxAngleDeg)), best = -2;
        int at = -1;
        P l = look.norm();
        for (int i = 0; i < nodes.size(); i++) {
            P d = nodes.get(i).sub(eye);
            double len = d.len();
            if (len < 0.5 || len > range) continue;
            double cos = d.scale(1 / len).dot(l);
            if (cos >= cosMax && cos > best) { best = cos; at = i; }
        }
        return at;
    }

    // ---------------------------------------------------------------------------------------------- the pane
    /** An oriented pane: centre, yaw of its normal (radians, in the x-z plane), width along the horizontal axis, height, thickness along the normal. */
    public record Pane(P center, double normalYaw, double width, double height, double thick) {
        public P normal() { return new P(Math.cos(normalYaw), 0, Math.sin(normalYaw)); }
        public P side() { return new P(-Math.sin(normalYaw), 0, Math.cos(normalYaw)); }
        /** The centre plus (u along the width, v up, w along the normal). */
        public P at(double u, double v, double w) { return center.add(side().scale(u)).add(new P(0, v, 0)).add(normal().scale(w)); }
        /** The AABB corners of the whole pane (for one cheap broad query): {minX, minY, minZ, maxX, maxY, maxZ}. */
        public double[] bounds() {
            double hu = width / 2, hw = thick / 2, hv = height / 2;
            double ex = Math.abs(side().x) * hu + Math.abs(normal().x) * hw, ez = Math.abs(side().z) * hu + Math.abs(normal().z) * hw;
            return new double[]{center.x - ex, center.y - hv, center.z - ez, center.x + ex, center.y + hv, center.z + ez};
        }
    }

    public static final double PANE_MAX_W = 6.0, PANE_MAX_H = 5.0, PANE_MIN_W = 1.5, PANE_MIN_H = 1.5, PANE_THICK = 1.5;
    public static final double PANE_MAX_W_QUINTILE = 8.0, PANE_MAX_H_QUINTILE = 6.0;

    /**
     * The pane the stars span: it stands at their centroid, faces the caster (its normal points from the centroid toward the caster), is as wide as the stars are on the horizontal axis across the
     * facing and as tall as they are, each clamped (width 1.5..max, height 1.5..max).
     */
    public static Pane pane(List<P> stars, P caster, double maxW, double maxH) {
        P c = centroid(stars);
        double dx = caster.x - c.x, dz = caster.z - c.z;
        double yaw = dx * dx + dz * dz < 1e-6 ? 0 : Math.atan2(dz, dx);
        Pane probe = new Pane(c, yaw, 1, 1, PANE_THICK);
        double minU = Double.MAX_VALUE, maxU = -Double.MAX_VALUE, minV = Double.MAX_VALUE, maxV = -Double.MAX_VALUE;
        for (P p : stars) {
            P d = p.sub(c);
            double u = d.dot(probe.side()), v = d.y;
            minU = Math.min(minU, u); maxU = Math.max(maxU, u);
            minV = Math.min(minV, v); maxV = Math.max(maxV, v);
        }
        double w = Math.max(PANE_MIN_W, Math.min(maxW, maxU - minU)), h = Math.max(PANE_MIN_H, Math.min(maxH, maxV - minV));
        return new Pane(c, yaw, w, h, PANE_THICK);
    }

    /** Does the oriented pane contain the point? */
    public static boolean contains(Pane pane, P p) {
        P d = p.sub(pane.center());
        return Math.abs(d.dot(pane.side())) <= pane.width() / 2 && Math.abs(d.y) <= pane.height() / 2 && Math.abs(d.dot(pane.normal())) <= pane.thick() / 2;
    }

    /** Does the segment a to b pass through the oriented pane (slab test in the pane's own axes)? */
    public static boolean crosses(Pane pane, P a, P b) {
        P da = a.sub(pane.center()), db = b.sub(pane.center());
        double[] lo = {-pane.width() / 2, -pane.height() / 2, -pane.thick() / 2}, hi = {pane.width() / 2, pane.height() / 2, pane.thick() / 2};
        double[] s = {da.dot(pane.side()), da.y, da.dot(pane.normal())}, e = {db.dot(pane.side()), db.y, db.dot(pane.normal())};
        double t0 = 0, t1 = 1;
        for (int i = 0; i < 3; i++) {
            double d = e[i] - s[i];
            if (Math.abs(d) < 1e-9) {
                if (s[i] < lo[i] || s[i] > hi[i]) return false;
            } else {
                double ta = (lo[i] - s[i]) / d, tb = (hi[i] - s[i]) / d;
                if (ta > tb) { double t = ta; ta = tb; tb = t; }
                t0 = Math.max(t0, ta);
                t1 = Math.min(t1, tb);
                if (t0 > t1) return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------------------------------------- the lance and the whip
    /** The end of a lance from {@code from} toward {@code aim}, never longer than {@code max}. */
    public static P lanceEnd(P from, P aim, double max) {
        P d = aim.sub(from);
        double l = d.len();
        return l <= max || l < 1e-9 ? aim : from.add(d.scale(max / l));
    }

    /** Distance from a point to the segment a to b. */
    public static double distToSegment(P p, P a, P b) {
        P ab = b.sub(a);
        double l2 = Math.max(1e-9, ab.dot(ab)), t = Math.max(0, Math.min(1, p.sub(a).dot(ab) / l2));
        return a.add(ab.scale(t)).dist(p);
    }

    /** Catmull-Rom point between p1 and p2 at t (0..1), p0 and p3 the neighbours. */
    public static P catmull(P p0, P p1, P p2, P p3, double t) {
        double t2 = t * t, t3 = t2 * t;
        double x = 0.5 * ((2 * p1.x) + (-p0.x + p2.x) * t + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3);
        double y = 0.5 * ((2 * p1.y) + (-p0.y + p2.y) * t + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3);
        double z = 0.5 * ((2 * p1.z) + (-p0.z + p2.z) * t + (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2 + (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3);
        return new P(x, y, z);
    }

    /** The whip as {@code perSegment} samples per leg through {@code hand, s1, s2, ...} (Catmull-Rom, the ends doubled). */
    public static List<P> whipPath(P hand, List<P> chain, int perSegment) {
        List<P> ctrl = new ArrayList<>();
        ctrl.add(hand);
        ctrl.addAll(chain);
        List<P> out = new ArrayList<>();
        int n = ctrl.size();
        for (int i = 0; i < n - 1; i++) {
            P p0 = ctrl.get(Math.max(0, i - 1)), p1 = ctrl.get(i), p2 = ctrl.get(i + 1), p3 = ctrl.get(Math.min(n - 1, i + 2));
            for (int k = 0; k < perSegment; k++) out.add(catmull(p0, p1, p2, p3, k / (double) perSegment));
        }
        out.add(ctrl.get(n - 1));
        return out;
    }

    // ---------------------------------------------------------------------------------------------- the automatic quartet
    /** {@code n} points on a horizontal ring of {@code radius} round {@code centre}, the first straight ahead ({@code yawRad}: direction of travel in the x-z plane). */
    public static List<P> ring(P centre, double yawRad, int n, double radius) {
        List<P> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double a = yawRad + Math.PI * 2 * i / n;
            out.add(new P(centre.x + Math.cos(a) * radius, centre.y, centre.z + Math.sin(a) * radius));
        }
        return out;
    }

    /** Where a placed star hangs: {@code lift} blocks above the point it was aimed at. */
    public static P hang(P aimed, double lift) { return new P(aimed.x, aimed.y + lift, aimed.z); }
}
