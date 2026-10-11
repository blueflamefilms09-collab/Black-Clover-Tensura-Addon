package com.newuniverse.nusmp.client.ui;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.Random;

/**
 * 0.100: the poses of the player figure on the main page of the Codex ({@link CodexScreen}). A pose is a set of limb rotations
 * (radians, in the vanilla player model's convention), a lean about the hips, how high the figure floats and a slow "breath".
 * {@link #apply} runs from {@code mixin.client.PlayerModelMixin} and only touches the one entity the screen is drawing
 * ({@link #subject}); everything else in the world keeps its vanilla pose. Poses are blended, so a change eases from the last one.
 * To add a pose, append an entry to {@link #ALL}.
 */
public final class UiPoses {
    private UiPoses() {}

    /** The entity the Codex is drawing right now (null the rest of the time). */
    public static volatile LivingEntity subject;

    // field indices of Pose.v
    static final int HEAD_X = 0, HEAD_Y = 1, ARM_R_X = 2, ARM_R_Y = 3, ARM_R_Z = 4, ARM_L_X = 5, ARM_L_Y = 6, ARM_L_Z = 7,
            LEG_R_X = 8, LEG_R_Y = 9, LEG_R_Z = 10, LEG_L_X = 11, LEG_L_Y = 12, LEG_L_Z = 13, LEAN = 14, LIFT = 15, SWAY = 16, N = 17;

    public record Pose(String name, String caption, float[] v) {}

    private static float[] v(Object... kv) {
        float[] a = new float[N];
        for (int i = 0; i < kv.length; i += 2) a[(Integer) kv[i]] = ((Number) kv[i + 1]).floatValue();
        return a;
    }

    /** Every pose. New ones go at the end. */
    public static final java.util.List<Pose> ALL = java.util.List.of(
            new Pose("standing", "Ready", v(ARM_R_Z, 0.09, ARM_L_Z, -0.09, SWAY, 1)),
            new Pose("sitting", "Resting", v(LEG_R_X, -1.45, LEG_L_X, -1.45, LEG_R_Y, 0.22, LEG_L_Y, -0.22, ARM_R_X, -0.5, ARM_L_X, -0.5, ARM_R_Z, 0.15, ARM_L_Z, -0.15, HEAD_X, 0.12, LIFT, -0.68)),
            new Pose("bowing", "A deep bow", v(LEAN, 0.78, HEAD_X, 0.25, ARM_R_X, 0.2, ARM_L_X, 0.2, ARM_R_Z, 0.1, ARM_L_Z, -0.1)),
            new Pose("casting", "Casting", v(ARM_R_X, -1.45, ARM_R_Y, -0.15, ARM_L_Z, -1.1, ARM_L_X, -0.2, LEG_R_X, -0.25, LEG_L_X, 0.2, HEAD_X, -0.1, SWAY, 1)),
            new Pose("floating", "Weightless", v(ARM_R_Z, 0.7, ARM_L_Z, -0.7, LEG_R_Z, 0.12, LEG_L_Z, -0.12, LEG_R_X, 0.18, LEG_L_X, -0.1, HEAD_X, -0.15, LIFT, 0.35, SWAY, 3)),
            new Pose("meditating", "Meditating", v(LEG_R_X, -1.3, LEG_L_X, -1.3, LEG_R_Y, 0.9, LEG_L_Y, -0.9, ARM_R_X, -0.7, ARM_L_X, -0.7, ARM_R_Z, 0.35, ARM_L_Z, -0.35, HEAD_X, 0.18, LIFT, -0.66)),
            new Pose("victory", "Victory", v(ARM_R_X, -3.0, ARM_R_Z, 0.25, ARM_L_Z, -0.2, LEG_R_Z, 0.1, LEG_L_Z, -0.1, HEAD_X, -0.2, SWAY, 1)),
            new Pose("salute", "At attention", v(ARM_R_X, -2.5, ARM_R_Y, 0.9, ARM_L_Z, -0.05, HEAD_X, -0.05)),
            new Pose("ready", "On guard", v(ARM_R_X, -1.2, ARM_R_Y, -0.35, ARM_L_X, -1.0, ARM_L_Y, 0.5, LEG_R_X, -0.4, LEG_L_X, 0.35, LEG_R_Y, 0.15, LEG_L_Y, 0.15, LEAN, 0.12)));

    private static final Random RNG = new Random();
    private static float[] from = new float[N], to = new float[N], shown = new float[N];
    private static float blend = 1f;
    private static int current = 0;

    /** Starts easing to a random other pose. Returns its index. */
    public static int roll() {
        int n;
        do { n = RNG.nextInt(ALL.size()); } while (n == current && ALL.size() > 1);
        return set(n);
    }

    public static int set(int index) {
        current = Mth.clamp(index, 0, ALL.size() - 1);
        from = shown.clone();
        to = ALL.get(current).v();
        blend = 0f;
        return current;
    }

    public static Pose current() { return ALL.get(current); }

    /** Advances the blend by 'dt' ticks. Called once per frame by the screen. */
    public static void step(float dt, float time) {
        blend = Math.min(1f, blend + dt / 14f);
        float e = blend * blend * (3 - 2 * blend);
        for (int i = 0; i < N; i++) shown[i] = Mth.lerp(e, from[i], to[i]);
        float sway = Mth.lerp(e, from[SWAY], to[SWAY]);
        shown[LIFT] += Mth.sin(time * 0.07f) * 0.04f * Math.max(sway, 0.35f);
    }

    /** How high the figure is lifted (blocks, negative = lowered): the screen applies it when positioning the model. */
    public static float lift() { return shown[LIFT]; }

    /** Called after vanilla has posed a player model. */
    public static void apply(PlayerModel<?> m, LivingEntity e, float age) {
        if (e != subject) return;
        float[] p = shown;
        float breath = Mth.sin(age * 0.09f) * 0.03f;
        m.head.xRot = p[HEAD_X];
        m.head.yRot = 0f;
        m.rightArm.xRot = p[ARM_R_X] + breath;
        m.rightArm.yRot = p[ARM_R_Y];
        m.rightArm.zRot = p[ARM_R_Z] + breath;
        m.leftArm.xRot = p[ARM_L_X] - breath;
        m.leftArm.yRot = p[ARM_L_Y];
        m.leftArm.zRot = p[ARM_L_Z] - breath;
        m.rightLeg.xRot = p[LEG_R_X];
        m.rightLeg.yRot = p[LEG_R_Y];
        m.rightLeg.zRot = p[LEG_R_Z];
        m.leftLeg.xRot = p[LEG_L_X];
        m.leftLeg.yRot = p[LEG_L_Y];
        m.leftLeg.zRot = p[LEG_L_Z];
        m.body.xRot = 0f; m.body.yRot = 0f; m.body.zRot = 0f;
        m.head.y = 0f; m.head.z = 0f; m.body.y = 0f; m.body.z = 0f;
        m.rightArm.y = 2f; m.rightArm.z = 0f; m.leftArm.y = 2f; m.leftArm.z = 0f;
        m.rightLeg.y = 12f; m.leftLeg.y = 12f; m.rightLeg.z = 0f; m.leftLeg.z = 0f;
        if (Math.abs(p[LEAN]) > 1e-3f) lean(m, p[LEAN]);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightPants.copyFrom(m.rightLeg);
        m.leftPants.copyFrom(m.leftLeg);
        m.jacket.copyFrom(m.body);
        m.hat.copyFrom(m.head);
    }

    /** Leans the head, torso and arms forward about the hip line (y = 12): positions swing round the hips, rotations follow. */
    private static void lean(PlayerModel<?> m, float a) {
        float sin = Mth.sin(a), cos = Mth.cos(a);
        for (ModelPart part : new ModelPart[]{m.head, m.body, m.rightArm, m.leftArm}) {
            float dy = part.y - 12f, dz = part.z;
            part.y = 12f + dy * cos - dz * sin;
            part.z = dy * sin + dz * cos;
            part.xRot += a;
        }
    }
}
