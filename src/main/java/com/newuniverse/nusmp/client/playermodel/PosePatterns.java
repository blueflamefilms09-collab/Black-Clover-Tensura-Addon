package com.newuniverse.nusmp.client.playermodel;

/**
 * Why: the poses the repo's characters need on top of the vanilla animations, as pure functions that write into a {@link PoseState}: a cast (both arms rise and spread while the power gathers, a big
 * cast raises them overhead and arches the back), a talk (the speaking arm gestures in a slow irregular rhythm while the other rests), a slump (a drained boss hangs its head and sags), a power-up
 * (the arms open, the head goes back, the body shakes), a bow, a read (the arms open as if holding a page), a point (the right arm levels at a target). Each takes a 0..1 amount and the entity's
 * age in ticks, so a caller fades a pose in and out by the amount. Everything is smooth in its inputs (no pop when the amount is 0 or 1).
 */
public final class PosePatterns {
    private PosePatterns() {}

    private static float smooth(float t) {
        float x = PoseState.clamp01(t);
        return x * x * (3f - 2f * x);
    }

    /** Both arms rise and spread to cast. {@code big}: overhead, the back arches. {@code amount} is the progress of the wind-up (it rises quickly and holds). */
    public static void cast(PoseState p, float amount, boolean big, float age) {
        float k = smooth(amount * 2.5f);
        float pulse = (float) Math.sin(age * 0.35f);
        float rise = big ? -2.75f : -1.75f;
        p.pull(PoseState.ARM_R, k, rise + 0.08f * pulse, -0.18f, big ? -0.55f : -0.30f);
        p.pull(PoseState.ARM_L, k, rise + 0.08f * -pulse, 0.18f, big ? 0.55f : 0.30f);
        p.pull(PoseState.HEAD, k * 0.6f, big ? -0.45f : -0.18f, 0f, 0f);
        if (big) p.pull(PoseState.BODY, k * 0.8f, -0.16f, 0f, 0f);
        p.lean = big ? -0.05f * k : 0f;
    }

    /** The speaking arm gestures; {@code amount} fades it. */
    public static void talk(PoseState p, float amount, float age) {
        float k = smooth(amount);
        float beat = (float) (Math.sin(age * 0.17f) * 0.5 + 0.5 * Math.sin(age * 0.11f + 1.3));
        p.pull(PoseState.ARM_R, k * 0.9f, -0.95f + 0.38f * beat, -0.15f, 0.12f + 0.2f * (float) Math.sin(age * 0.09f));
        p.pull(PoseState.ARM_L, k * 0.5f, -0.22f + 0.1f * beat, 0.1f, 0.1f);
        p.add(PoseState.HEAD, 0.06f * k * (float) Math.sin(age * 0.21f), 0.05f * k * (float) Math.sin(age * 0.07f), 0f);
    }

    /** A drained or exposed body: the head hangs, the back rounds, the arms dangle forward. */
    public static void slump(PoseState p, float amount, float age) {
        float k = smooth(amount);
        float breath = (float) Math.sin(age * 0.12f) * 0.03f;
        p.pull(PoseState.HEAD, k * 0.8f, 0.42f + breath, 0f, 0f);
        p.pull(PoseState.BODY, k * 0.7f, 0.28f, 0f, 0f);
        p.pull(PoseState.ARM_R, k * 0.55f, 0.1f, 0f, 0.05f);
        p.pull(PoseState.ARM_L, k * 0.55f, 0.1f, 0f, -0.05f);
        p.lean = 0.0f;
    }

    /** The power-up: arms open to the sides, the head goes back, the whole body trembles. {@code amount} is the progress of the transformation. */
    public static void powerUp(PoseState p, float amount, float age) {
        float k = smooth(amount * 3f) * (1f - 0.5f * smooth((amount - 0.85f) * 6f));
        float shake = (float) Math.sin(age * 1.9f) * 0.045f * k;
        p.pull(PoseState.ARM_R, k, -0.15f, 0f, -1.35f + shake);
        p.pull(PoseState.ARM_L, k, -0.15f, 0f, 1.35f - shake);
        p.pull(PoseState.HEAD, k * 0.8f, -0.5f, 0f, 0f);
        p.pull(PoseState.BODY, k * 0.7f, -0.15f, 0f, 0f);
        p.add(PoseState.BODY, shake, 0f, shake * 0.6f);
    }

    /** Reading: both forearms held out in front as if holding an open page. */
    public static void read(PoseState p, float amount, float age) {
        float k = smooth(amount);
        p.pull(PoseState.ARM_R, k, -1.25f, -0.35f, 0.1f);
        p.pull(PoseState.ARM_L, k, -1.25f, 0.35f, -0.1f);
        p.pull(PoseState.HEAD, k * 0.5f, 0.22f, 0f, 0f);
    }

    /** The right arm levelled forward at a target (a beam, a pointed staff). */
    public static void point(PoseState p, float amount, float age) {
        float k = smooth(amount);
        p.pull(PoseState.ARM_R, k, -1.5707964f, 0f, 0f);
        p.pull(PoseState.ARM_L, k * 0.3f, 0.05f, 0f, 0.05f);
    }

    /** A bow from the waist, {@code amount} 0..1 down and back up as the caller drives it. */
    public static void bow(PoseState p, float amount) {
        float k = smooth(amount);
        p.pull(PoseState.BODY, k, 0.95f, 0f, 0f);
        p.pull(PoseState.HEAD, k * 0.5f, 0.35f, 0f, 0f);
        p.pull(PoseState.ARM_R, k, 0.35f, 0f, -0.1f);
        p.pull(PoseState.ARM_L, k, 0.35f, 0f, 0.1f);
    }

    /** Hovering: a slow bob and a slight forward lean. */
    public static void hover(PoseState p, float height, float age) {
        p.lift = height + 0.06f * (float) Math.sin(age * 0.12f);
    }

    // ---- Kakashi Hatake (docs/bosses/kakashi.md), appended: each takes the 0..1 progress of the move and the age; all finite, all fade with the amount

    /** Raikiri: the right hand drawn back at the hip with the left hand steadying its wrist, the body leaning into the lunge as the amount runs out. */
    public static void raikiri(PoseState p, float amount, float age) {
        float k = smooth(amount * 3f), lunge = smooth((amount - 0.75f) * 4f);
        float chirp = (float) Math.sin(age * 1.7f) * 0.03f;
        p.pull(PoseState.ARM_R, k, -0.55f + lunge * -0.95f + chirp, -0.25f, -0.35f);
        p.pull(PoseState.ARM_L, k * 0.9f, -0.95f + chirp, 0.45f, 0.30f);
        p.pull(PoseState.BODY, k * 0.7f, 0.12f + lunge * 0.25f, -0.18f, 0f);
        p.pull(PoseState.HEAD, k * 0.6f, 0.08f, 0.12f, 0f);
    }

    /** Hand seals: both forearms raised and brought together in front of the chest, the fingers working (a small fast tremble). */
    public static void seals(PoseState p, float amount, float age) {
        float k = smooth(amount * 3f);
        float work = (float) Math.sin(age * 1.3f) * 0.05f * k;
        p.pull(PoseState.ARM_R, k, -1.35f + work, 0.38f, 0.55f);
        p.pull(PoseState.ARM_L, k, -1.35f - work, -0.38f, -0.55f);
        p.pull(PoseState.HEAD, k * 0.5f, 0.14f, 0f, 0f);
        p.pull(PoseState.BODY, k * 0.4f, 0.08f, 0f, 0f);
    }

    /** A throw: the right arm cocked back and up, then whipped forward as the amount passes the middle. */
    public static void throwing(PoseState p, float amount, float age) {
        float wind = smooth(amount * 2f), snap = smooth((amount - 0.5f) * 4f);
        p.pull(PoseState.ARM_R, wind, -2.35f + snap * 1.55f, -0.1f, -0.35f + snap * 0.35f);
        p.pull(PoseState.ARM_L, wind * 0.6f, -0.35f, 0.25f, 0.5f);
        p.pull(PoseState.BODY, wind * 0.5f, -0.06f + snap * 0.2f, -0.35f * (1f - snap), 0f);
    }

    /** The hand to the covered eye: the left forearm raised to the forehead, the head tilted (Sharingan, Kamui). */
    public static void kamuiEye(PoseState p, float amount, float age) {
        float k = smooth(amount * 3f);
        float breath = (float) Math.sin(age * 0.2f) * 0.02f;
        p.pull(PoseState.ARM_L, k, -2.55f + breath, -0.2f, 0.25f);
        p.pull(PoseState.ARM_R, k * 0.5f, -0.25f, -0.1f, -0.15f);
        p.pull(PoseState.HEAD, k * 0.7f, -0.12f, -0.18f, 0.06f);
    }

    /** The arms spread wide and slightly raised, the chest open: the Susanoo, the Six Paths, the burst of lightning. Steadier than the power-up (no shake). */
    public static void spread(PoseState p, float amount, float age) {
        float k = smooth(amount * 2.5f);
        float sway = (float) Math.sin(age * 0.15f) * 0.04f;
        p.pull(PoseState.ARM_R, k, -0.55f + sway, 0f, -1.15f);
        p.pull(PoseState.ARM_L, k, -0.55f - sway, 0f, 1.15f);
        p.pull(PoseState.HEAD, k * 0.6f, -0.28f, 0f, 0f);
        p.pull(PoseState.BODY, k * 0.5f, -0.1f, 0f, 0f);
    }

    /** A sword cut across: the right arm from high over the shoulder down across the body, the body turning with it. */
    public static void slash(PoseState p, float amount, float age) {
        float wind = smooth(amount * 2f), cut = smooth((amount - 0.45f) * 3f);
        p.pull(PoseState.ARM_R, wind, -2.5f + cut * 1.9f, 0.2f - cut * 0.5f, -0.5f + cut * 0.45f);
        p.pull(PoseState.ARM_L, wind * 0.5f, -0.4f, 0.3f, 0.4f);
        p.pull(PoseState.BODY, wind * 0.8f, 0.04f, 0.45f * (1f - 2f * cut), 0f);
    }
}
