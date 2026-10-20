package com.newuniverse.nusmp.client.playermodel;

/**
 * Why: the poses the dungeon's bodies need on top of the vanilla animation that {@link PosePatterns} does not have: a puppet throwing a bone shard, and the moves of the Goliath, a six-block giant
 * that used to play a clip of its own for each of them (slam, stomp, roar, fist, line, rubble) and now plays them as arm, leg, head and back poses on the player model, paced the same way: the
 * move's wind-up progress runs 0 to 1, the blow lands at 80 percent of it, the rest is the settling after. Pure functions into a {@link PoseState}; rotations are the model's radians (an arm
 * straight forward is -PI/2, straight up -PI). Every curve is smooth at 0 and at 1, so a pose never pops.
 */
public final class DungeonPoses {
    private DungeonPoses() {}

    /** The moment of a wind-up at which the blow lands (the Goliath's clips were paced to it). */
    public static final float LANDS_AT = 0.8f;

    private static float smooth(float t) {
        float x = PoseState.clamp01(t);
        return x * x * (3f - 2f * x);
    }

    /** The right arm goes up and back, then throws forward: {@code progress} 0 to 1 over the throw. */
    public static void throwArm(PoseState p, float progress, float age) {
        float wind = smooth(progress / 0.55f), release = smooth((progress - 0.55f) / 0.3f);
        float x = -2.6f * wind * (1f - release) + (-1.1f) * release;
        p.pull(PoseState.ARM_R, 1f, x, 0f, -0.25f * (1f - release));
        p.pull(PoseState.ARM_L, 0.5f * wind, -0.5f, 0.2f, 0.1f);
        p.pull(PoseState.BODY, 0.6f * wind, -0.2f * (1f - release) + 0.25f * release, 0f, 0f);
        p.pull(PoseState.HEAD, 0.4f * wind, -0.2f, 0f, 0f);
    }

    /** Both fists overhead, then down on the floor. */
    public static void slam(PoseState p, float progress) {
        float up = smooth(progress / LANDS_AT), down = smooth((progress - LANDS_AT) / (1f - LANDS_AT) * 3f);
        float x = -2.9f * up * (1f - down) - 0.3f * down;
        p.pull(PoseState.ARM_R, 1f, x, 0f, -0.18f);
        p.pull(PoseState.ARM_L, 1f, x, 0f, 0.18f);
        p.pull(PoseState.BODY, 1f, -0.25f * up * (1f - down) + 0.4f * down, 0f, 0f);
        p.pull(PoseState.HEAD, 0.6f, -0.3f * up * (1f - down) + 0.3f * down, 0f, 0f);
    }

    /** One knee up, then the whole weight down on the foot. */
    public static void stomp(PoseState p, float progress) {
        float lift = smooth(progress / LANDS_AT) * (1f - smooth((progress - LANDS_AT) / (1f - LANDS_AT) * 3f));
        p.pull(PoseState.LEG_R, 1f, -1.2f * lift, 0f, 0f);
        p.pull(PoseState.ARM_R, 0.8f, 0.5f * lift, 0f, -0.5f * lift);
        p.pull(PoseState.ARM_L, 0.8f, -0.5f * lift, 0f, 0.5f * lift);
        p.pull(PoseState.BODY, 0.8f, 0.3f * smooth((progress - LANDS_AT) / (1f - LANDS_AT) * 3f), 0f, 0f);
    }

    /** The head thrown back, the chest out and the arms flung wide. */
    public static void roar(PoseState p, float progress, float age) {
        float k = smooth(progress / 0.5f) * (1f - 0.4f * smooth((progress - LANDS_AT) * 5f));
        float shake = (float) Math.sin(age * 1.7f) * 0.05f * k;
        p.pull(PoseState.HEAD, k, -0.7f, 0f, 0f);
        p.pull(PoseState.BODY, k, -0.22f, 0f, 0f);
        p.pull(PoseState.ARM_R, k, -0.3f, 0f, -1.15f + shake);
        p.pull(PoseState.ARM_L, k, -0.3f, 0f, 1.15f - shake);
    }

    /** The right fist pulled far back, then driven forward. */
    public static void fist(PoseState p, float progress) {
        float back = smooth(progress / 0.6f), punch = smooth((progress - 0.6f) / 0.25f);
        float x = 1.0f * back * (1f - punch) - 1.6f * punch;
        p.pull(PoseState.ARM_R, 1f, x, 0f, -0.1f);
        p.pull(PoseState.ARM_L, 0.6f, -0.9f, 0f, 0.3f);
        p.pull(PoseState.BODY, 0.8f, -0.25f * back * (1f - punch) + 0.3f * punch, 0.25f * back * (1f - punch) - 0.45f * punch, 0f);
    }

    /** Both arms level in front, then swept outward along the ground (the seismic line). */
    public static void line(PoseState p, float progress) {
        float reach = smooth(progress / 0.5f), sweep = smooth((progress - 0.5f) / 0.3f);
        p.pull(PoseState.ARM_R, 1f, -1.45f * reach - 0.1f * sweep, 0f, -1.0f * sweep);
        p.pull(PoseState.ARM_L, 1f, -1.45f * reach - 0.1f * sweep, 0f, 1.0f * sweep);
        p.pull(PoseState.BODY, 0.7f, 0.3f * reach, 0f, 0f);
    }

    /** Both arms raised and shaking: the ceiling comes down. */
    public static void rubble(PoseState p, float progress, float age) {
        float k = smooth(progress / 0.5f);
        float shake = (float) Math.sin(age * 1.4f) * 0.06f * k;
        p.pull(PoseState.ARM_R, k, -2.7f + shake, 0f, -0.3f);
        p.pull(PoseState.ARM_L, k, -2.7f - shake, 0f, 0.3f);
        p.pull(PoseState.HEAD, k * 0.7f, -0.45f, 0f, 0f);
    }

    /** Rising out of the floor: the fists come up first, so the arms reach overhead and settle as the body clears the ground ({@code rise} 0 to 1). */
    public static void rise(PoseState p, float rise) {
        float k = 1f - smooth((rise - 0.55f) / 0.45f);
        if (rise >= 1f || k <= 0f) return;
        p.pull(PoseState.ARM_R, k, -2.8f, 0f, -0.35f);
        p.pull(PoseState.ARM_L, k, -2.8f, 0f, 0.35f);
        p.pull(PoseState.HEAD, k * 0.6f, -0.35f, 0f, 0f);
    }

    /**
     * The pose of a Goliath move by the clip name its table gives it ({@code GoliathPose}: slam, stomp, roar, fist, line, rubble, cast_big); false when the name is not one of them (the caller
     * falls back to the generic cast).
     */
    public static boolean goliath(String clip, PoseState p, float progress, float age) {
        switch (clip) {
            case "slam" -> slam(p, progress);
            case "stomp" -> stomp(p, progress);
            case "roar" -> roar(p, progress, age);
            case "fist" -> fist(p, progress);
            case "line" -> line(p, progress);
            case "rubble" -> rubble(p, progress, age);
            case "cast_big" -> PosePatterns.cast(p, progress, true, age);
            default -> {
                return false;
            }
        }
        return true;
    }
}
