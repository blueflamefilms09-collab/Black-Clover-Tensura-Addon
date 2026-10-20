package com.newuniverse.nusmp.client.labyrinth.rift;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: the shape of a MEGA-RARE tear, as plain numbers, made once per seed and then only read. A mega-rare rift is not the plain lens of the other tears: it is a jagged vertical slit of the world,
 * about two blocks wide and four and a half tall, with a ragged edge, long forked strands of light leaping from it, and a core that shows a sky of the pocket's colour. All of that geometry is a function of
 * the rift's seed, so it is built ONCE ({@link #of}, kept in a small least-recently-used cache) into flat float arrays, and the renderer ({@link MegaRiftRenderer}) only walks the arrays every frame:
 * no object is made per frame (the per-frame wobble of a strand is a hash of integers, {@link #hash}), which is the point. The outline is star-shaped about the centre by construction (a polar
 * outline whose radius never falls under {@link #MIN_RADIUS} of the base ellipse), so a triangle fan from the centre fills it without overlap. The detail follows the distance ({@link #lod}): the nearer
 * the camera the more layers, planes and strands, and past {@link #CULL_DISTANCE} nothing is drawn; {@link #vertices} is the vertex count of a frame at a level of detail and never exceeds
 * {@link #BUDGET}. No Minecraft type is used here, so a unit test pins the arithmetic.
 */
public final class TearGeometry {
    /** Points of the outline, strands, and points per strand. */
    public static final int N = 28, STRANDS = 12, STRAND_POINTS = 7;
    /** Half width and half height of the base ellipse of the tear, in blocks, and the lowest the outline's radius may fall to as a share of it. */
    public static final float RX = 0.95f, RY = 2.25f, MIN_RADIUS = 0.55f;
    /** The height of the tear's centre above the block's floor. */
    public static final float CENTRE_Y = RY + 0.15f;
    /** The most vertices one tear may draw in a frame, and the distances (blocks) of the levels of detail. */
    public static final int BUDGET = 1536;
    public static final float NEAR = 18f, MID = 40f, CULL_DISTANCE = 84f;
    /** The levels of detail: 0 = full, 1 = middle, 2 = far, 3 = not drawn. */
    public static final int FULL = 0, MIDDLE = 1, FAR = 2, NONE = 3;

    /** Outline points (x, y), y measured from the tear's floor; the centre is (0, {@link #CENTRE_Y}). */
    public final float[] ox = new float[N], oy = new float[N];
    /** Strand base points, flattened: strand s, point k at index (s * STRAND_POINTS + k); the first point lies on the outline, the strand leaves it outward. */
    public final float[] sx = new float[STRANDS * STRAND_POINTS], sy = new float[STRANDS * STRAND_POINTS];
    /** The width of each strand at its root (blocks), and how far along the outline it starts (index of the outline point). */
    public final float[] sw = new float[STRANDS];
    public final int[] root = new int[STRANDS];
    public final long seed;

    private TearGeometry(long seed) {
        this.seed = seed;
        long h = seed * 0x9E3779B97F4A7C15L + 0x51ED270B1L;
        for (int i = 0; i < N; i++) {
            double a = 2.0 * Math.PI * i / N;
            h = mix(h + i);
            float jag = 0.62f + 0.38f * unit(h);                                   // ragged: 0.62 .. 1.0 of the ellipse
            if ((i & 1) == 1) jag = Math.max(MIN_RADIUS, jag * 0.82f);             // every other point sits back: a saw edge
            double sy_ = Math.sin(a), sx_ = Math.cos(a);
            float stretch = (float) (1.0 + 0.28 * Math.pow(Math.abs(sy_), 6.0));    // the top and the bottom are drawn out into points
            ox[i] = (float) (sx_ * RX * jag * (1.0 - 0.25 * Math.abs(sy_)));
            oy[i] = Math.max(0.02f, CENTRE_Y + (float) (sy_ * RY * jag * stretch));          // the point at the bottom rests on the floor, it does not sink into it
        }
        for (int s = 0; s < STRANDS; s++) {
            h = mix(h + 1000 + s);
            int i = (int) (unit(h) * N) % N;
            root[s] = i;
            h = mix(h);
            float len = 0.7f + 1.5f * unit(h);                                      // 0.7 .. 2.2 blocks
            double nx = ox[i], ny = oy[i] - CENTRE_Y, l = Math.sqrt(nx * nx + ny * ny);
            nx /= Math.max(l, 1e-4);
            ny /= Math.max(l, 1e-4);
            float x = ox[i], y = oy[i];
            for (int k = 0; k < STRAND_POINTS; k++) {
                sx[s * STRAND_POINTS + k] = x;
                sy[s * STRAND_POINTS + k] = y;
                h = mix(h);
                float side = (unit(h) - 0.5f) * 0.55f;                                // each step wanders a little to the side of the outward direction
                h = mix(h);
                float along = len / (STRAND_POINTS - 1) * (0.7f + 0.6f * unit(h));
                x += (float) (nx * along - ny * side * along);
                y += (float) (ny * along + nx * side * along);
            }
            h = mix(h);
            sw[s] = 0.035f + 0.03f * unit(h);
        }
    }

    // ---------------------------------------------------------------- the cache
    private static final int CACHE = 24;
    private static final Map<Long, TearGeometry> CACHE_MAP = new LinkedHashMap<>(32, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, TearGeometry> eldest) { return size() > CACHE; }
    };

    /** The geometry of a seed: made on first use, then the same object (client thread only). */
    public static TearGeometry of(long seed) {
        TearGeometry g = CACHE_MAP.get(seed);
        if (g == null) {
            g = new TearGeometry(seed);
            CACHE_MAP.put(seed, g);
        }
        return g;
    }

    public static int cached() { return CACHE_MAP.size(); }

    public static void clearCache() { CACHE_MAP.clear(); }

    // ---------------------------------------------------------------- the level of detail
    /** The level of detail at a distance (blocks) squared from the camera. */
    public static int lod(double distSq) {
        if (distSq < (double) NEAR * NEAR) return FULL;
        if (distSq < (double) MID * MID) return MIDDLE;
        if (distSq < (double) CULL_DISTANCE * CULL_DISTANCE) return FAR;
        return NONE;
    }

    /** Planes crossed at the tear (two perpendicular ones are seen from every side; one is enough from far). */
    public static int planes(int lod) { return lod == FULL ? 2 : lod == NONE ? 0 : 1; }

    /** Parallax layers of the core in each plane. */
    public static int layers(int lod) { return lod == FULL ? 3 : lod == MIDDLE ? 2 : lod == FAR ? 1 : 0; }

    /** Strands drawn. */
    public static int strands(int lod) { return lod == FULL ? STRANDS : lod == MIDDLE ? 8 : lod == FAR ? 3 : 0; }

    /** Line segments of each strand that are drawn. */
    public static int strandSegments(int lod) { return lod == FULL ? STRAND_POINTS - 1 : lod == MIDDLE ? 4 : 2; }

    /** Is the shimmer (the haze round the tear) drawn? */
    public static boolean shimmer(int lod) { return lod <= MIDDLE; }

    /** Is the rim (the bright outline) drawn? */
    public static boolean rim(int lod) { return lod != NONE; }

    /**
     * The vertices of one frame at a level of detail: the core (a triangle fan as quads: 4 each, N triangles, per layer and plane), the rim (a quad per outline segment per plane), the strands (a quad
     * per drawn segment per plane), the shimmer (one quad per plane) and the glow behind (one quad).
     */
    public static int vertices(int lod) {
        if (lod == NONE) return 0;
        int planes = planes(lod), v = 4;                                     // the glow
        v += planes * layers(lod) * N * 4;
        if (rim(lod)) v += planes * N * 4;
        v += planes * strands(lod) * strandSegments(lod) * 4;
        if (shimmer(lod)) v += planes * 4;
        return v;
    }

    // ---------------------------------------------------------------- hashes (no object is made per frame)
    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** A float in 0 .. 1 from a hash. */
    static float unit(long h) { return (h >>> 40) / (float) (1L << 24); }

    /** A deterministic float in -1 .. 1 from a seed and two integers: the wobble of a strand point at a tick, with no allocation. */
    public static float hash(long seed, int a, int b) { return unit(mix(seed * 31L + a * 0x9E3779B1L + b * 0x85EBCA77L)) * 2f - 1f; }
}
