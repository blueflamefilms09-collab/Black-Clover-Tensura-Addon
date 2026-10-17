package com.newuniverse.nusmp.avatar.menu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Why: the owner asked for a 3D dice on the Identity page ("3D dice (existing stat roll)"). A real twenty-sided die is an icosahedron, and drawing one is
 * pure geometry: twelve points, twenty triangles, a rotation, a camera. This class does all of it without a single game class (so a unit test can check that
 * the solid is a true icosahedron, that the roll always ends with the right number facing the viewer, and that culling hides the far side) and the screen
 * only fills the triangles it returns. The roll is built backwards so that it can never end on the wrong face: the final orientation is chosen first (the
 * wanted number's face turned to the viewer with a vertex pointing up), and the spin is that orientation plus an extra turn about a random axis that
 * eases out to nothing. The numbering is a real d20's: opposite faces add up to 21. Nothing here decides a stat; the die only shows the number the server
 * already rolled.
 */
public final class DiceSpin {
    private static final double PHI = (1.0 + Math.sqrt(5.0)) / 2.0;
    /** The distance of the eye from the die's centre, in die radii; smaller = stronger perspective. */
    private static final double EYE = 5.5;

    private static final double[][] V = new double[12][];
    /** The 20 faces as vertex triples, counter-clockwise seen from outside. */
    private static final int[][] F = {
            {0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11},
            {1, 5, 9}, {5, 11, 4}, {11, 10, 2}, {10, 7, 6}, {7, 1, 8},
            {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9},
            {4, 9, 5}, {2, 4, 11}, {6, 2, 10}, {8, 6, 7}, {9, 8, 1}};
    private static final double[][] N = new double[20][];
    private static final int[] NUMBER = new int[20];
    private static final int[] FACE_OF_NUMBER = new int[21];

    static {
        double[][] raw = {{-1, PHI, 0}, {1, PHI, 0}, {-1, -PHI, 0}, {1, -PHI, 0}, {0, -1, PHI}, {0, 1, PHI}, {0, -1, -PHI}, {0, 1, -PHI},
                {PHI, 0, -1}, {PHI, 0, 1}, {-PHI, 0, -1}, {-PHI, 0, 1}};
        for (int i = 0; i < 12; i++) V[i] = norm(raw[i]);
        for (int f = 0; f < 20; f++) {
            double[] a = V[F[f][0]], b = V[F[f][1]], c = V[F[f][2]];
            N[f] = norm(cross(sub(b, a), sub(c, a)));
        }
        // a real d20: opposite faces add up to 21
        boolean[] done = new boolean[20];
        int next = 1;
        for (int f = 0; f < 20; f++) {
            if (done[f]) continue;
            int opp = oppositeOf(f);
            NUMBER[f] = next;
            NUMBER[opp] = 21 - next;
            done[f] = true;
            done[opp] = true;
            next++;
        }
        for (int f = 0; f < 20; f++) FACE_OF_NUMBER[NUMBER[f]] = f;
    }

    private static int oppositeOf(int f) {
        int best = f;
        double bd = 2.0;
        for (int g = 0; g < 20; g++) {
            double d = dot(N[f], N[g]);
            if (d < bd) { bd = d; best = g; }
        }
        return best;
    }

    // ---- small vector helpers (double[3])
    private static double[] sub(double[] a, double[] b) { return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]}; }

    private static double[] cross(double[] a, double[] b) { return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]}; }

    private static double dot(double[] a, double[] b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }

    private static double[] norm(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return l < 1.0e-12 ? new double[]{0, 0, 1} : new double[]{a[0] / l, a[1] / l, a[2] / l};
    }

    // ---- quaternions (w, x, y, z)
    private static double[] qAxis(double[] axis, double angle) {
        double[] a = norm(axis);
        double s = Math.sin(angle / 2.0);
        return new double[]{Math.cos(angle / 2.0), a[0] * s, a[1] * s, a[2] * s};
    }

    private static double[] qMul(double[] a, double[] b) {
        return new double[]{
                a[0] * b[0] - a[1] * b[1] - a[2] * b[2] - a[3] * b[3],
                a[0] * b[1] + a[1] * b[0] + a[2] * b[3] - a[3] * b[2],
                a[0] * b[2] - a[1] * b[3] + a[2] * b[0] + a[3] * b[1],
                a[0] * b[3] + a[1] * b[2] - a[2] * b[1] + a[3] * b[0]};
    }

    private static double[] qRot(double[] q, double[] v) {
        double[] p = {0, v[0], v[1], v[2]};
        double[] qc = {q[0], -q[1], -q[2], -q[3]};
        double[] r = qMul(qMul(q, p), qc);
        return new double[]{r[1], r[2], r[3]};
    }

    /** The shortest rotation taking unit vector {@code u} onto unit vector {@code v}. */
    private static double[] qFromTo(double[] u, double[] v) {
        double d = dot(u, v);
        if (d > 0.999999) return new double[]{1, 0, 0, 0};
        if (d < -0.999999) {                                   // opposite: turn half a circle about any axis perpendicular to u
            double[] ax = Math.abs(u[0]) < 0.9 ? cross(u, new double[]{1, 0, 0}) : cross(u, new double[]{0, 1, 0});
            return qAxis(ax, Math.PI);
        }
        double[] c = cross(u, v);
        double[] q = {1.0 + d, c[0], c[1], c[2]};
        double l = Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        return new double[]{q[0] / l, q[1] / l, q[2] / l, q[3] / l};
    }

    // ---- public geometry (for tests and the preview)
    public static int faces() { return 20; }

    public static int vertexCount() { return 12; }

    /** The unit vertex {@code i} of the solid in its own frame. */
    public static double[] vertex(int i) { return V[i].clone(); }

    /** The three vertex indices of face {@code f}, counter-clockwise from outside. */
    public static int[] face(int f) { return F[f].clone(); }

    /** The outward unit normal of face {@code f}. */
    public static double[] normal(int f) { return N[f].clone(); }

    /** The number printed on face {@code f} (1..20, opposite faces add up to 21). */
    public static int numberOf(int f) { return NUMBER[f]; }

    /** The face that carries number {@code n} (clamped into 1..20). */
    public static int faceOf(int n) { return FACE_OF_NUMBER[Math.max(1, Math.min(20, n))]; }

    // ================================================================ the roll
    /** One visible triangle in GUI pixels with its flat shade (0..1) and the face it belongs to. */
    public record Tri(float x0, float y0, float x1, float y1, float x2, float y2, float shade, int face) {}

    /** One visible edge in GUI pixels. */
    public record Seg(float x0, float y0, float x1, float y1) {}

    /** Everything the screen draws for one frame; {@code labelScale} is how squarely the front face looks at the viewer (1 = head on). */
    public record View(List<Tri> tris, List<Seg> edges, int frontFace, int frontNumber, float labelX, float labelY, float labelScale) {}

    private enum Mode { IDLE, FREE, ROLL }

    private final Random rng;
    private double age;
    private Mode mode = Mode.IDLE;
    private double[] base;                       // the orientation the current mode starts from (IDLE: where the die rests)
    private double modeStart = Double.NEGATIVE_INFINITY;
    private double[] axis = {0.3, 1, 0.2};      // FREE: the spin axis; ROLL: the axis of the turn that is still to come
    private double spinTotal;                    // ROLL: the angle still to turn at the start of the roll
    private double rollTicks = 1;
    private int number = 20;
    private double[] target;

    public DiceSpin(long seed) {
        rng = new Random(seed);
        target = targetFor(faceOf(number));
        base = target;
    }

    /** The orientation that shows face {@code f} to the viewer with one of its corners pointing up. */
    private static double[] targetFor(int f) {
        double[] q1 = qFromTo(N[f], new double[]{0, 0, 1});
        double[] v0 = qRot(q1, V[F[f][0]]);
        double theta = Math.atan2(v0[1], v0[0]);
        return qMul(qAxis(new double[]{0, 0, 1}, Math.PI / 2.0 - theta), q1);
    }

    private static double[] qConj(double[] q) { return new double[]{q[0], -q[1], -q[2], -q[3]}; }

    /**
     * The roll was asked for but the server has not answered yet: the die starts spinning at once, freely, so the button feels instant. {@link #roll} then
     * lands it on the number, continuing from wherever the die has got to.
     */
    public void tumble() {
        double[] now = orientation(0f);
        base = now;
        mode = Mode.FREE;
        modeStart = age;
        axis = new double[]{rng.nextDouble() * 2 - 1, rng.nextDouble() * 2 - 1 + 0.4, rng.nextDouble() * 2 - 1};
        if (dot(axis, axis) < 0.05) axis = new double[]{0.3, 1, 0.2};
    }

    /**
     * Starts a roll that ends with {@code value} (1..20) facing the viewer after {@code ticks}. It begins exactly where the die is now (so there is no jump
     * after {@link #tumble} or in the middle of an earlier roll) with a few extra turns that ease out to nothing.
     */
    public void roll(int value, int ticks) {
        double[] now = orientation(0f);
        number = Math.max(1, Math.min(20, value));
        target = targetFor(faceOf(number));
        double[] rel = qMul(now, qConj(target));                // the turn that takes the landing pose to the pose the die is in now
        if (rel[0] < 0) rel = new double[]{-rel[0], -rel[1], -rel[2], -rel[3]};
        double s = Math.sqrt(rel[1] * rel[1] + rel[2] * rel[2] + rel[3] * rel[3]);
        double angle = 2.0 * Math.atan2(s, rel[0]);
        if (s < 1.0e-9) {
            axis = new double[]{rng.nextDouble() * 2 - 1, rng.nextDouble() * 2 - 1 + 0.4, rng.nextDouble() * 2 - 1};
            if (dot(axis, axis) < 0.05) axis = new double[]{0.3, 1, 0.2};
            angle = 0.0;
        } else {
            axis = new double[]{rel[1] / s, rel[2] / s, rel[3] / s};
        }
        spinTotal = angle + (2 + rng.nextInt(2)) * 2.0 * Math.PI;      // whole extra turns about the same axis change nothing at the start and add the tumble
        mode = Mode.ROLL;
        modeStart = age;
        rollTicks = Math.max(8, ticks);
    }

    /** Advances the die's clock by {@code dt} ticks (one per client tick, or the frame time). */
    public void advance(float dt) { if (dt > 0f) age += dt; }

    /** True while the die is tumbling (waiting for the server's number) or on its way to the number. */
    public boolean rolling() { return mode == Mode.FREE || (mode == Mode.ROLL && age - modeStart < rollTicks); }

    /** True only while it spins freely, before the number is known. */
    public boolean tumbling() { return mode == Mode.FREE; }

    /** The number the last roll ends on (20 before any roll). */
    public int value() { return number; }

    /** 0..1: how far the current roll has come (1 when none is running; 0 while tumbling). */
    public double progress(float partial) {
        if (mode == Mode.FREE) return 0.0;
        if (mode != Mode.ROLL) return 1.0;
        return ForgeMotion.clamp01((float) ((age + partial - modeStart) / rollTicks));
    }

    /** A small pop in size right after the die lands: 1.0 normally, up to about 1.12 for ten ticks. */
    public float pop(float partial) {
        if (mode != Mode.ROLL) return 1f;
        double since = age + partial - (modeStart + rollTicks);
        if (since < 0 || since > 10) return 1f;
        return 1f + 0.12f * (float) Math.sin(Math.PI * since / 10.0);
    }

    private static final double FREE_SPEED = 0.55;       // radians per tick while tumbling

    private double[] orientation(float partial) {
        double t = age + partial;
        switch (mode) {
            case FREE:
                return qMul(qAxis(axis, FREE_SPEED * Math.max(0.0, t - modeStart)), base);
            case ROLL: {
                if (t < modeStart + rollTicks) {
                    double u = ForgeMotion.clamp01((float) ((t - modeStart) / rollTicks));
                    double remaining = spinTotal * (1.0 - ForgeMotion.easeOutCubic((float) u));
                    return qMul(qAxis(axis, remaining), target);
                }
                return sway(target, Math.min(1.0, (t - modeStart - rollTicks) / 20.0), t);       // at rest the die breathes a little, more the longer it has rested
            }
            default:
                return sway(base, 1.0, t);
        }
    }

    /** A gentle sway kept under about 10 degrees in total: a neighbouring face is 41.8 degrees away, so the landed number can never be pushed off the front. */
    private static double[] sway(double[] q, double amount, double t) {
        double[] wy = qAxis(new double[]{0, 1, 0}, 0.13 * amount * Math.sin(t * 0.045));
        double[] wx = qAxis(new double[]{1, 0, 0}, 0.10 * amount * Math.sin(t * 0.031 + 1.0));
        return qMul(wy, qMul(wx, q));
    }

    private static final double[] LIGHT = norm(new double[]{-0.45, 0.75, 0.55});

    /** The frame at {@code partial} ticks past the last {@link #advance}: front-facing triangles far to near, the visible edges, and where the number goes. */
    public View view(float partial, float cx, float cy, float radius) {
        double[] q = orientation(partial);
        double[][] rv = new double[12][];
        for (int i = 0; i < 12; i++) rv[i] = qRot(q, V[i]);
        List<Tri> tris = new ArrayList<>();
        List<Seg> segs = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        int front = 0;
        double bestFacing = -2;
        double[] eye = {0, 0, EYE};
        for (int f = 0; f < 20; f++) {
            double[] n = qRot(q, N[f]);
            double[] a = rv[F[f][0]], b = rv[F[f][1]], c = rv[F[f][2]];
            double[] centre = {(a[0] + b[0] + c[0]) / 3, (a[1] + b[1] + c[1]) / 3, (a[2] + b[2] + c[2]) / 3};
            double facing = dot(n, norm(sub(eye, centre)));
            if (facing > bestFacing) { bestFacing = facing; front = f; }
            if (facing <= 0.0) continue;                                   // the far side of a solid is hidden by its near side
            double lit = Math.max(0.0, dot(n, LIGHT));
            float shade = (float) (0.30 + 0.70 * (0.78 * lit + 0.22 * facing));
            float[] pa = screen(a, cx, cy, radius), pb = screen(b, cx, cy, radius), pc = screen(c, cx, cy, radius);
            tris.add(new Tri(pa[0], pa[1], pb[0], pb[1], pc[0], pc[1], Math.min(1f, shade), f));
            int[][] es = {{F[f][0], F[f][1]}, {F[f][1], F[f][2]}, {F[f][2], F[f][0]}};
            for (int[] e : es) {
                long key = ((long) Math.min(e[0], e[1]) << 8) | Math.max(e[0], e[1]);
                if (!seen.add(key)) continue;
                float[] p = screen(rv[e[0]], cx, cy, radius), r = screen(rv[e[1]], cx, cy, radius);
                segs.add(new Seg(p[0], p[1], r[0], r[1]));
            }
        }
        tris.sort((x, y) -> Float.compare(depthOf(x, rv), depthOf(y, rv)));
        double[] fa = rv[F[front][0]], fb = rv[F[front][1]], fc = rv[F[front][2]];
        double[] fcentre = {(fa[0] + fb[0] + fc[0]) / 3, (fa[1] + fb[1] + fc[1]) / 3, (fa[2] + fb[2] + fc[2]) / 3};
        float[] lp = screen(fcentre, cx, cy, radius);
        return new View(tris, segs, front, NUMBER[front], lp[0], lp[1], (float) Math.max(0.0, bestFacing));
    }

    private static float depthOf(Tri t, double[][] rv) {
        double[][] vs = {rv[F[t.face()][0]], rv[F[t.face()][1]], rv[F[t.face()][2]]};
        return (float) ((vs[0][2] + vs[1][2] + vs[2][2]) / 3.0);
    }

    /** Perspective: a point nearer the eye is drawn larger; y is flipped because the screen's y grows downwards. */
    private static float[] screen(double[] p, float cx, float cy, float radius) {
        double k = EYE / (EYE - p[2]);
        return new float[]{(float) (cx + p[0] * k * radius), (float) (cy - p[1] * k * radius)};
    }
}
