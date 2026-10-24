package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.perf.BoundedCache;
import com.newuniverse.nusmp.perf.PerfMath;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * The maths of the Mana Method rune array (pure: JOML and plain numbers, no Minecraft class, so it runs on a server and in a unit test; the CLIENT draws with it in
 * {@code client.manamethod.RuneClient}). The rune is a flat figure of concentric rings, a ring of rune symbols and a star polygon:
 * <ol>
 *   <li>{@link #geometry}: the figure is plotted with {@code sin} / {@code cos} in a 2D plane of radius 1 (x right, y up, the plane's normal is +z). Built once per
 *       (rings, symbols) and cached ({@link BoundedCache}); the points are ordered ring by ring from the outside in, then the symbols, then the star, so a rune can DRAW ITSELF
 *       by showing a growing prefix ({@link #visible}).</li>
 *   <li>{@link #faceQuaternion}: a quaternion that turns the plane so its normal points back at the caster (minus the look vector) with no roll, so the symbols stay upright.</li>
 *   <li>{@link #frame} / {@link #toWorld}: the plane's two axes in the world, then every point is {@code centre + ux * x * R + uy * y * R} (a few multiplications, no quaternion per
 *       point, no allocation per point).</li>
 *   <li>{@link #strideFor} / {@link #newPoints}: each point is spawned once, when the rune draws it; the quality and the distance thin the figure by a stride, the client
 *       budget caps what a tick may spawn.</li>
 * </ol>
 * Complexity 1..{@value #MAX_COMPLEXITY}: that many rings and {@code 4 + 2 * complexity} symbols. At most {@value #MAX_POINTS} points per rune.
 */
public final class RuneGeneratorUtil {
    private RuneGeneratorUtil() {}

    public static final int MIN_COMPLEXITY = 1, MAX_COMPLEXITY = 5, MAX_POINTS = 400;
    /** Points per unit of ring length on the unit circle (a ring of radius 1 has {@code 2 pi * 6} points). */
    public static final double RING_DENSITY = 6.0;
    /** At most this many points of one rune are spawned in one client tick, and at most this many runes are drawn at once. */
    public static final int MAX_PER_TICK = 40, MAX_RUNES_DRAWN = 6;
    /** A rune farther than this from the camera is not drawn at all; within {@link #LOD_NEAR} it is at full detail. */
    public static final double CULL_DISTANCE = 64.0, LOD_NEAR = 24.0;

    /** What a rune looks like: radius in blocks, complexity (rings; symbols follow), the particle of the rings and the particle of the symbols (catalog ids), the colour. */
    public record Params(double radius, int complexity, String ringParticle, String glyphParticle, int tint) {
        public Params {
            radius = Double.isNaN(radius) ? 1.0 : Math.max(0.3, Math.min(4.0, radius));
            complexity = Math.max(MIN_COMPLEXITY, Math.min(MAX_COMPLEXITY, complexity));
            ringParticle = ringParticle == null ? RuneElement.RING_PARTICLE : ringParticle;
            glyphParticle = glyphParticle == null ? RuneElement.FIRE.glyph : glyphParticle;
        }

        public int rings() { return complexity; }

        public int symbols() { return 4 + 2 * complexity; }
    }

    /** The general entry: a radius, a complexity and a particle for the symbols. */
    public static Params of(double radius, int complexity, String glyphParticle, int tint) {
        return new Params(radius, complexity, RuneElement.RING_PARTICLE, glyphParticle, tint);
    }

    /** The look of a spell's rune: bigger with more modifiers, one more ring for True Magic. */
    public static Params paramsFor(RuneSpec s) {
        int mods = RuneMods.count(s.mods());
        int complexity = 2 + mods + (s.trueMagic() ? 1 : 0);
        double radius = 1.15 + 0.2 * mods + (s.trueMagic() ? 0.25 : 0.0);
        return of(radius, complexity, s.element().glyph, s.element().tint);
    }

    // ---------------------------------------------------------------- the 2D figure
    public static final byte KIND_RING = 0, KIND_GLYPH = 1, KIND_LINE = 2;

    /** The cached figure on the unit circle: x and y per point, the kind of each, the ring it belongs to (symbols and the star: the ring count), and where each stage ends. */
    public static final class Geometry {
        public final int count;
        public final float[] x, y;
        public final byte[] kind;
        public final byte[] ring;
        /** stageEnd[s] = the number of points shown when stage s is complete: stages 0..rings-1 are the rings (outer first), then the symbols, then the star. */
        public final int[] stageEnd;

        Geometry(float[] x, float[] y, byte[] kind, byte[] ring, int[] stageEnd) {
            this.count = x.length;
            this.x = x;
            this.y = y;
            this.kind = kind;
            this.ring = ring;
            this.stageEnd = stageEnd;
        }
    }

    private static final BoundedCache<Integer, Geometry> CACHE = new BoundedCache<>(64);

    /** The figure of {@code p} on the unit circle (cached: the same rings and symbols give the same instance). */
    public static Geometry geometry(Params p) {
        return CACHE.get(p.rings() * 100 + p.symbols(), k -> build(p.rings(), p.symbols()));
    }

    public static int cacheSize() { return CACHE.size(); }

    static Geometry build(int rings, int symbols) {
        int cap = MAX_POINTS;
        float[] xs = new float[cap], ys = new float[cap];
        byte[] kinds = new byte[cap], rs = new byte[cap];
        int[] stageEnd = new int[rings + 2];
        int n = 0;
        // the rings, outer first: ring k has radius 1 - 0.17 k and 2 pi r * density points
        double[] radii = new double[rings];
        for (int k = 0; k < rings; k++) {
            radii[k] = 1.0 - 0.17 * k;
            int pts = Math.max(12, (int) Math.round(2 * Math.PI * radii[k] * RING_DENSITY));
            double phase = (k % 2) * Math.PI / pts;                                // inner rings are offset by half a step so the dots do not line up
            for (int i = 0; i < pts && n < cap; i++, n++) {
                double a = phase + 2 * Math.PI * i / pts;
                xs[n] = (float) (radii[k] * Math.cos(a));
                ys[n] = (float) (radii[k] * Math.sin(a));
                kinds[n] = KIND_RING;
                rs[n] = (byte) k;
            }
            stageEnd[k] = n;
        }
        // the symbols: one ring of marks in the gap between the first two rings (or just inside the only ring)
        double gap = rings >= 2 ? (radii[0] + radii[1]) / 2.0 : radii[0] - 0.09;
        for (int i = 0; i < symbols && n < cap; i++, n++) {
            double a = 2 * Math.PI * i / symbols + Math.PI / 2;                    // the first symbol at the top
            xs[n] = (float) (gap * Math.cos(a));
            ys[n] = (float) (gap * Math.sin(a));
            kinds[n] = KIND_GLYPH;
            rs[n] = (byte) rings;
        }
        stageEnd[rings] = n;
        // the star: a {m/2} polygon (a plain polygon for m < 5) inside the innermost ring, its edges drawn with 4 points each
        int m = Math.max(3, symbols / 2);
        int skip = m >= 5 ? 2 : 1;
        double rs0 = Math.max(0.2, radii[rings - 1] - 0.14);
        int edgePoints = 4;
        for (int v = 0; v < m; v++) {
            double a0 = Math.PI / 2 + 2 * Math.PI * v / m, a1 = Math.PI / 2 + 2 * Math.PI * ((v + skip) % m) / m;
            double x0 = rs0 * Math.cos(a0), y0 = rs0 * Math.sin(a0), x1 = rs0 * Math.cos(a1), y1 = rs0 * Math.sin(a1);
            for (int e = 0; e < edgePoints && n < cap; e++, n++) {
                double t = e / (double) edgePoints;
                xs[n] = (float) (x0 + (x1 - x0) * t);
                ys[n] = (float) (y0 + (y1 - y0) * t);
                kinds[n] = KIND_LINE;
                rs[n] = (byte) rings;
            }
        }
        stageEnd[rings + 1] = n;
        return new Geometry(java.util.Arrays.copyOf(xs, n), java.util.Arrays.copyOf(ys, n), java.util.Arrays.copyOf(kinds, n), java.util.Arrays.copyOf(rs, n), stageEnd);
    }

    /**
     * How many points are visible when the rune is {@code progress} (0..1) drawn: the figure draws itself ring by ring from the outside, then the symbols, then the star; the
     * prefix grows smoothly (a ring appears as a sweeping arc).
     */
    public static int visible(Geometry g, double progress) {
        double p = Double.isNaN(progress) ? 0.0 : Math.max(0.0, Math.min(1.0, progress));
        return (int) Math.ceil(p * g.count);
    }

    // ---------------------------------------------------------------- facing the caster
    /**
     * The rotation that takes the plane (normal +z, up +y) to a plane whose normal is {@code -look} (it faces the caster who looks along {@code look}), turned by yaw about the
     * world's vertical axis and then pitch about the plane's own x axis: no roll, so the plane's +x stays horizontal and the symbols stay upright. A zero or NaN look gives the
     * identity facing (+z).
     */
    public static Quaterniond faceQuaternion(double lx, double ly, double lz) {
        double len = Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (!(len > 1e-9)) return new Quaterniond();
        double nx = -lx / len, ny = -ly / len, nz = -lz / len;                      // the plane's normal: back toward the caster
        double pitch = -Math.asin(Math.max(-1.0, Math.min(1.0, ny)));
        double yaw = Math.atan2(nx, nz);
        return new Quaterniond().rotateY(yaw).rotateX(pitch);
    }

    /** The plane in the world: its centre, its x axis, its y axis and its normal (unit vectors). */
    public record Frame(double cx, double cy, double cz, double ux, double uy, double uz, double vx, double vy, double vz, double nx, double ny, double nz) {}

    /** The world frame of a plane rotated by {@code q} and centred at (cx, cy, cz). */
    public static Frame frame(Quaterniond q, double cx, double cy, double cz) {
        Vector3d u = q.transform(new Vector3d(1, 0, 0)), v = q.transform(new Vector3d(0, 1, 0)), n = q.transform(new Vector3d(0, 0, 1));
        return new Frame(cx, cy, cz, u.x, u.y, u.z, v.x, v.y, v.z, n.x, n.y, n.z);
    }

    /** The frame of a rune at (cx, cy, cz) that faces a caster looking along (lx, ly, lz). */
    public static Frame frameFacing(double cx, double cy, double cz, double lx, double ly, double lz) {
        return frame(faceQuaternion(lx, ly, lz), cx, cy, cz);
    }

    /**
     * The world position of point {@code i} of the figure at radius {@code radius}, with the symbols and the star turned by {@code spin} radians about the normal (the rings are
     * circles, a turn changes nothing for them). Writes x, y, z to {@code out}. No allocation.
     */
    public static void toWorld(Geometry g, int i, double radius, Frame f, double spin, double[] out) {
        double x = g.x[i], y = g.y[i];
        if (g.kind[i] != KIND_RING && spin != 0.0) {
            double dir = (g.ring[i] & 1) == 0 ? 1.0 : -1.0;
            double c = Math.cos(spin * dir), s = Math.sin(spin * dir);
            double rx = x * c - y * s, ry = x * s + y * c;
            x = rx;
            y = ry;
        }
        x *= radius;
        y *= radius;
        out[0] = f.cx + f.ux * x + f.vx * y;
        out[1] = f.cy + f.uy * x + f.vy * y;
        out[2] = f.cz + f.uz * x + f.vz * y;
    }

    // ---------------------------------------------------------------- the budget
    /**
     * Every point of the figure is spawned ONCE, when the rune draws it (the custom rune particles live 24 to 36 ticks, longer than the rune charges). The detail (0..1: effect
     * quality x distance) thins the figure by a stride: every point at detail 0.99 or more, every second at 0.7, every third at 0.45, every fourth below; 0 or less draws nothing.
     */
    public static int strideFor(float detail) {
        if (!(detail > 0f)) return 0;
        if (detail >= 0.99f) return 1;
        if (detail >= 0.7f) return 2;
        return detail >= 0.45f ? 3 : 4;
    }

    /** How many points a tick spawns when the visible prefix grows from {@code prev} to {@code now} at this stride (the points whose index is a multiple of the stride), capped by {@link #MAX_PER_TICK}. */
    public static int newPoints(int prev, int now, int stride) {
        if (stride <= 0 || now <= prev) return 0;
        int first = ((Math.max(0, prev) + stride - 1) / stride) * stride;            // the first multiple of the stride at or after prev
        if (first >= now) return 0;
        return Math.min(MAX_PER_TICK, (now - 1 - first) / stride + 1);
    }

    /** The index of the {@code j}-th new point of a tick (j from 0 to {@link #newPoints} - 1). */
    public static int newPointIndex(int prev, int stride, int j) {
        int first = ((Math.max(0, prev) + stride - 1) / stride) * stride;
        return first + j * stride;
    }

    /** The ring that flares when the rune is released: the outer ring (stage 0) of the figure, its points. */
    public static int flareCount(Geometry g) { return g.stageEnd[0]; }

    /** The detail of a rune at this squared distance from the camera: 0 beyond {@link #CULL_DISTANCE}, 1 within {@link #LOD_NEAR} (stepped, see {@link PerfMath#lodDetailSq}). */
    public static float distanceDetailSq(double distSq, boolean own) {
        return own ? 1f : PerfMath.lodDetailSq(distSq, LOD_NEAR, CULL_DISTANCE);
    }
}
