package com.newuniverse.nusmp.labyrinth.mega.macro;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntToDoubleFunction;

/**
 * Why: the Spire's staircase and the Chasm's ramp are the same object seen from two sides: a walkway of fixed width that winds round a vertical axis, one
 * half block of height per block of arc, with landings, a railing on the open side and headroom above it. {@link HelixPath} computes WHERE the walkway
 * is; this class says WHAT every voxel near it is (the walking surface, the support under it, the carved headroom, the curb and the rail on the open
 * side), so both landmark models ask the same question and only choose the materials. The answers are deterministic and independent of the noise and of
 * the Wave Function Collapse tiles, which is the whole point: the road to each gate is a constraint the rest of the landmark is built around, so it cannot be
 * blocked by a random tile. Pure: no Minecraft import.
 *
 * <p>Geometry. The walkway's centre radius at an arc step is {@code path.radius(step)}; the walking columns are those whose radius is within half the
 * width of it, and the rail column is the one just inside (towards the axis). The walking surface of a step is {@link #topHalf} half blocks up (an even
 * number is a full block, an odd one a bottom slab).
 */
final class Spiral {
    /** What a voxel is, relative to the walkway. */
    static final int NONE = 0, WALK = 1, SUPPORT = 2, HEAD = 3, CURB = 4, RAIL = 5;

    final HelixPath path;
    final int width;
    final boolean ascending;
    final int baseY;
    final int railFromQ;
    final int headroom;
    /** How far (blocks) from the walkway's centre radius a column still belongs to it: half the width plus the rail column. */
    final double window;

    Spiral(HelixPath path, int width, boolean ascending, int baseY, int railFromQ, int headroom) {
        this.path = path;
        this.width = width;
        this.ascending = ascending;
        this.baseY = baseY;
        this.railFromQ = railFromQ;
        this.headroom = headroom;
        this.window = width / 2.0 + 1.0;
    }

    /** Scratch space for the laps of the path that pass through one column (a column is crossed once per revolution). */
    static final class Hits {
        final int[] step = new int[6];
        final int[] lap = new int[6];
        int n;
    }

    /** Fills {@code out} with the arc step of every revolution of the path that passes through the column at (angle, radius). */
    void find(double angle, double r, Hits out) {
        out.n = 0;
        double sweep = path.sweepOf(angle);
        int from = 0;
        while (out.n < out.step.length && from < 8) {
            int enc = path.next(sweep, r, from, window);
            if (enc < 0) break;
            out.step[out.n] = enc >> 3;
            out.lap[out.n] = enc & 7;
            out.n++;
            from = (enc & 7) + 1;
        }
    }

    /** The height of the walking surface of a step, in half blocks (2 x the y of the surface). */
    int topHalf(int step) {
        int q = path.q(step);
        return ascending ? 2 * baseY + q : 2 * baseY - q;
    }

    /** The y of the block row that forms the walking surface (a full block, or the cell that holds the bottom slab). */
    int surfaceRow(int step) { return Math.floorDiv(topHalf(step) - 1, 2); }

    /** Is the walking surface of this step a bottom slab (half a block) rather than a full block? */
    boolean isSlab(int step) { return (topHalf(step) & 1) != 0; }

    /** Does the open side of this step carry a rail? Not the first few steps (the walkway is at the level of the floor there), not a railing-free stop. */
    boolean railed(int step) { return path.q(step) >= railFromQ && !path.isOpen(step); }

    /** The part a voxel at {@code y} plays for the walkway step that passes through its column at radius {@code r}. */
    int part(int step, double r, int y) {
        double rel = r - path.radius(step);
        double half = width / 2.0;
        int yb = surfaceRow(step);
        boolean railed = railed(step);
        if (rel >= -half && rel < half || (!railed && rel >= -half - 1 && rel < -half)) {
            if (y == yb) return WALK;
            if (y == yb - 1 || y == yb - 2) return SUPPORT;
            if (y > yb && y <= yb + headroom) return HEAD;
            return NONE;
        }
        if (railed && rel >= -half - 1 && rel < -half) {
            if (y == yb) return CURB;
            if (y == yb - 1 || y == yb - 2) return SUPPORT;
            if (y == yb + 1) return RAIL;
            if (y > yb && y <= yb + headroom) return HEAD;
            return NONE;
        }
        return NONE;
    }

    /** Is the column (at radius {@code r}) the rail column of this step, and does the step carry a rail? */
    boolean inRailColumn(int step, double r) {
        double rel = r - path.radius(step);
        double half = width / 2.0;
        return railed(step) && rel >= -half - 1 && rel < -half;
    }

    /** The middle angle of the landing at half step {@code q} (the position angle of its centre), or NaN when there is no such landing. */
    double landingMid(int q) {
        int[] ab = path.landingSteps(q);
        if (ab == null) return Double.NaN;
        double a = path.angleOf(ab[0]);
        double b = path.angleOf(ab[1]) + path.dir / Math.max(1.0, path.radius(ab[1]));
        return (a + b) / 2.0;
    }

    /** The first and the last arc step of a landing, or null. */
    int[] landingSteps(int q) { return path.landingSteps(q); }

    /**
     * Builds the path of a spiral: landings at the given half-step heights (the first at {@code q = 0}), a railing-free final stop at {@code qEnd}, and a
     * start angle chosen so that the landing at q = 0 is centred on {@code doorAngle}; {@code arcInset} is passed on to {@link HelixPath} (the arc is measured that far
     * inside the centre of the walkway: half its width, so every lattice neighbour of the walkway is at most one wedge away). Why the two passes: where the first landing lies depends on the
     * start angle only by an offset, so the path is built once at angle 0 to measure the landing's middle and again shifted.
     */
    static HelixPath build(int dir, double doorAngle, int qEnd, IntToDoubleFunction radiusAt, int[] landingQ, int landingLen, int endLen, double arcInset) {
        HelixPath trial = make(dir, 0.0, qEnd, radiusAt, landingQ, landingLen, endLen, arcInset);
        int[] ab = trial.landingSteps(0);
        double a = trial.angleOf(ab[0]);
        double b = trial.angleOf(ab[1]) + trial.dir / Math.max(1.0, trial.radius(ab[1]));
        double mid = (a + b) / 2.0;
        return make(dir, Geo.wrapPi(doorAngle - mid), qEnd, radiusAt, landingQ, landingLen, endLen, arcInset);
    }

    private static HelixPath make(int dir, double startAngle, int qEnd, IntToDoubleFunction radiusAt, int[] landingQ, int landingLen, int endLen, double arcInset) {
        List<HelixPath.Stop> stops = new ArrayList<>();
        for (int q : landingQ) stops.add(new HelixPath.Stop(q, (landingLen - 0.5) / (radiusAt.applyAsDouble(q) - arcInset), false));
        stops.add(new HelixPath.Stop(qEnd, (endLen - 0.5) / (radiusAt.applyAsDouble(qEnd) - arcInset), true));
        return new HelixPath(dir, startAngle, qEnd, radiusAt, stops, arcInset);
    }
}
