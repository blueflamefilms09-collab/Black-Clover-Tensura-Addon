package com.newuniverse.nusmp.book;

/**
 * Spirit Dive, WIND (the PACK look, the owner's reference pack of Yuno's Spirit Dive stills): the geometry as pure arithmetic on float arrays (the painter allocates nothing; the unit test checks the
 * bounds without a game). Units are model pixels (1/16 block) in the layer space of the player renderer: the origin at the neck, y DOWN (the feet are at y = 24), the front of the player is -z, the back
 * +z, the LEFT arm is at +x. The head frame is the head part's own (the head box is x -4..4, y -8..0, z -4..4); the arm frame is the arm part's own (the arm box of the left arm is x -1..3, y -2..10).
 *
 * <ul>
 *   <li>The <b>crown</b> (Spirit_Dive.png, the manga and the video): a thin ring floating above the head and tall vertical bars with arrow-tip tops standing on it; the character tier has more bars and a
 *       second, lower ring.</li>
 *   <li>The <b>leaf wing</b>: ONE big smooth leaf from the shoulder blade, swept up and back (the spine is REEL's {@link SpiritDiveReelGeometry#leafSpine}, the rows are this look's own), a smaller
 *       second leaf, an ornate cross near the tip of the big one.</li>
 *   <li>The <b>wrist circle</b> of connected crosses on the casting hand and the big circle of crosses behind the back (character tier).</li>
 *   <li>The <b>weapons</b> of the character tier: Zephyr's long sword with the crossed circles of the guard, Boreas' leaf bow with an arrow of light.</li>
 * </ul>
 */
public final class SpiritDiveWindGeometry {
    private SpiritDiveWindGeometry() {}

    // ------------------------------------------------------------------ the crown (head frame, px)
    /** Half side of the crown's upper ring (a square band round the head: the head is 4 and the hat layer 4.5), its height above the head (y, negative = up) and the thickness of the band, px. */
    public static final float RING_HALF = 5.4f, RING_Y = -10.6f, RING_THICK = 1.15f;
    /** The second, lower ring of the character tier: half side, y, thickness. */
    public static final float RING2_HALF = 5.0f, RING2_Y = -8.9f, RING2_THICK = 0.9f;
    /** Most bars of the crown (the standard tier uses the first {@link #BARS_STANDARD}). */
    public static final int BARS_MAX = 14, BARS_STANDARD = 8;
    /** The bars: x, z of the foot on the ring, the height (px) and a flicker phase. The first eight are spread evenly round the ring; the other six fill the gaps. */
    public static final float[][] BARS = new float[BARS_MAX][4];
    /** The width of a bar as a fraction of its height (the texture is 24 x 96: a quarter). */
    public static final float BAR_ASPECT = 0.25f;

    static {
        float[] h = {5.0f, 6.4f, 4.6f, 6.0f, 5.2f, 6.8f, 4.8f, 5.8f, 6.2f, 7.4f, 5.4f, 6.9f, 5.6f, 7.6f};       // tall and short bars alternate (the crown of the pack is uneven)
        float[] t = {0.0625f, 0.1875f, 0.3125f, 0.4375f, 0.5625f, 0.6875f, 0.8125f, 0.9375f, 0.125f, 0.375f, 0.625f, 0.875f, 0.0f, 0.5f};
        for (int i = 0; i < BARS_MAX; i++) {
            float s = t[i] * 4f;
            int side = (int) s;
            float f = s - side, a = RING_HALF, p = -a + 2f * a * f;
            float x, z;
            switch (side) {
                case 0 -> { x = p; z = -a; }                 // the front
                case 1 -> { x = a; z = p; }                  // the left side (+x)
                case 2 -> { x = -p; z = a; }                 // the back
                default -> { x = -a; z = -p; }               // the right side
            }
            BARS[i][0] = x; BARS[i][1] = z; BARS[i][2] = h[i]; BARS[i][3] = i * 1.7f;
        }
    }

    /** Bars drawn for a spec count: all of the first {@code n}, or only the left half (x >= 0) for the first dive of Wind. */
    public static int barCount(int n, boolean leftOnly) {
        int c = 0;
        for (int i = 0; i < Math.min(n, BARS_MAX); i++) if (!leftOnly || BARS[i][0] >= -1e-3f) c++;
        return c;
    }

    // ------------------------------------------------------------------ the leaves (rows of REEL's leafSpine: root xyz, direction out/up/back, bend out/up/back, length, half width, wave, phase)
    public static final int BIG = 0, SMALL = 1;
    private static final float[][] LEAF = {
            {3.2f, 0.8f, 2.6f, 0.30f, 0.92f, 0.34f, 0.60f, -0.30f, 0.45f, 40f, 8.6f, 1.0f, 0.0f, 0f},      // the big leaf: from the shoulder blade up, out and back; the tip bends outward
            {3.0f, 3.6f, 2.6f, 0.80f, 0.36f, 0.34f, 0.34f, -0.10f, 0.28f, 19f, 4.0f, 0.9f, 2.1f, 0f}};     // the small leaf: lower, flatter, shorter
    /** The leaves of the character tier are a sixth bigger. */
    public static final float LEAF_CHARACTER = 1.12f;
    /** Where on the big leaf the cross sits (0 root .. 1 tip), the cross's half size in px (standard, character) and its offset away from the spine, px. */
    public static final float CROSS_AT = 0.80f, CROSS_SIZE = 7.0f, CROSS_SIZE_CHARACTER = 9.0f, CROSS_LIFT = 3.2f;

    public static int leafRows() { return LEAF.length; }

    public static float[] leaf(int i) { return LEAF[i]; }

    // ------------------------------------------------------------------ the circles of crosses
    /** The wrist ring (arm frame, px): its height on the arm (y; the box of the arm is -2..10), the half size of the plane perpendicular to the arm and of the sprite that always faces the viewer. */
    public static final float WRIST_Y = 8.4f, WRIST_PLANE = 5.6f, WRIST_SPRITE = 7.2f;
    /** The big circle behind the back of the character tier (body frame, px): centre, half size, turns per tick (rad). */
    public static final float[] BACK = {0f, 7.0f, 4.4f, 15.5f, 0.012f};

    // ------------------------------------------------------------------ the weapons (body frame, px)
    /**
     * Zephyr's sword: the hilt (the crossed circles of the guard) in front of the right hip, the blade out, forward and up (a long blade, reaching well past the arm). Root x, y, z, direction (out to the
     * wearer's right, up, back: negative z is forward), the length of the blade, the half width of the guard circle, the pommel's length behind the hilt (px). The sword is a flat strip that turns its face
     * to the viewer about its own axis; it sways with a slow bob.
     */
    public static final float[] SWORD = {-9.5f, 11.0f, -5.0f, -0.30f, 0.78f, -0.55f, 46f, 8.5f, 10f};
    /** Boreas' bow: the grip in front of the left hand (centre x, y, z), the axis of the bow (out, up, back: nearly vertical), the height of the bow, and the arrow (its tail's distance behind the grip, its length, half width). */
    public static final float[] BOW = {11.5f, 7.0f, -10.5f, 0.10f, 1.0f, 0.0f, 46f};
    public static final float[] ARROW = {5.0f, 36f, 2.3f};

    /** The weapon's bob (px) at the renderer's clock. */
    public static float bob(float age, float phase) { return SpiritDiveReelGeometry.bob(age, phase); }

    // ------------------------------------------------------------------ the steady wind ribbons (the white loops of Spirit_of_Zephyr.png)

    /**
     * The spine of steady ribbon {@code i} of {@code n} into {@code out} (3 floats per point, {@code segs + 1} points), px: a long soft loop round the body, about 200 degrees of turn from the hip to above the
     * shoulder, drifting slowly with {@code age}. Smooth (a few degrees per segment), so the strip never reads as a zigzag.
     */
    public static void ribbonSpine(float[] out, int segs, int i, int n, float age) {
        float base = age * 0.03f + i * (float) (2.0 * Math.PI) / Math.max(1, n);
        for (int k = 0; k <= segs; k++) {
            float s = k / (float) segs;
            float a = base + s * 3.5f, r = 13.5f + 3.5f * (float) Math.sin(s * Math.PI) + 1.5f * (i % 2);
            int j = k * 3;
            out[j] = r * (float) Math.cos(a);
            out[j + 1] = 27f - 40f * s + 2.5f * (float) Math.sin(s * 5f + i * 1.3f + age * 0.05f) + 5f * (i % 3);
            out[j + 2] = r * (float) Math.sin(a);
        }
    }

    /** The furthest any point of the look can be from the body's centre line, px (bounds it for the unit test): the leaves, the weapons, the circles, the ribbons. */
    public static float reach() {
        float w = 0f;
        for (float[] r : LEAF) w = Math.max(w, Math.abs(r[0]) + r[9] * LEAF_CHARACTER * 1.5f + r[10] * 2f);
        w = Math.max(w, Math.abs(SWORD[0]) + SWORD[6] + SWORD[7]);
        w = Math.max(w, Math.abs(BOW[0]) + BOW[6] * 0.6f + 6f);
        w = Math.max(w, BACK[3] + 4f);
        return Math.max(w, SpiritDiveReelGeometry.HELIX_R0 + SpiritDiveReelGeometry.HELIX_RK * SpiritDiveReelGeometry.BODY_TOP + 3f);
    }
}
