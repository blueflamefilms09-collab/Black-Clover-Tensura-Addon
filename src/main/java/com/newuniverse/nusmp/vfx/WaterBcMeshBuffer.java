package com.newuniverse.nusmp.vfx;

/**
 * The mesh quads of one frame for the water hybrid, as fixed arrays (no growth, no allocation per frame): the solid pass (depth written), the translucent pass (blended, no depth write) and the glow
 * pass (additive). Pure data: the painter fills it while the effects are drawn, {@code vfx.client.WaterBcDraw} draws it once per frame before the sprite buffer and clears it. The total is hard-capped at
 * {@link #limit} (the frame ceiling, scaled by the quality level): a quad over it is dropped and counted, whatever the layer asks for.
 *
 * <p>A quad is 12 floats (four vertices in camera-relative space, x y z) and one packed ARGB colour: the quads are flat shaded (the toon shade is one colour per quad) and the texture is one white cell.
 */
public final class WaterBcMeshBuffer {
    public static final int SOLID = 0, TRANSLUCENT = 1, GLOW = 2, PASSES = 3, STRIDE = 12;

    /** The one buffer of the render thread. */
    public static final WaterBcMeshBuffer FRAME = new WaterBcMeshBuffer(WaterBc.FRAME_MESH_CEILING);

    private final float[][] data = new float[PASSES][];
    private final int[][] colour = new int[PASSES][];
    private final int[] count = new int[PASSES];
    private final int capacity;
    private int limit, dropped;

    public WaterBcMeshBuffer(int capacity) {
        this.capacity = capacity;
        this.limit = capacity;
        for (int p = 0; p < PASSES; p++) {
            data[p] = new float[capacity * STRIDE];
            colour[p] = new int[capacity];
        }
    }

    /** Empties the buffer for the next frame and sets the frame ceiling of mesh quads (at most the capacity). */
    public void reset(int quadLimit) {
        for (int p = 0; p < PASSES; p++) count[p] = 0;
        limit = Math.max(0, Math.min(capacity, quadLimit));
        dropped = 0;
    }

    public void reset() { reset(capacity); }

    public int total() { return count[0] + count[1] + count[2]; }

    public int count(int pass) { return count[pass]; }

    public float[] data(int pass) { return data[pass]; }

    public int[] colours(int pass) { return colour[pass]; }

    public int limit() { return limit; }

    /** Quads refused this frame because the ceiling was reached. */
    public int dropped() { return dropped; }

    /** Adds one quad to a pass; false (and counted) when the frame ceiling is reached. */
    public boolean add(int pass, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, int argb) {
        if (total() >= limit) {
            dropped++;
            return false;
        }
        float[] d = data[pass];
        int n = count[pass]++, i = n * STRIDE;
        d[i] = x0; d[i + 1] = y0; d[i + 2] = z0;
        d[i + 3] = x1; d[i + 4] = y1; d[i + 5] = z1;
        d[i + 6] = x2; d[i + 7] = y2; d[i + 8] = z2;
        d[i + 9] = x3; d[i + 10] = y3; d[i + 11] = z3;
        colour[pass][n] = argb;
        return true;
    }
}
