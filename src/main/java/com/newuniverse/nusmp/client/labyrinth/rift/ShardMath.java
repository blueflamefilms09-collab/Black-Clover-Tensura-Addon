package com.newuniverse.nusmp.client.labyrinth.rift;

/**
 * Why: the "reality shards" of a space-time fracture, as plain arithmetic. A shard is a sliver of the world torn loose by the crack: it appears near the tear, grows in, drifts slowly outward while
 * it turns, then DISINTEGRATES (it shrinks, splits into two halves that move apart, and fades) and is born again somewhere else on the next cycle. Everything is a closed-form function of the
 * rift's seed, the shard's number and the tick, so there is no list of shards to keep, tick or clean up: {@link #state} fills a caller-owned scratch array with the position, size, rotation,
 * fade and split of one shard at one moment, and {@code RealityShards} draws it. The number of shards follows the distance ({@link #count}: 14 near, 8 in the middle, 3 far, none beyond, the
 * levels of {@link TearGeometry#lod}) and {@link #vertices} never exceeds {@link #BUDGET}. No Minecraft type is used, so a unit test pins the arithmetic.
 */
public final class ShardMath {
    private ShardMath() {}

    /** A shard is drawn as up to two pieces (the whole, or the two halves it breaks into), each a four-faced sliver of 16 vertices. */
    public static final int PIECES = 2, VERTS_PER_PIECE = 16;
    /** The most vertices the shards of one tear may draw in a frame. */
    public static final int BUDGET = 512;
    /** Indices of the scratch array filled by {@link #state}. */
    public static final int X = 0, Y = 1, Z = 2, SIZE = 3, RX = 4, RY = 5, RZ = 6, ALPHA = 7, SPLIT = 8, OUT_LEN = 9;
    /** Cycle length of a shard in ticks: the shortest and how much longer it can be (7.5 to 16 seconds). */
    public static final int LIFE_MIN = 150, LIFE_SPAN = 170;
    /** Where in the cycle (0..1) the shard has fully grown in and where it starts to break up. */
    public static final float GROWN = 0.12f, BREAKS = 0.55f;
    /** The largest a shard grows to (blocks, the half-length of the sliver). */
    public static final float MAX_SIZE = 0.2f;

    public static int count(int lod) {
        switch (lod) {
            case TearGeometry.FULL: return 14;
            case TearGeometry.MIDDLE: return 8;
            case TearGeometry.FAR: return 3;
            default: return 0;
        }
    }

    /** The vertices of all the shards at a level of detail, counting every shard as split (the worst case). */
    public static int vertices(int lod) { return count(lod) * PIECES * VERTS_PER_PIECE; }

    /** How many pieces a shard with this split amount is drawn as: one until it starts to part. */
    public static int pieces(float split) { return split > 0.02f ? 2 : 1; }

    /**
     * Fills {@code out} with shard {@code i} of the tear {@code seed} at {@code tick}: position relative to the tear's centre, size (blocks), rotation (degrees about x, y, z), alpha (0..1) and split (0..1). {@code rx} is
     * the tear's half width and {@code ry} its half height. Returns false when the shard is not visible at this moment (not yet grown, or gone), and the caller then draws nothing.
     */
    public static boolean state(long seed, int i, float tick, float rx, float ry, float[] out) {
        long hs = TearGeometry.mix(seed * 0x9E3779B97F4A7C15L + i * 0x632BE59BD9B4E019L + 17L);
        float period = LIFE_MIN + LIFE_SPAN * TearGeometry.unit(hs);
        float offset = period * TearGeometry.unit(TearGeometry.mix(hs + 1));
        float u = (tick + offset) / period;
        int cycle = (int) Math.floor(u);
        float p = u - cycle;
        long h = TearGeometry.mix(hs + cycle * 0x2545F4914F6CDD1DL + 3L);
        float theta = (float) (TearGeometry.unit(h) * Math.PI * 2.0);
        h = TearGeometry.mix(h);
        float phi = (TearGeometry.unit(h) - 0.5f) * 2.5f;                                           // latitude, about +-72 degrees
        h = TearGeometry.mix(h);
        float rf = 0.75f + 0.5f * TearGeometry.unit(h);
        float cp = (float) Math.cos(phi);
        float sx = cp * (float) Math.cos(theta) * rx * rf * 1.6f, sy = (float) Math.sin(phi) * ry * rf, sz = cp * (float) Math.sin(theta) * rx * rf * 1.6f;
        h = TearGeometry.mix(h);
        float dist = 0.5f + 0.9f * TearGeometry.unit(h);                                               // how far it drifts outward over its life
        float len = (float) Math.sqrt(sx * sx + sy * sy * 0.09f + sz * sz);
        float inv = len < 1e-4f ? 0f : 1f / len;
        float bob = 0.05f * (float) Math.sin(tick * 0.07f + i * 1.7f);
        out[X] = sx + sx * inv * dist * p;
        out[Y] = sy + sy * 0.3f * inv * dist * p + 0.25f * p + bob;                                    // it also rises a little
        out[Z] = sz + sz * inv * dist * p;
        h = TearGeometry.mix(h);
        float base = 0.07f + (MAX_SIZE - 0.07f) * TearGeometry.unit(h);
        float grow = p >= GROWN ? 1f : smooth(p / GROWN);
        float decay = 1f;
        float split = 0f;
        if (p > BREAKS) {
            float k = (p - BREAKS) / (1f - BREAKS);
            decay = 1f - (float) Math.pow(k, 1.6);
            split = Math.min(1f, (p - (BREAKS - 0.1f)) / (1f - BREAKS + 0.1f));
        } else if (p > BREAKS - 0.1f) {
            split = (p - (BREAKS - 0.1f)) / (1f - BREAKS + 0.1f);
        }
        out[SIZE] = base * grow * decay;
        float fadeIn = Math.min(1f, p / 0.1f), fadeOut = p > 0.8f ? (1f - p) / 0.2f : 1f;
        out[ALPHA] = Math.max(0f, Math.min(1f, fadeIn * fadeOut));
        out[SPLIT] = split;
        h = TearGeometry.mix(h);
        float spinX = (TearGeometry.unit(h) - 0.5f) * 3.2f;
        h = TearGeometry.mix(h);
        float spinY = (TearGeometry.unit(h) - 0.5f) * 3.2f;
        h = TearGeometry.mix(h);
        float spinZ = (TearGeometry.unit(h) - 0.5f) * 2.0f;
        float t0 = tick - cycle * period + offset;
        out[RX] = TearGeometry.unit(TearGeometry.mix(h + 1)) * 360f + spinX * t0 * 0.35f;
        out[RY] = TearGeometry.unit(TearGeometry.mix(h + 2)) * 360f + spinY * t0 * 0.35f;
        out[RZ] = TearGeometry.unit(TearGeometry.mix(h + 3)) * 360f + spinZ * t0 * 0.35f;
        return out[SIZE] > 0.004f && out[ALPHA] > 0.02f;
    }

    private static float smooth(float x) { return x * x * (3f - 2f * x); }
}
