package com.newuniverse.nusmp.client.playermodel;

/**
 * Why: the vanilla player animations (walking, the attack swing, the bow, crossbow, spyglass and shield poses, sneaking, riding, the fall of a death) come from the model itself, but the mod's
 * characters also cast, talk, slump when they are drained, power up, sit and kneel, and those poses are the repo's own. They are written as plain data into one {@code PoseState} per frame
 * (by an entity's {@link PlayerBody} or by {@link PosePatterns}) and the kit's model applies it after the vanilla animation: for each part the final rotation is
 * {@code vanilla + (target - vanilla) * blend + add}, so a pose can lay on top of the walk (an additive wave), replace it (arms raised to cast: blend 1) or fade in. No game class here: the maths
 * is tested. Rotations are the model's own radians (x positive nods or swings a limb FORWARD-DOWN: a limb pointing straight forward is -PI/2 for an arm, straight up is -PI).
 */
public final class PoseState {
    public static final int HEAD = 0, BODY = 1, ARM_R = 2, ARM_L = 3, LEG_R = 4, LEG_L = 5, PARTS = 6;

    /** 0..1 how far each part is pulled from the vanilla pose to {@link #target}. */
    public final float[] blend = new float[PARTS];
    /** The rotation (x, y, z) the part is pulled to. */
    public final float[][] target = new float[PARTS][3];
    /** A rotation added on top, after the blend. */
    public final float[][] add = new float[PARTS][3];
    /** Sit as if riding (the knees bend and the arms rest): the vanilla riding pose. */
    public boolean sitting;
    /** Blocks the whole body is moved up (a hover); the renderer applies it. */
    public float lift;
    /** Radians the whole body leans forward (positive) about the feet, applied by the renderer. */
    public float lean;

    public void clear() {
        for (int i = 0; i < PARTS; i++) {
            blend[i] = 0f;
            for (int k = 0; k < 3; k++) {
                target[i][k] = 0f;
                add[i][k] = 0f;
            }
        }
        sitting = false;
        lift = 0f;
        lean = 0f;
    }

    /** Pulls a part towards a rotation by {@code amount} (0 leaves the vanilla pose, 1 replaces it). A second pull on the same part keeps the stronger one. */
    public PoseState pull(int part, float amount, float x, float y, float z) {
        float b = clamp01(amount);
        if (b >= blend[part]) {
            blend[part] = b;
            target[part][0] = x;
            target[part][1] = y;
            target[part][2] = z;
        }
        return this;
    }

    /** Adds a rotation to a part on top of everything. */
    public PoseState add(int part, float x, float y, float z) {
        add[part][0] += x;
        add[part][1] += y;
        add[part][2] += z;
        return this;
    }

    /** The final rotation component of a part for the vanilla value {@code vanilla}. */
    public float apply(int part, int axis, float vanilla) {
        return vanilla + (target[part][axis] - vanilla) * blend[part] + add[part][axis];
    }

    public boolean any() {
        if (sitting || lift != 0f || lean != 0f) return true;
        for (int i = 0; i < PARTS; i++) {
            if (blend[i] != 0f) return true;
            for (int k = 0; k < 3; k++) if (add[i][k] != 0f) return true;
        }
        return false;
    }

    public static float clamp01(float v) { return v < 0f ? 0f : Math.min(v, 1f); }
}
