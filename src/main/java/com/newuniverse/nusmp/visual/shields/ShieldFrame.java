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
    public static final float MAX_W = 1.3f, MAX_H = 1.2f;

    /** {scale, tx, ty, tz}: a mesh point p goes to {@code t + scale * p}. */
    public static float[] fit(float xmin, float xmax, float ymin, float ymax, float zmax) {
        float w = xmax - xmin, h = ymax - ymin;
        if (!(w > 0f) || !(h > 0f) || Float.isNaN(zmax)) return new float[]{1f, 0f, 0f, 0f};
        float k = Math.min(MAX_W / w, MAX_H / h);
        return new float[]{k, -k * (xmin + xmax) * 0.5f, -k * (ymin + ymax) * 0.5f, FACE_Z - k * zmax};
    }

    /** Where a point of the mesh ends up on the shield frame. */
    public static float[] apply(float[] fit, float x, float y, float z) {
        return new float[]{fit[1] + fit[0] * x, fit[2] + fit[0] * y, fit[3] + fit[0] * z};
    }
}
