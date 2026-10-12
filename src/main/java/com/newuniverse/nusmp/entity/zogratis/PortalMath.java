package com.newuniverse.nusmp.entity.zogratis;

import java.util.ArrayList;
import java.util.List;

/**
 * The geometry of Zenon's Spatial Magic (pure, no Minecraft types, unit tested): the spot behind a target, where the exit portals of
 * a portal-link volley open, the hops of a warp barrage, the retreat after a backstab, and how many of each there are at a power.
 * Points are {x, z} on the ground; the caller finds the floor.
 */
public final class PortalMath {
    private PortalMath() {}

    /** Ticks the entry portal is open before a blink takes the boss through (a visible gate, a short blink time). */
    public static final int BLINK_TICKS = 5;
    /** Ticks the exit portals of a portal link stay open before the first spear comes out (the player's window to read them). */
    public static final int EMERGE_TICKS = 18;
    public static final double MIN_EXIT_SEPARATION = 3.5, HOP_STEP = 2.4;

    /** The spot 'dist' blocks behind a target at (tx, tz) facing 'yawDeg' (Minecraft's yaw: 0 faces +Z). */
    public static double[] behind(double tx, double tz, double yawDeg, double dist) {
        double yaw = Math.toRadians(yawDeg);
        return new double[]{tx + Math.sin(yaw) * dist, tz - Math.cos(yaw) * dist};
    }

    /** The spot 'dist' blocks in front of a target facing 'yawDeg'. */
    public static double[] ahead(double tx, double tz, double yawDeg, double dist) {
        double yaw = Math.toRadians(yawDeg);
        return new double[]{tx - Math.sin(yaw) * dist, tz + Math.cos(yaw) * dist};
    }

    /**
     * 'n' exit portals round (tx, tz), evenly spread from angle 'phase', alternating between the inner radius and the outer one so
     * they do not sit on one circle. With radii of 6 and 9 or more, no two of up to six are closer than {@link #MIN_EXIT_SEPARATION}.
     */
    public static List<double[]> exitPoints(int n, double tx, double tz, double rMin, double rMax, double phase) {
        List<double[]> out = new ArrayList<>();
        int count = Math.max(0, Math.min(8, n));
        for (int k = 0; k < count; k++) {
            double a = phase + k * Math.PI * 2 / count, r = k % 2 == 0 ? rMin : rMax;
            out.add(new double[]{tx + Math.cos(a) * r, tz + Math.sin(a) * r});
        }
        return out;
    }

    /** The angle of hop k of a warp barrage: each hop is {@link #HOP_STEP} radians (about 137 degrees) round from the last, so no two in a row are near each other. */
    public static double hopAngle(int k, double start) { return start + k * HOP_STEP; }

    /** The spot on a ring of radius r round (tx, tz) at 'angle'. */
    public static double[] onRing(double tx, double tz, double r, double angle) { return new double[]{tx + Math.cos(angle) * r, tz + Math.sin(angle) * r}; }

    /** The spot 'dist' blocks from (tx, tz) on the line from the target through the boss at (bx, bz): where he goes after a backstab. */
    public static double[] retreat(double bx, double bz, double tx, double tz, double dist) {
        double dx = bx - tx, dz = bz - tz, len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-6) { dx = 1; dz = 0; len = 1; }
        return new double[]{tx + dx / len * dist, tz + dz / len * dist};
    }

    /** 'n' entry portals in an arc 'forward' blocks ahead of the boss at (bx, bz) facing 'yawDeg', 'spacing' blocks apart. */
    public static List<double[]> entryArc(double bx, double bz, double yawDeg, int n, double spacing, double forward) {
        List<double[]> out = new ArrayList<>();
        double yaw = Math.toRadians(yawDeg);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = Math.cos(yaw), rz = Math.sin(yaw);
        for (int i = 0; i < Math.max(0, Math.min(8, n)); i++) {
            double lat = (i - (n - 1) / 2.0) * spacing, fw = forward + 0.35 * Math.abs(lat);
            out.add(new double[]{bx + rx * lat + fx * fw, bz + rz * lat + fz * fw});
        }
        return out;
    }

    /** The smallest distance between any two points of a list (infinity for fewer than two). */
    public static double minSeparation(List<double[]> pts) {
        double best = Double.POSITIVE_INFINITY;
        for (int i = 0; i < pts.size(); i++)
            for (int j = i + 1; j < pts.size(); j++)
                best = Math.min(best, Math.hypot(pts.get(i)[0] - pts.get(j)[0], pts.get(i)[1] - pts.get(j)[1]));
        return best;
    }

    // ---------------------------------------------------------------- how many, by power
    /** Portal pairs in a portal link: 2 at 40%, 3 at 60%, 4 at 80%, 5 at the Gates. */
    public static int linkPairs(int power) { return Math.max(2, Math.min(5, 1 + PowerCurve.stage(power))); }

    /** Waves of a portal link: one, and at the Gates a second one right behind the first (the portals chain). */
    public static int linkWaves(int power) { return PowerCurve.gatesOpen(power) ? 2 : 1; }

    /** Hops of a warp barrage: 3 at 40%, 4 at 60% and 80%, 5 at the Gates. */
    public static int warpHops(int power) { return Math.min(5, 2 + (PowerCurve.stage(power) + 2) / 2); }
}
