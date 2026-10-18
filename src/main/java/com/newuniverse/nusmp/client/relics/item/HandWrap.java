package com.newuniverse.nusmp.client.relics.item;

import org.joml.Matrix3f;
import org.joml.Quaternionf;

import java.util.Map;

/**
 * Weapons that are worn ON the hand instead of held in it: the Vassal Claw and the Vassal Gauntlet (1.71.0, owner request: "actually be on / wrapped around your hand like a
 * Curios item in the hand slot"). They were drawn like a sword, floating in front of the fist; now their bracer sits round the forearm and their hand plate round the fist.
 *
 * Why a table and a transform rather than new meshes: the two meshes were already modelled as a bracer + hand shell (the forearm along mesh y), so only where they are drawn was wrong.
 * In the third-person hand context vanilla's ItemInHandLayer leaves the pose at the hand: rotated so that item +x points away from the body, item +y forward (the thumb side) and
 * item +z up the arm toward the elbow, with the origin 10 pixels down the arm and 2 pixels in front of the arm's axis. The mesh is therefore turned so that its forearm axis (mesh y)
 * runs along item -z (toward the fingers) and its hand axes lie on the arm's two axes, scaled to the arm's 4 x 4 pixel section and slid so that its mouth sits `mouthPx` pixels below
 * the shoulder. The model json of each carries identity third-person display entries (and "nusmp_display_preset": "manual") so no extra transform comes before this one. First person
 * is unchanged: vanilla draws no arm there, only the item, which already shows the back of the hand with its gem.
 */
public final class HandWrap {
    private HandWrap() {}

    /** Arm geometry of the third-person hand pose, in pixels. */
    public static final float ORIGIN_DOWN_ARM = 10f;
    public static final float ORIGIN_FORWARD = 2f;

    /** `rot` maps mesh axes to item axes (columns: where mesh x, y, z go); sx, sy, sz scale in blocks per mesh unit; mouthPx is where mesh y = 0 sits below the shoulder pivot. */
    public record Wrap(Matrix3f rot, float sx, float sy, float sz, float mouthPx) {
        public Quaternionf quaternion() { return new Quaternionf().setFromNormalized(rot); }

        /** Pixels from the shoulder pivot, along the arm, to a point at mesh height y. */
        public float armPixelOf(float y) { return mouthPx + y * sy * 16f; }
    }

    private static final Map<String, Wrap> WRAPPED = Map.of(
            // gauntlet: back of the hand = mesh +x = outward, thumb = mesh +z = forward; the vambrace mouth is 28 x 28 units, 4.4 pixels at 0.0115
            "vassal_gauntlet", new Wrap(new Matrix3f(1, 0, 0, 0, 0, -1, 0, 1, 0), 0.0115f, 0.0115f, 0.0115f, 2f),
            // claw: hand width = mesh x = forward, back of the hand = mesh z = lateral; the bracer is 35 x 25 units, 4.8 x 4.6 pixels; the blades run on 14 pixels beyond the fist
            "vassal_claw", new Wrap(new Matrix3f(0, 1, 0, 0, 0, -1, -1, 0, 0), 0.0085f, 0.0085f, 0.0115f, 3f));

    public static Wrap of(String id) { return WRAPPED.get(id); }

    public static boolean isWrapped(String id) { return WRAPPED.containsKey(id); }

    /** The mesh's forearm axis after the rotation, as an item-space vector: must be item -z (toward the fingers) for every wrapped weapon. */
    public static float[] forearmAxis(Wrap w) {
        org.joml.Vector3f v = w.rot().transform(new org.joml.Vector3f(0, 1, 0));
        return new float[]{v.x, v.y, v.z};
    }
}
