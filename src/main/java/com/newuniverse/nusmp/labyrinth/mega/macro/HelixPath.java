package com.newuniverse.nusmp.labyrinth.mega.macro;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntToDoubleFunction;

/**
 * Why: both landmarks are reached by a long spiral walkway (the Spire's staircase climbs to the Holy gate, the Chasm's ramp descends to the Abyssal
 * gate), and the owner's rule is that those roads must ALWAYS exist whatever the noise or the Wave Function Collapse does. So the road is not
 * produced by any random process: it is this small deterministic table, computed from a few numbers, and the landmark models consult it BEFORE
 * they consult the noise or a tile (it is the "pre-collapsed" path). The path advances one BLOCK of arc per step and one HALF block of height per
 * block of arc (rise 1 per 2 blocks, the brief's number; a bottom slab is half a block, so the walkway is a ramp of alternating full blocks and
 * slabs that a player walks up without jumping); a half-step index {@code q} counts the half blocks climbed (or descended) from the start. At
 * the requested stops the height is held for a while (a landing: a flat stretch wide enough to hold a whole chamber door) and the last stop can
 * be flagged open (no railing: the walkway runs into the gate platform). Because the table is indexed by swept ANGLE, a column of the world is
 * classified by one {@code atan2} and a binary search, and several revolutions can share a column (they are far apart in height).
 *
 * <p>All arithmetic is plain {@code double} addition and division (no transcendental function), so the table is identical on every JVM. Pure: no
 * Minecraft import.
 */
public final class HelixPath {
    /** A pause in the climb: hold half-step {@code q} for at least {@code holdAngle} radians of sweep; {@code open} flags a railing-free stop. */
    public record Stop(int q, double holdAngle, boolean open) {}

    public final int dir;
    public final double startAngle;
    public final int qEnd;
    private final double[] theta;
    private final int[] qOf;
    private final double[] radius;
    private final boolean[] open;
    private final boolean[] landing;
    private final int steps;
    /** Total angle swept by the whole path (radians). */
    public final double thetaEnd;

    /**
     * @param dir +1 = the angle grows along the path (counter-clockwise seen from above), -1 = it shrinks
     * @param startAngle the position angle (atan2 of dz, dx) of the first step
     * @param qEnd the last half-step index (the path ends there)
     * @param radiusAt the radius of the centre of the walkway at a half-step index
     * @param stops landings, ascending by {@code q}; a stop at {@code qEnd} is the final platform
     */
    public HelixPath(int dir, double startAngle, int qEnd, IntToDoubleFunction radiusAt, List<Stop> stops) {
        this.dir = dir >= 0 ? 1 : -1;
        this.startAngle = startAngle;
        this.qEnd = qEnd;
        List<double[]> rows = new ArrayList<>();
        double t = 0;
        int next = 0;
        for (int q = 0; q <= qEnd; q++) {
            Stop stop = next < stops.size() && stops.get(next).q() == q ? stops.get(next) : null;
            double hold = stop == null ? 0 : stop.holdAngle();
            double acc = 0;
            do {
                double r = radiusAt.applyAsDouble(q);
                rows.add(new double[] {t, q, r, stop != null && stop.open() ? 1 : 0, stop != null ? 1 : 0});
                double dt = 1.0 / r;
                t += dt;
                acc += dt;
            } while (acc < hold);
            if (stop != null) next++;
        }
        this.steps = rows.size();
        this.theta = new double[steps];
        this.qOf = new int[steps];
        this.radius = new double[steps];
        this.open = new boolean[steps];
        this.landing = new boolean[steps];
        for (int i = 0; i < steps; i++) {
            double[] row = rows.get(i);
            theta[i] = row[0];
            qOf[i] = (int) row[1];
            radius[i] = row[2];
            open[i] = row[3] != 0;
            landing[i] = row[4] != 0;
        }
        this.thetaEnd = t;
    }

    public int steps() { return steps; }

    /** The half-step index (height) of an arc step. */
    public int q(int step) { return qOf[step]; }

    /** The radius of the walkway centre at an arc step. */
    public double radius(int step) { return radius[step]; }

    /** True when the step belongs to the final railing-free platform. */
    public boolean isOpen(int step) { return open[step]; }

    /** True when the step belongs to a landing (a held height). */
    public boolean isLanding(int step) { return landing[step]; }

    /** The swept angle at the start of an arc step. */
    public double thetaAt(int step) { return theta[step]; }

    /** The position angle (atan2 convention, not normalised) of the start of an arc step. */
    public double angleOf(int step) { return startAngle + dir * theta[step]; }

    /** The swept angle of a position angle, in [0, 2*pi): how far round from the start the column lies in the first revolution. */
    public double sweepOf(double positionAngle) {
        double rel = dir * (positionAngle - startAngle);
        double twoPi = 2.0 * Math.PI;
        rel -= twoPi * Math.floor(rel / twoPi);
        return rel >= twoPi ? 0.0 : rel;
    }

    /**
     * The next arc step, from revolution {@code fromK} on, whose angular interval holds the column and whose walkway centre is within
     * {@code window} blocks of the column's radius; encoded as {@code (step << 3) | k}, or -1 when there is none. Call again with {@code k + 1}
     * to see the next lap (laps are far apart in height, but the caller decides by height which one it is in).
     */
    public int next(double sweep, double r, int fromK, double window) {
        double twoPi = 2.0 * Math.PI;
        for (int k = fromK; k < 8; k++) {
            double t = sweep + twoPi * k;
            if (t >= thetaEnd) return -1;
            int lo = 0, hi = steps - 1;
            while (lo < hi) {
                int mid = (lo + hi + 1) >>> 1;
                if (theta[mid] <= t) lo = mid; else hi = mid - 1;
            }
            if (Math.abs(r - radius[lo]) <= window) return (lo << 3) | k;
        }
        return -1;
    }

    /** The first arc step of the final railing-free platform, or -1. */
    public int firstOpenStep() {
        for (int i = 0; i < steps; i++) if (open[i]) return i;
        return -1;
    }

    /** The first and last arc steps of the landing at half-step {@code q}, or null. */
    public int[] landingSteps(int q) {
        int a = -1, b = -1;
        for (int i = 0; i < steps; i++) if (landing[i] && qOf[i] == q) { if (a < 0) a = i; b = i; }
        return a < 0 ? null : new int[] {a, b};
    }
}
