package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Element;

/**
 * Spirit Dive, REEL look: the geometry of the look as pure arithmetic on float arrays (the painter allocates nothing; the unit test checks the bounds without a game). Units are model pixels (1/16 block)
 * in the layer space of the player renderer: the origin at the neck, y DOWN (the feet are at y = 24), the front of the player is -z, the back +z, the LEFT arm is at +x. The head frame is the head
 * part's own (the head box is x -4..4, y -8..0, z -4..4); the arm frame is the arm part's own (the arm box of the left arm is x -1..3, y -2..10).
 *
 * <ul>
 *   <li>The <b>crown</b>: a ring of cubes standing on the head, on a square of half side 5.3 px (four corners, mid points, extras), with a vertical bar and a white light on some of them.</li>
 *   <li>The <b>leaf wing</b> (Wind, the hex-membrane panel of the V2 design shaped like the leaf of the video): a parabolic spine from the shoulder blade, out, up and back, folded on the back at the start of the entry.</li>
 *   <li>The <b>comb wing</b> (Water; flame feathers for Fire, crystal blades for Earth): a fan of thin blades from one root, the angle of each from {@link #FAN_DOWN} to {@link #FAN_UP}, the length bell shaped.</li>
 *   <li>The <b>helix</b>: the spiral ribbons of the entry, one canonical spine (rotated for each ribbon: the shared geometry).</li>
 *   <li>The <b>orbit rings</b>, the <b>cage</b> of crossing circles, the <b>sword</b>, the <b>ball of blades</b> and the <b>sparks</b>.</li>
 * </ul>
 */
public final class SpiritDiveReelGeometry {
    private SpiritDiveReelGeometry() {}

    /** The most segments of any strip of the look (the scratch arrays of the ribbon class are this big). */
    public static final int MAX_SEGS = SpiritDiveAnimeGeometry.MAX_SEGS;

    // ------------------------------------------------------------------ the crown (head frame, px)
    /** The cubes of the ring: x, z of the centre. The first eight are the standard tier's (corners and mid points), the next four the character tier's. */
    public static final float[][] CROWN = {
            {-5.2f, -5.2f}, {5.2f, -5.2f}, {5.2f, 5.2f}, {-5.2f, 5.2f},            // corners
            {0f, -5.4f}, {5.4f, 0f}, {0f, 5.4f}, {-5.4f, 0f},                     // mid points
            {-2.6f, -5.3f}, {2.6f, -5.3f}, {2.6f, 5.3f}, {-2.6f, 5.3f}};           // the extras of the front and the back
    /** The cubes that carry a bar (index into {@link #CROWN}): the corners, then the front and back mid points. */
    public static final int[] BARS = {0, 1, 2, 3, 4, 6};

    /** The edge of a crown cube and its height, px: standard, character. */
    public static final float CUBE = 2.9f, CUBE_CHARACTER = 3.2f, CUBE_HEIGHT = 3.0f, CUBE_HEIGHT_CHARACTER = 3.4f;
    /** The y of the foot of the cubes (the top of the head is -8), the foot of the bars and the height of a bar above it, px, by element ordinal (WIND, WATER, FIRE, EARTH). */
    public static final float CROWN_Y = -8.0f;
    public static final float[] BAR_HEIGHT = {3.6f, 3.0f, 4.6f, 3.4f};

    /** Cubes of the ring that are drawn for a spec count; the half dive of Wind draws only the left half (x >= 0). */
    public static int crownCount(int n, boolean leftOnly) {
        int c = 0;
        for (int i = 0; i < Math.min(n, CROWN.length); i++) if (!leftOnly || CROWN[i][0] >= 0f) c++;
        return c;
    }

    /** Bars drawn for a spec count (the first {@code n} of {@link #BARS}); left half only for the half dive of Wind. */
    public static int barCount(int n, boolean leftOnly) {
        int c = 0;
        for (int i = 0; i < Math.min(n, BARS.length); i++) if (!leftOnly || CROWN[BARS[i]][0] >= 0f) c++;
        return c;
    }

    // ------------------------------------------------------------------ the leaf wing (Wind)
    /**
     * A leaf row: root xyz, initial direction (out, up, back), bend (out, up, back), length, half width, wave, phase. Standard tier uses the first two rows, the character tier all three. The numbers are
     * the leaf of the video (B_10): out and up from the shoulder blade, curving outward and back at the tip.
     */
    public static final int LEAF_ROW = 14;
    private static final float[][] LEAF = {
            {3.4f, 1.6f, 2.6f, 0.38f, 0.88f, 0.30f, 0.55f, -0.10f, 0.42f, 24f, 5.0f, 1.3f, 0.0f, 0f},
            {3.2f, 3.4f, 2.6f, 0.82f, 0.42f, 0.30f, 0.40f, -0.12f, 0.36f, 15f, 3.2f, 1.0f, 1.9f, 0f},
            {3.0f, 5.2f, 2.6f, 0.88f, 0.05f, 0.36f, 0.30f, 0.20f, 0.30f, 13f, 2.8f, 0.8f, 3.3f, 0f}};
    /** The leaves of the character tier are a quarter bigger. */
    public static final float LEAF_CHARACTER = 1.18f;

    public static int leafRows() { return LEAF.length; }

    public static float[] leaf(int i) { return LEAF[i]; }

    /**
     * A leaf's spine into {@code out} (3 floats per point, {@code segs + 1} points), px. {@code open} is {@link SpiritDiveReelRules#wingScale} (0 folded on the back .. 1 open, with the overshoot);
     * the leaf is folded on the back (down and behind) while it is closed, swings open to its direction and flaps a little; {@code swing} 0..1 sweeps it back when the wearer walks.
     */
    public static void leafSpine(float[] out, int segs, float[] row, float side, float lenK, float open, float age, float swing) {
        float o = Math.max(0f, open), f = Math.min(1f, o), len = row[9] * lenK * (0.25f + 0.75f * Math.min(o, 1.15f));
        // the folded direction: down along the back; the open direction: the row's own
        float fx = 0.05f, fy = 0.55f, fz = 0.85f;
        float dx = fx + (row[3] - fx) * f, dy = fy + (-row[4] - fy) * f, dz = fz + (row[5] - fz) * f;
        float bx = row[6] * f, by = -row[7] * f, bz = row[8] * f + 0.5f * clamp01(swing);
        float flap = (float) Math.sin(age * 0.11f + row[12]) * 0.06f * f;
        for (int k = 0; k <= segs; k++) {
            float s = k / (float) segs, s2 = s * s;
            float w = row[11] * (float) Math.sin(age * 0.13f + row[12] + s * 4.5f) * s2 * f;
            int i = k * 3;
            out[i] = row[0] * side + side * (len * ((dx + flap) * s + bx * s2) + 0.5f * w);
            out[i + 1] = row[1] + len * (dy * s + by * s2) + 0.3f * w;
            out[i + 2] = row[2] + len * (dz * s + bz * s2) + w;
        }
    }

    // ------------------------------------------------------------------ the comb wing (Water, Fire, Earth)
    /** The fan: the angle of the lowest blade and of the highest, degrees from the outward axis (positive up). */
    public static final float FAN_DOWN = -40f, FAN_UP = 26f;
    /** Floats of a blade: root xyz, direction xyz, bend xyz, length, half width. */
    public static final int BLADE_ROW = 11;
    /** The length of the longest blade, px, by element ordinal (WIND unused, WATER, FIRE, EARTH): standard tier, character tier. */
    public static final float[][] BLADE_LENGTH = {{0f, 0f}, {22f, 27f}, {24f, 29f}, {20f, 20f}};

    private static float clamp01(float x) { return x < 0f ? 0f : x > 1f ? 1f : x; }

    /**
     * The blade {@code i} of {@code n} of one wing into {@code row} (11 floats): the roots sit on a small arc at the shoulder blade, the direction turns through the fan, the length follows a bell with the
     * longest blade a little above the horizontal. {@code spread} (0..1, the unfolding) closes the fan on the back while the wing is not open.
     */
    public static void blade(float[] row, int i, int n, float side, float lenMax, float spread) {
        float u = n <= 1 ? 0.5f : i / (float) (n - 1);
        float deg = FAN_DOWN + (FAN_UP - FAN_DOWN) * u, closed = 1f - clamp01(spread);
        deg = deg * (0.25f + 0.75f * clamp01(spread)) - 30f * closed;                                 // folded: all the blades hang down on the back
        float a = deg * (float) (Math.PI / 180.0), ca = (float) Math.cos(a), sa = (float) Math.sin(a);
        float bell = 0.58f + 0.42f * (float) Math.cos((u - 0.62f) * 1.65f);                           // longest at u 0.62 (a little above the horizontal), shorter at both ends
        float len = lenMax * bell * (0.3f + 0.7f * clamp01(spread));
        row[0] = side * (3.0f + 0.2f * i); row[1] = 1.4f + 0.18f * i; row[2] = 2.7f;                // the root: a small arc along the shoulder blade
        row[3] = side * 0.93f * ca; row[4] = -sa; row[5] = 0.22f * ca + 0.12f * closed;              // out, up (y is down), a little back
        row[6] = side * 0.05f; row[7] = -0.10f * (1f + closed); row[8] = 0.12f;                      // a gentle curl
        row[9] = len;
        row[10] = Math.max(0.9f, 0.6f * len * (float) Math.abs(((FAN_UP - FAN_DOWN) * (float) (Math.PI / 180.0)) / Math.max(1, n - 1)) * 1.06f);
    }

    /** A blade's spine into {@code out} (3 floats per point), px; {@code age} flutters the tip. */
    public static void bladeSpine(float[] out, int segs, float[] row, int i, float age, float swing) {
        float len = row[9];
        float dl = (float) Math.sqrt(row[3] * row[3] + row[4] * row[4] + row[5] * row[5]);
        if (dl < 1e-4f) dl = 1f;
        float dx = row[3] / dl, dy = row[4] / dl, dz = row[5] / dl;
        for (int k = 0; k <= segs; k++) {
            float s = k / (float) segs, s2 = s * s;
            float w = 0.7f * (float) Math.sin(age * 0.12f + i * 0.9f + s * 3.2f) * s2;
            int j = k * 3;
            out[j] = row[0] + len * (dx * s + row[6] * s2);
            out[j + 1] = row[1] + len * (dy * s + row[7] * s2) + 0.4f * w;
            out[j + 2] = row[2] + len * (dz * s + row[8] * s2 + 0.45f * clamp01(swing) * s2) + w;
        }
    }

    // ------------------------------------------------------------------ the helix of the entry
    /** Turns of a ribbon over the full height of the body, the radius at the feet and the growth of the radius with the height (px). */
    public static final float HELIX_TURNS = 1.1f, HELIX_R0 = 11f, HELIX_RK = 0.2f, BODY_TOP = 44f;

    /**
     * The canonical spine of ribbon 0 into {@code out} (3 floats per point, y down, the feet at 24): a comet of {@code tail} px of height ending (at its head) at the height {@code head} above the feet,
     * turning {@link #HELIX_TURNS} turns per {@link #BODY_TOP} px. The other ribbons are this spine turned about y by {@link #ribbonAngle} and moved in height by {@link #ribbonLift}.
     */
    public static void helix(float[] out, int segs, float head, float tail, float spin, float radiusK) {
        for (int k = 0; k <= segs; k++) {
            float s = k / (float) segs;                                                                // 0 = the tail, 1 = the head
            float h = head - tail * (1f - s);
            float ang = spin + HELIX_TURNS * 2f * (float) Math.PI * h / BODY_TOP;
            float r = (HELIX_R0 + HELIX_RK * Math.max(0f, h)) * radiusK;
            int j = k * 3;
            out[j] = r * (float) Math.cos(ang);
            out[j + 1] = 24f - h;
            out[j + 2] = r * (float) Math.sin(ang);
        }
    }

    /** The angle ribbon {@code i} of {@code n} is turned by (radians): evenly round the body. */
    public static float ribbonAngle(int i, int n) { return i * (float) (2.0 * Math.PI) / Math.max(1, n); }

    /** The height offset of ribbon {@code i} (px): the ribbons are staggered so they are not one band. */
    public static float ribbonLift(int i) { return (i % 3) * 2.2f - 2.2f; }

    /** The radius factor of ribbon {@code i}: a few wide ones and a few close to the body. */
    public static float ribbonRadius(int i) { return 0.82f + 0.12f * (i % 4); }

    // ------------------------------------------------------------------ the orbit rings (blocks, y up from the feet)
    /** Rings: height above the feet, radius, tilt (rad), turn speed (rad per tick), bob. The first two are the standard tier's. */
    public static final float[][] ORBIT = {
            {0.92f, 0.64f, 0.10f, 0.045f, 0.0f},
            {1.40f, 0.52f, -0.16f, -0.060f, 1.7f},
            {0.58f, 0.80f, 0.22f, 0.035f, 3.1f}};

    /** The great circles of the cage round the body (blocks): centre height above the feet, the direction the plane turns about (unit axis x, y, z) and the turn speed. */
    public static final float[][] CAGE = {
            {0.82f, 0.0f, 0.0f, 1.0f, 0.00f, 0.70f},
            {0.82f, 1.0f, 0.0f, 0.0f, 0.55f, -0.50f},
            {0.82f, 0.6f, 0.0f, 0.8f, 1.10f, 0.60f},
            {0.82f, -0.6f, 0.0f, 0.8f, 1.65f, -0.65f},
            {0.82f, 0.9f, 0.0f, 0.4f, 2.20f, 0.55f}};

    // ------------------------------------------------------------------ the weapons (body frame, px)
    /**
     * Yuno's sword: the hilt (the magic circle) beside the right hip, the blade out and up. Root x, y, z, the direction (out, up, back), the length of the blade, the half width of the guard, the pommel's
     * length behind the hilt. The sword is a flat strip that always turns its face to the viewer about its own axis; it sways with a slow bob.
     */
    public static final float[] SWORD = {-10.5f, 12.0f, -4.0f, -0.52f, 0.84f, -0.14f, 38f, 6.4f, 9f};
    /** The flame blade of Fuegoleon: on the left, shorter. */
    public static final float[] FLAME_BLADE = {10.5f, 12.5f, -3.5f, 0.50f, 0.85f, -0.14f, 30f, 5.0f, 7f};
    /** Noelle's ball of water blades: its centre (right of the body, at the chest) and the radii of the blades. */
    public static final float[] BALL = {-12.5f, 6.0f, -3.0f, 1.4f, 7.0f, 1.1f};
    /** The crystal spire behind Noelle: the foot (behind the right shoulder), height, half width. */
    public static final float[] SPIRE = {-6.5f, -6.0f, 7.5f, 24f, 4.2f};

    /** The weapon's bob (px) at the renderer's clock. */
    public static float bob(float age, float phase) { return 1.2f * (float) Math.sin(age * 0.07f + phase); }

    // ------------------------------------------------------------------ the sparks
    /** The position of spark {@code i} (blocks, y up from the feet, round the body) at the renderer's clock: out into x, y, z. */
    public static void spark(float[] out, int i, float age) {
        float ph = (age * 0.011f * (1f + 0.3f * (i % 3)) + i * 0.173f) % 1f;
        float a = i * 2.399f + age * 0.02f * (1f + 0.25f * (i % 4));
        float r = 0.55f + 0.28f * (float) Math.sin(i * 1.9f) + 0.1f * ph;
        out[0] = r * (float) Math.cos(a);
        out[1] = 0.1f + 1.9f * ph;
        out[2] = r * (float) Math.sin(a);
        out[3] = ph;
    }

    /** The furthest any point of the look can be from the body's centre line, px (bounds it for the unit test): the largest of the wings, the weapons, the ribbons. */
    public static float reach(Element el) {
        float w = 0f;
        for (float[] r : LEAF) w = Math.max(w, Math.abs(r[0]) + r[9] * LEAF_CHARACTER * 1.5f + r[10] * 2f);
        w = Math.max(w, 3f + BLADE_LENGTH[el.ordinal()][1] * 1.4f);
        w = Math.max(w, Math.abs(SWORD[0]) + SWORD[6] + SWORD[7]);
        return Math.max(w, HELIX_R0 + HELIX_RK * BODY_TOP + 3f);
    }
}
