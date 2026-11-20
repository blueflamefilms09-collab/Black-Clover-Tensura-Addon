package com.newuniverse.nusmp.visual.shields;

/**
 * 1.110.0: where a shield mesh sits on vanilla's shield frame (no Minecraft classes; client/WeaponRenderer applies the numbers).
 * <p>
 * Vanilla draws the shield entity model from the origin of the item pose (the block corner, after ItemRenderer's {@code translate(-0.5)}) with
 * {@code scale(1, -1, -1)}: the 12 x 22 pixel plate is centred on x = 0, y = 0, its emblem face looks toward +z (front plane z = 2/16, back plane 1/16), the handle
 * sticks out behind it toward -z, and up is +y. The display values of {@link ShieldPreset} are written for exactly that frame, so a mesh that is put on the same
 * frame turns, moves and scales with the hand the way the vanilla shield does.
 * <p>
 * The shield meshes of the mod are built face toward +z, handle (straps) toward -z, point or top toward +y (checked by ShieldFrameTest over every shipped mesh). They
 * are scaled uniformly to fit the box of {@link #MAX_W} x {@link #MAX_H} blocks (a vanilla plate is 0.75 x 1.375), centred on the plate's centre, and slid so the front of
 * the face (the highest z of the mesh) lies on the plate's front plane.
 */
public final class ShieldFrame {
    private ShieldFrame() {}

    /** The plate's front plane (the emblem side) of vanilla's shield model after its flip, in blocks. */
    public static final float FACE_Z = 2f / 16f;
    /** The largest footprint of a mesh, in blocks: wider than the vanilla plate (0.75) for the winged Celestial Support, shorter than its 1.375. */
    public static final float MAX_W = 1.1f, MAX_H = 1.0f;
    /** 4.71.0: the footprint before this version (1.3 x 1.2): a round or spiked shield stood out far beyond the arm; {@code -Danime_multiverse.shieldSize=old} restores it. */
    public static final float OLD_MAX_W = 1.3f, OLD_MAX_H = 1.2f;
    public static boolean oldSize() { return "old".equalsIgnoreCase(System.getProperty("anime_multiverse.shieldSize", "new")); }
    public static float maxW() { return oldSize() ? OLD_MAX_W : MAX_W; }
    public static float maxH() { return oldSize() ? OLD_MAX_H : MAX_H; }

    /**
     * 4.36.2: the footprint box in FIRST person, in blocks: vanilla's own plate width (12 / 16) and the usual height. Vanilla's first-person display multiplies a shield by 1.25 and
     * holds it close to the camera, so the 1.3 wide box above filled the screen; this keeps a mod shield the size of a vanilla one in the hand.
     */
    public static final float FP_MAX_W = 12f / 16f, FP_MAX_H = 1.2f;

    /** Client kill switch: {@code -Danime_multiverse.shieldFirstPerson=old} restores the 1.110.0 first-person size (the same box as every other view). */
    public static boolean firstPersonVanillaSize() { return !"old".equalsIgnoreCase(System.getProperty("anime_multiverse.shieldFirstPerson", "vanilla")); }

    /** {scale, tx, ty, tz}: a mesh point p goes to {@code t + scale * p}. */
    public static float[] fit(float xmin, float xmax, float ymin, float ymax, float zmax) { return fit(xmin, xmax, ymin, ymax, zmax, MAX_W, MAX_H); }

    /** 4.44.1: the lowest the back (the straps) of a mesh may reach behind the plate; vanilla's handle ends at -5/16, the fist holding it fills about -4/16..0. */
    public static final float BACK_MIN = -0.25f;
    /** 4.44.1: the deepest a mesh may be (face to straps) after scaling: a spiked form is shrunk a little instead of sticking a block out in front of the player. */
    public static final float MAX_DEPTH = 0.7f;

    /**
     * 4.44.1: the same fit, depth-aware. A flat shield (back above {@link #BACK_MIN} once its face is on the front plane) lands exactly as before; a deep one (the forms of the
     * Legendary Shield: spikes, eggs, balloons) keeps its straps in the hand ({@code BACK_MIN}) and grows forward, so it is neither buried in the arm nor floating away from it.
     */
    public static float[] fit(float xmin, float xmax, float ymin, float ymax, float zmin, float zmax, float maxW, float maxH) {
        float[] f = fit(xmin, xmax, ymin, ymax, zmax, maxW, maxH);
        float d = zmax - zmin;
        if (!(d > 0f) || Float.isNaN(zmin)) return f;
        if (f[0] * d > MAX_DEPTH) f = fit(xmin, xmax, ymin, ymax, zmax, MAX_DEPTH / d * (xmax - xmin), MAX_DEPTH / d * (ymax - ymin));
        float back = f[3] + f[0] * zmin;
        if (back < BACK_MIN) f[3] += BACK_MIN - back;
        return f;
    }

    /** The same fit into a box of {@code maxW} x {@code maxH} blocks. */
    public static float[] fit(float xmin, float xmax, float ymin, float ymax, float zmax, float maxW, float maxH) {
        float w = xmax - xmin, h = ymax - ymin;
        if (!(w > 0f) || !(h > 0f) || Float.isNaN(zmax)) return new float[]{1f, 0f, 0f, 0f};
        float k = Math.min(maxW / w, maxH / h);
        return new float[]{k, -k * (xmin + xmax) * 0.5f, -k * (ymin + ymax) * 0.5f, FACE_Z - k * zmax};
    }

    /** Where a point of the mesh ends up on the shield frame. */
    public static float[] apply(float[] fit, float x, float y, float z) {
        return new float[]{fit[1] + fit[0] * x, fit[2] + fit[0] * y, fit[3] + fit[0] * z};
    }
}
