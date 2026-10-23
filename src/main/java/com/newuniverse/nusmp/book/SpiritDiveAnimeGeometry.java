package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Element;

/**
 * Spirit Dive, ANIME look: the geometry of the smooth ribbons (plume sheets, hair locks, trails) as pure arithmetic on float arrays, so the painter allocates nothing and the unit test can check the
 * bounds without a game. Units are model pixels (1/16 block) in the layer space of the player renderer (y down, the front of the player is -z, the back +z; the left arm is at +x).
 *
 * <ul>
 *   <li>A <b>plume</b> is a cubic Bezier spine from a root on the shoulder blade (control points as fractions of the length along out, up and back), drawn as a strip whose half width follows a shape
 *       profile ({@link Shape}); the strip is bent by a travelling wave and swept back by the wearer's speed (CPU only, a few sin per point).</li>
 *   <li>A <b>lock</b> of hair is a curved spike: root, initial direction, a bend vector and a length, in the frame of the head.</li>
 *   <li>The strip's width vector is the spine tangent crossed with a normal that is a blend of the sheet's own normal and the camera's look vector, so the sheet keeps its shape yet never turns
 *       edge-on to the viewer; {@link #facing} says which winding shows its front.</li>
 * </ul>
 */
public final class SpiritDiveAnimeGeometry {
    private SpiritDiveAnimeGeometry() {}

    /** The ribbon profiles. */
    public enum Shape { LEAF, FLAME, RIBBON, SHARD, SPIKE }

    /** Most segments of any ribbon (the scratch arrays are this big). */
    public static final int MAX_SEGS = 12;
    /** Floats of one plume row: root xyz, three control points (out, up, back), length, width, wave, phase, shape ordinal. */
    public static final int ROW = 17;
    /** Floats of one lock row: root xyz, direction xyz, bend xyz, length, width, wave, phase, shape ordinal. */
    public static final int LOCK_ROW = 14;

    // ------------------------------------------------------------------ the plume tables (px; the first rows are the standard tier's)
    private static final float[][] WIND = {
            {4.0f, 2.0f, 2.8f, 0.30f, 0.50f, 0.00f, 0.95f, 0.95f, 0.10f, 1.55f, 0.85f, 0.70f, 40f, 7.0f, 2.0f, 0.0f, 0},
            {4.2f, 3.5f, 2.8f, 0.60f, 0.15f, 0.10f, 1.20f, 0.20f, 0.30f, 1.70f, 0.00f, 0.80f, 28f, 3.8f, 2.5f, 1.3f, 0},
            {4.0f, 5.0f, 2.8f, 0.40f, -0.20f, 0.10f, 0.90f, -0.50f, 0.30f, 1.20f, -0.90f, 0.80f, 24f, 3.5f, 3.0f, 2.1f, 0},
            {3.6f, 1.0f, 2.6f, 0.25f, 0.50f, 0.05f, 0.50f, 0.90f, 0.10f, 0.65f, 1.15f, 0.25f, 22f, 3.0f, 1.8f, 3.4f, 0}};
    private static final float[][] FIRE = {
            {3.2f, 1.5f, 2.5f, 0.25f, -0.20f, 0.35f, 0.40f, -0.70f, 0.75f, 0.35f, -1.00f, 1.10f, 40f, 5.5f, 3.0f, 0.0f, 1},
            {3.6f, 2.0f, 2.6f, 0.30f, 0.40f, 0.20f, 0.65f, 0.70f, 0.40f, 0.85f, 0.85f, 0.80f, 30f, 3.8f, 2.6f, 1.1f, 1},
            {3.8f, 3.5f, 2.8f, 0.45f, 0.10f, 0.25f, 0.80f, 0.15f, 0.55f, 1.00f, 0.00f, 1.00f, 32f, 3.4f, 2.8f, 2.4f, 1},
            {3.4f, 5.0f, 2.7f, 0.20f, -0.30f, 0.30f, 0.30f, -0.70f, 0.70f, 0.30f, -1.00f, 1.20f, 26f, 3.0f, 2.2f, 3.3f, 1}};
    private static final float[][] WATER = {
            {3.4f, 2.5f, 2.6f, 0.30f, 0.10f, 0.30f, 0.55f, -0.25f, 0.70f, 0.60f, -0.80f, 1.00f, 36f, 4.5f, 2.6f, 0.0f, 2},
            {3.2f, 2.0f, 2.5f, 0.30f, 0.50f, 0.20f, 0.70f, 0.75f, 0.40f, 0.95f, 0.60f, 0.75f, 38f, 2.0f, 2.4f, 1.7f, 2},
            {3.8f, 3.0f, 2.7f, 0.50f, 0.00f, 0.30f, 0.90f, -0.30f, 0.60f, 1.10f, -0.90f, 0.90f, 36f, 1.9f, 2.6f, 2.9f, 2},
            {3.0f, 6.0f, 2.5f, 0.10f, -0.20f, 0.50f, 0.10f, -0.60f, 0.90f, 0.00f, -1.00f, 1.30f, 40f, 1.7f, 2.2f, 4.0f, 2}};
    private static final float[][] EARTH = {
            {3.4f, 2.2f, 2.6f, 0.30f, 0.45f, 0.10f, 0.65f, 0.85f, 0.25f, 0.90f, 1.00f, 0.50f, 28f, 3.6f, 0.0f, 0.0f, 3},
            {3.6f, 3.2f, 2.8f, 0.45f, 0.20f, 0.20f, 0.85f, 0.30f, 0.40f, 1.05f, 0.20f, 0.80f, 30f, 3.8f, 0.0f, 1.0f, 3},
            {3.2f, 4.5f, 2.7f, 0.35f, -0.10f, 0.30f, 0.70f, -0.30f, 0.60f, 0.90f, -0.55f, 0.90f, 24f, 3.2f, 0.0f, 2.0f, 3}};

    /** Plume rows of an element's table (the character tier of a dive may use more of them than the standard tier). */
    public static int rows(Element el, boolean character) { return table(el).length; }

    public static float[] row(Element el, int i) { return table(el)[i]; }

    private static float[][] table(Element el) {
        return switch (el) { case WIND -> WIND; case FIRE -> FIRE; case WATER -> WATER; case EARTH -> EARTH; };
    }

    // ------------------------------------------------------------------ the lock tables (head frame, px: the head box is x -4..4, y -8..0, z -4..4)
    private static final float[][] LOCK_WIND = lockTable(0);
    private static final float[][] LOCK_FIRE = lockTable(1);
    private static final float[][] LOCK_WATER = lockTable(2);
    private static final float[][] LOCK_EARTH = lockTable(3);

    /** The lock rows are generated from a few numbers so the tables stay short (pure and deterministic: the same rows on every client). */
    private static float[][] lockTable(int el) {
        int n = 16;
        float[][] t = new float[n][LOCK_ROW];
        for (int i = 0; i < n; i++) {
            float a = (i % 2 == 0 ? 1f : -1f) * (0.5f + 0.5f * ((i / 2) % 8) / 7f);                           // -1..1 across the head, alternating sides
            float back = (i / 16f), rr = 0.5f + 0.5f * ((i * 7) % 5) / 4f;
            float[] r = t[i];
            switch (el) {
                case 0 -> {                                                                                     // WIND: short spikes lifted up and swept back, a few long ones behind
                    boolean longOne = i >= 10;
                    r[0] = a * 3.4f; r[1] = -8.2f + (longOne ? 0.8f : 0f) ; r[2] = longOne ? 3.4f : -2.8f + 5.2f * (i % 4) / 3f;
                    r[3] = a * 1.1f; r[4] = -0.8f; r[5] = longOne ? 0.9f : 0.25f + 0.1f * (i % 3);
                    r[6] = 0f; r[7] = longOne ? 0.9f : 0.15f; r[8] = longOne ? 0.9f : 0.5f;
                    r[9] = longOne ? 12f : 5.5f + 2.5f * rr; r[10] = longOne ? 2.0f : 2.0f; r[11] = 1.1f; r[12] = i * 0.9f; r[13] = 4;
                }
                case 1 -> {                                                                                     // FIRE: long flame locks streaming up and back off the head
                    if (i < 2) {                                                                                // the first two rows are the Salamander's horns (canon: draconic features), swept back
                        float sd = i == 0 ? 1f : -1f;
                        r[0] = sd * 3.3f; r[1] = -7.2f; r[2] = -2.6f;
                        r[3] = sd * 0.35f; r[4] = -0.55f; r[5] = 0.6f; r[6] = sd * 0.05f; r[7] = -0.1f; r[8] = 0.9f;
                        r[9] = 13f; r[10] = 2.4f; r[11] = 0.2f; r[12] = i; r[13] = 3;
                        continue;
                    }
                    r[0] = a * 3.4f; r[1] = -8.0f + 0.5f * Math.abs(a); r[2] = -1.0f + 4.6f * back;
                    r[3] = a * 0.35f; r[4] = -0.85f; r[5] = 0.55f + 0.4f * back;
                    r[6] = a * 0.1f; r[7] = 0.15f; r[8] = 0.5f;
                    r[9] = 16f + 12f * rr; r[10] = 3.0f; r[11] = 2.6f; r[12] = i * 0.8f; r[13] = 1;
                }
                case 2 -> {                                                                                     // WATER: twin tails and glints: long ribbons from the back of the head hanging down
                    boolean tail = i < 4;
                    r[0] = tail ? (i % 2 == 0 ? 3.6f : -3.6f) : a * 3.4f; r[1] = tail ? -6.4f : -8.0f; r[2] = tail ? 2.8f : -1.5f + 4f * back;
                    r[3] = tail ? (i % 2 == 0 ? 0.55f : -0.55f) : a * 0.4f; r[4] = tail ? -0.1f : -0.9f; r[5] = tail ? 0.8f : 0.4f;
                    r[6] = 0f; r[7] = tail ? 1.1f : 0.5f; r[8] = tail ? 0.2f : 0.5f;
                    r[9] = tail ? 17f : 8f + 3f * rr; r[10] = tail ? 2.8f : 1.8f; r[11] = tail ? 1.8f : 1.0f; r[12] = i * 1.1f; r[13] = 2;
                }
                default -> {                                                                                    // EARTH: crystal shards rising off the head
                    r[0] = a * 3.2f; r[1] = -8.2f; r[2] = -2.5f + 5f * back;
                    r[3] = a * 0.6f; r[4] = -1f; r[5] = 0.1f * (1f + a);
                    r[6] = 0f; r[7] = 0f; r[8] = 0.2f;
                    r[9] = 5f + 4f * rr; r[10] = 2.2f; r[11] = 0f; r[12] = 0f; r[13] = 3;
                }
            }
        }
        return t;
    }

    public static int lockRows(Element el) { return lockTable(el).length; }

    public static float[] lockRow(Element el, int i) { return (switch (el) { case WIND -> LOCK_WIND; case FIRE -> LOCK_FIRE; case WATER -> LOCK_WATER; case EARTH -> LOCK_EARTH; })[i]; }

    private static float[][] lockTable(Element el) { return switch (el) { case WIND -> LOCK_WIND; case FIRE -> LOCK_FIRE; case WATER -> LOCK_WATER; case EARTH -> LOCK_EARTH; }; }

    // ------------------------------------------------------------------ the trails (body frame, px): hands then ankles, each mirrored
    /** Trail roots and directions: x, y, z, dx, dy, dz, length, width. */
    public static final float[][] TRAIL = {
            {7.0f, 10.5f, -0.5f, 0.35f, 0.05f, 1f, 24f, 1.7f},
            {2.6f, 22.0f, 0.5f, 0.20f, -0.05f, 1f, 20f, 1.5f}};

    // ------------------------------------------------------------------ profiles
    private static float clamp01(float x) { return x < 0f ? 0f : x > 1f ? 1f : x; }

    /** The half width of a ribbon at {@code s} (0 root .. 1 tip), as a fraction of the row's width (0..1), for the shape; {@code ph} wobbles the edge of a flame. */
    public static float profile(Shape sh, float s, float ph) {
        s = clamp01(s);
        switch (sh) {
            case LEAF: {
                float f = (float) Math.pow(Math.sin(Math.PI * Math.pow(s, 0.8)), 1.0) * (1f - 0.3f * s);
                return Math.max(f, 0.2f * (1f - s));
            }
            case FLAME: {
                float f = (float) Math.pow(Math.sin(Math.PI * Math.pow(s, 0.62)), 1.05);
                float wob = 1f + 0.2f * (float) Math.sin(11f * s + ph) * s;
                return Math.max(f * wob, 0.2f * (1f - s));
            }
            case RIBBON: {
                float up = Math.min(1f, s / 0.1f);
                return up * (1f - 0.7f * s * s) * (1f - (float) Math.pow(s, 7));
            }
            case SHARD: {
                float up = Math.min(1f, s / 0.12f);
                return up * (float) Math.pow(1f - s, 0.8);
            }
            default: {
                float up = Math.min(1f, s / 0.06f);
                return up * (float) Math.pow(1f - s, 1.15);
            }
        }
    }

    public static Shape shape(float ordinal) { Shape[] v = Shape.values(); int i = (int) ordinal; return v[i < 0 ? 0 : Math.min(i, v.length - 1)]; }

    // ------------------------------------------------------------------ spines
    /**
     * A plume's spine into {@code out} (3 floats per point, {@code segs + 1} points), in the frame of the body, px.
     *
     * @param row    a plume row of the table
     * @param side   +1 the left side (x positive), -1 the right side
     * @param lenK   length factor (the tier, the entry, the standard tier's smaller plumes)
     * @param age    the renderer's clock in ticks
     * @param swing  0..1 how fast the wearer walks (sweeps the plume back)
     * @param vy     vertical speed in blocks per tick (a fall or a climb bends the plume)
     */
    public static void plumeSpine(float[] out, int segs, float[] row, float side, float lenK, float age, float swing, float vy) {
        float len = row[12] * lenK, wave = row[14];
        float rx = row[0] * side, ry = row[1], rz = row[2];
        float sw = clamp01(swing), rise = Math.max(-1f, Math.min(1f, vy * 2.5f));
        for (int k = 0; k <= segs; k++) {
            float s = k / (float) segs, u = 1f - s;
            float b0 = u * u * u, b1 = 3f * u * u * s, b2 = 3f * u * s * s, b3 = s * s * s;
            float o = b1 * row[3] + b2 * row[6] + b3 * row[9];
            float up = b1 * row[4] + b2 * row[7] + b3 * row[10];
            float bk = b1 * row[5] + b2 * row[8] + b3 * row[11];
            float w = wave * (float) Math.sin(age * 0.11f + row[15] + s * 5.5f) * (float) Math.pow(s, 1.3);
            float s2 = s * s;
            int i = k * 3;
            out[i] = rx + side * (o * len + 0.5f * w);
            out[i + 1] = ry - up * len + rise * len * 0.35f * s2 + 0.35f * w;
            out[i + 2] = rz + bk * len + w + sw * 0.5f * len * s2;
        }
    }

    /**
     * A hair lock's spine into {@code out}, in the frame of the head, px. {@code lift} (0..1.1) is the entry's hair lift: the lock is short and straight up at first.
     */
    public static void lockSpine(float[] out, int segs, float[] row, float lenK, float lift, float age, float swing, float vy) {
        float len = row[9] * lenK * (0.25f + 0.75f * Math.min(lift, 1.1f));
        float sw = clamp01(swing), rise = Math.max(-1f, Math.min(1f, vy * 2.5f));
        float dx = row[3], dy = row[4], dz = row[5];
        float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dl < 1e-4f) { dl = 1f; dy = -1f; }
        dx /= dl; dy /= dl; dz /= dl;
        float up = 1f - Math.min(lift, 1f);                                         // before the lift the lock stands straight up
        dx *= 1f - 0.8f * up; dz *= 1f - 0.8f * up; dy = dy * (1f - up) - up;
        for (int k = 0; k <= segs; k++) {
            float s = k / (float) segs, s2 = s * s;
            float w = row[11] * (float) Math.sin(age * 0.17f + row[12] + s * 4.2f) * s2;
            int i = k * 3;
            out[i] = row[0] + len * (dx * s + row[6] * s2) + w;
            out[i + 1] = row[1] + len * (dy * s + row[7] * s2) + rise * len * 0.3f * s2 + 0.4f * w;
            out[i + 2] = row[2] + len * (dz * s + row[8] * s2) + sw * 0.6f * len * s2 + 0.5f * w;
        }
    }

    /** A trail's spine into {@code out} (body frame, px): streams back from the hand or ankle, longer when the wearer moves. */
    public static void trailSpine(float[] out, int segs, float[] row, float side, float age, float swing, float phase) {
        float len = row[6] * (0.6f + 0.8f * clamp01(swing));
        float dx = row[3] * side, dy = row[4], dz = row[5];
        float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= dl; dy /= dl; dz /= dl;
        for (int k = 0; k <= segs; k++) {
            float s = k / (float) segs;
            float w = 1.8f * (float) Math.sin(age * 0.2f + phase + s * 5f) * s;
            int i = k * 3;
            out[i] = row[0] * side + len * dx * s + w;
            out[i + 1] = row[1] + len * dy * s - 3f * s * s + 0.6f * w;
            out[i + 2] = row[2] + len * dz * s;
        }
    }

    // ------------------------------------------------------------------ the strip
    /**
     * The two edges of the strip along a spine: {@code left} and {@code right} (3 floats per point), the spine pushed by +- half width along (tangent x normal), where the normal is the sheet's
     * normal {@code (nx, ny, nz)} blended with the camera look vector {@code look} by {@code blend} (0 = fixed sheet, 1 = always facing the viewer).
     *
     * @param widths half width per point in px
     */
    public static void edges(float[] spine, int n, float[] widths, float nx, float ny, float nz, float[] look, float blend, float[] left, float[] right) {
        float bx = nx * (1f - blend) - look[0] * blend, by = ny * (1f - blend) - look[1] * blend, bz = nz * (1f - blend) - look[2] * blend;
        for (int k = 0; k < n; k++) {
            int a = Math.max(0, k - 1) * 3, b = Math.min(n - 1, k + 1) * 3, i = k * 3;
            float tx = spine[b] - spine[a], ty = spine[b + 1] - spine[a + 1], tz = spine[b + 2] - spine[a + 2];
            float wx = ty * bz - tz * by, wy = tz * bx - tx * bz, wz = tx * by - ty * bx;
            float l = (float) Math.sqrt(wx * wx + wy * wy + wz * wz);
            if (l < 1e-5f) { wx = 1f; wy = 0f; wz = 0f; l = 1f; }
            float hw = widths[k] / l;
            left[i] = spine[i] + wx * hw; left[i + 1] = spine[i + 1] + wy * hw; left[i + 2] = spine[i + 2] + wz * hw;
            right[i] = spine[i] - wx * hw; right[i + 1] = spine[i + 1] - wy * hw; right[i + 2] = spine[i + 2] - wz * hw;
        }
    }

    /**
     * True when the strip's quads (left_k, right_k, right_k+1, left_k+1) face the viewer, judged at the middle of the strip: the painter emits the quad in this order, or reversed (the additive
     * render types cull the back faces, so the order is what makes the strip visible).
     */
    public static boolean facing(float[] left, float[] right, int n, float[] look) {
        int m = Math.max(0, (n - 1) / 2) * 3, m2 = Math.min(n - 1, (n - 1) / 2 + 1) * 3;
        float ax = right[m] - left[m], ay = right[m + 1] - left[m + 1], az = right[m + 2] - left[m + 2];
        float cx = left[m2] - left[m], cy = left[m2 + 1] - left[m + 1], cz = left[m2 + 2] - left[m + 2];
        float nx = ay * cz - az * cy, ny = az * cx - ax * cz, nz = ax * cy - ay * cx;      // the quad's normal (the vanilla winding: counter-clockwise seen from the front)
        return nx * look[0] + ny * look[1] + nz * look[2] < 0f;
    }

    /** The same test for one quad of the strip (left_k, right_k, right_k+1, left_k+1), given the offsets {@code i} and {@code j} of the two points in the edge arrays. */
    public static boolean facingQuad(float[] left, float[] right, int i, int j, float[] look) {
        float ax = right[i] - left[i], ay = right[i + 1] - left[i + 1], az = right[i + 2] - left[i + 2];
        float cx = left[j] - left[i], cy = left[j + 1] - left[i + 1], cz = left[j + 2] - left[i + 2];
        float nx = ay * cz - az * cy, ny = az * cx - ax * cz, nz = ax * cy - ay * cx;
        return nx * look[0] + ny * look[1] + nz * look[2] < 0f;
    }

    /** The furthest any point of a plume can be from the body centre line, in px (bounds the effect for the culling and the unit test): length + wave + sweep + width. */
    public static float reach(float[] row, float lenK) {
        float o = Math.abs(row[9]) + 0.2f, up = Math.abs(row[10]) + 0.2f, bk = Math.abs(row[11]) + 0.6f;
        float len = row[12] * lenK;
        return (float) Math.sqrt(sq(row[0] + o * len) + sq(row[1] + up * len) + sq(row[2] + bk * len)) + row[14] * 2f + row[13];
    }

    private static float sq(float x) { return x * x; }
}
