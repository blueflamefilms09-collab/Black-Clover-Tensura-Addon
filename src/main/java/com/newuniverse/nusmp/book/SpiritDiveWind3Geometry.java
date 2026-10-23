package com.newuniverse.nusmp.book;

/**
 * Spirit Dive WIND remake, the three looks: where every real 3D piece stands, as pure arithmetic on float arrays (no allocation; the unit test checks the bounds without a game). Units are model pixels
 * (1/16 block) in the layer space of the player renderer, like {@link SpiritDiveWindGeometry}: the origin at the neck, y DOWN (the feet at y = 24), the front of the player is -z, the back +z, the LEFT
 * arm is at +x. A mesh piece is placed by a root point and the direction its +y axis points to; its flat face is turned across the body ({@code hint}), so a blade shows its width to a viewer in front.
 */
public final class SpiritDiveWind3Geometry {
    private SpiritDiveWind3Geometry() {}

    // ------------------------------------------------------------------ A and B: the sword, the bow, the spear
    /**
     * Zephyr's sword: the hilt floats at the wearer's right side (x < 0) in front of the hip, the blade points up and forward: root x, y, z, direction (out, up as negative y, back as positive z), the scale
     * of the mesh (1 = the mesh's 46 px blade), the bob amplitude.
     */
    public static final float[] SWORD = {-10.5f, 14.0f, -5.5f, -0.38f, -0.86f, -0.34f, 1.0f, 0.5f};
    /** The same sword in the version B (the clip): lower hilt, the blade pointing out and forward like the second video, a touch more tilted. */
    public static final float[] SWORD_B = {-9.5f, 13.0f, -6.0f, -0.62f, -0.70f, -0.36f, 1.05f, 0.5f};
    /** The small leaf blade of the standard tier: root, direction, scale of the 14 px blade mesh (so about 36 px long). */
    public static final float[] LEAF_BLADE = {-9.5f, 12.0f, -5.0f, -0.50f, -0.80f, -0.32f, 2.5f, 0.5f};
    /** The leaf bow on the back at rest (version A): centre, direction of its axis, scale. */
    public static final float[] BOW_BACK = {-1.0f, 7.0f, 6.6f, 0.34f, -0.94f, 0.0f, 0.82f, 0.4f};
    /** The leaf bow in the left hand while a page is cast: centre, axis, scale; the arrow flies from the bow's centre along -z for ARROW_AT. */
    public static final float[] BOW_HAND = {11.0f, 6.0f, -10.0f, 0.10f, -1.0f, 0.0f, 0.95f, 0.5f};
    public static final float ARROW_AT = 3.0f;
    /** The second blade of the clip (version B): a spear standing up behind the wearer's left shoulder: root, direction, scale. */
    public static final float[] SPEAR_B = {5.5f, 14.0f, 6.5f, 0.10f, -1.0f, 0.16f, 1.0f, 0.4f};
    /** The flat magic circle round the hilt (version B): half size in px (it faces the viewer). */
    public static final float CIRCLE_B = 13.0f;

    // ------------------------------------------------------------------ B: the cube crown, the gauntlets, the voxel wing
    /** The cubes of the crown ring (head frame): half edge of a low cube, the height of a tall one's half and the number of ring places (the bars of the pack's crown, in the same places). */
    public static final float CUBE_HALF = 0.95f, BAR_HALF_MIN = 0.95f, BAR_HALF_K = 0.30f;
    public static final int CROWN_PLACES = SpiritDiveWindGeometry.BARS_MAX;

    /** Writes the crown cube {@code i} (head frame): x, y, z of the centre and the half extents in x, y, z into {@code out} (6 floats); odd places are the tall bars, even places low cubes. */
    public static void crownCube(float[] out, int i) {
        float[] b = SpiritDiveWindGeometry.BARS[i];
        boolean tall = (i & 1) == 1;
        float hy = tall ? BAR_HALF_MIN + b[2] * BAR_HALF_K : CUBE_HALF * 0.9f;
        out[0] = b[0] * 0.92f; out[1] = SpiritDiveWindGeometry.RING_Y + 1.6f - hy + 0.4f; out[2] = b[1] * 0.92f;
        out[3] = tall ? CUBE_HALF * 0.78f : CUBE_HALF; out[4] = hy; out[5] = tall ? CUBE_HALF * 0.78f : CUBE_HALF;
    }

    /** Crown places drawn for a count: the first {@code n}, or only those with x >= 0 (the first dive of Wind draws the left half). */
    public static int crownCount(int n, boolean leftOnly) { return SpiritDiveWindGeometry.barCount(n, leftOnly); }

    /** The gauntlet of an arm (arm frame): the forearm cube centre y and half extents, the fist cube centre y and half extents, the band's y and half extents. */
    public static final float[] GAUNTLET = {7.0f, 3.1f, 3.9f, 3.1f, 12.4f, 2.5f, 1.6f, 2.5f, 3.6f, 3.5f, 0.55f, 3.5f};

    /** The voxel wing: root (body frame, left side; the right side mirrors x), the axis the panels stack along (out, up as negative y, back) and the stacking of the panels. */
    public static final float[] WING_ROOT = {3.0f, 1.5f, 3.0f}, WING_AXIS = {0.40f, -0.90f, 0.34f};
    /** Per big-wing panel: the distance of its centre along the axis (px), the half width, the half length (the stacked silhouette of a leaf, narrow at the root, widest at 55 percent). */
    public static final float[][] PANELS = {
            {2.5f, 1.6f, 2.4f}, {7.0f, 3.0f, 2.4f}, {11.5f, 4.2f, 2.4f}, {16.0f, 5.0f, 2.4f}, {20.5f, 5.0f, 2.4f}, {25.0f, 4.0f, 2.4f}, {29.5f, 2.2f, 2.4f}};
    /** The small wing panels (lower, flatter, shorter); the axis is {@link #WING_SMALL_AXIS}. */
    public static final float[][] PANELS_SMALL = {{2.2f, 1.4f, 2.2f}, {6.4f, 2.5f, 2.2f}, {10.6f, 3.2f, 2.2f}, {14.8f, 2.8f, 2.2f}, {19.0f, 1.6f, 2.2f}};
    public static final float[] WING_SMALL_ROOT = {3.0f, 4.0f, 3.0f}, WING_SMALL_AXIS = {0.82f, -0.40f, 0.30f};

    // ------------------------------------------------------------------ C: the spirit wings, the plates, the halo of arrows
    /** The root of a feather wing (left shoulder blade; the right side mirrors x) and the plane the fan opens in: the feathers turn from {@link #FAN_FROM} to {@link #FAN_TO} (rad) away from straight up. */
    public static final float[] FEATHER_ROOT = {2.6f, 1.2f, 2.8f};
    public static final float FAN_FROM = 0.06f, FAN_TO = 1.0f, FEATHER_BACK = 0.85f;
    /** The scale of feather {@code i} of {@code n} (the first is the longest, the last the shortest). */
    public static float featherScale(int i, int n, boolean character) { return (character ? 0.95f : 0.8f) * (1.0f - 0.38f * i / Math.max(1, n - 1)); }

    /**
     * The direction (out, up as negative y, back) of feather {@code i} of {@code n} on the side {@code side} (+1 the left, -1 the right) into {@code out[0..2]}; {@code sway} (rad) breathes the fan.
     */
    public static void featherDir(float[] out, int i, int n, float side, float sway) {
        float f = n <= 1 ? 0f : i / (float) (n - 1), a = FAN_FROM + (FAN_TO - FAN_FROM) * f + sway;
        float x = side * (float) Math.sin(a), y = -(float) Math.cos(a), z = FEATHER_BACK * (1f - 0.55f * f);
        float l = (float) Math.sqrt(x * x + y * y + z * z);
        out[0] = x / l; out[1] = y / l; out[2] = z / l;
    }

    /** The pauldron plate of a shoulder: centre x (left side), y, z, the lean outward, the scale. */
    public static final float[] PLATE = {6.2f, 1.2f, 0.0f, 0.62f, 0.78f};

    /** The halo of arrows round the torso: radius, the height of the middle (y), the bob, the turn per tick (rad), and the scale of the blade mesh (14 px -> about 21 px). */
    public static final float HALO_R = 15.0f, HALO_Y = 12.0f, HALO_BOB = 5.5f, HALO_TURN = 0.045f, HALO_SCALE = 1.0f;

    /**
     * The orbiter {@code i} of {@code n} at the renderer's {@code age} into {@code out}: x, y, z of its root and the unit direction its tip points to (along the motion, tilted outward and a little up so
     * the halo reads as a ring of arrows seen from any side).
     */
    public static void orbiter(float[] out, int i, int n, float age) {
        float a = age * HALO_TURN + i * 6.2831855f / Math.max(1, n);
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        float r = HALO_R + 1.2f * (float) Math.sin(i * 2.1f + age * 0.05f);
        out[0] = r * c;
        out[1] = HALO_Y + HALO_BOB * (float) Math.sin(i * 2.4f + age * 0.035f);
        out[2] = r * s;
        float dx = -s + 0.25f * c, dy = -0.18f, dz = c + 0.25f * s, l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        out[3] = dx / l; out[4] = dy / l; out[5] = dz / l;
    }

    /** The reach of every piece from the body's centre line (px), for the bounds test. */
    public static float reach() {
        float w = Math.abs(SWORD_B[0]) + 52f * SWORD_B[6];
        w = Math.max(w, Math.abs(WING_ROOT[0]) + PANELS[PANELS.length - 1][0] + PANELS[PANELS.length - 1][2] + 6f);
        w = Math.max(w, FEATHER_ROOT[0] + 30f * 0.95f + 2f);
        w = Math.max(w, HALO_R + 3f + 15f * HALO_SCALE);
        return w;
    }
}
