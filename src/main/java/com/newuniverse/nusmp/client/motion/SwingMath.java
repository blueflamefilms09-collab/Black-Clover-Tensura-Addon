package com.newuniverse.nusmp.client.motion;

/**
 * Why: a whip, a flail and a morning star must lag behind the swing instead of staying rigid (docs/vassal_forms_v2.md). The mesh carries a chain of bones ({@link SwingRig}); this is the pure maths
 * that moves them, with no Minecraft class in it so it is unit-tested and so tools/weaponmotion/swing_math.py (the preview) can be its exact twin.
 * <p>
 * A bone is a damped spring on its parent's frame. When the arm's angular velocity changes (the strike speeds up, then stops) every frame down the chain is accelerated, and a bone answers with
 * {@code a = -w^2 th - 2 zeta w th' - gain * (arm acceleration + the relative accelerations of the bones above it)}: the head trails behind the strike, then whips past it when the arm stops, and
 * rings out. The input is the swing's progress {@code u} (0..1, vanilla's attack animation) and its rate, so it is smooth and deterministic: no frame-to-frame differencing, no noise.
 * <p>
 * Cost: one call is at most {@link #MAX_SUBSTEPS} x {@link #MAX_BONES} spring updates (about 4 x 32 x 12 flops), allocation free.
 */
public final class SwingMath {
    private SwingMath() {}

    public static final int MAX_BONES = 32;
    /** Peak angular velocity of the arm during a strike, rad/s (the strike's velocity is {@code PEAK_OMEGA * sin(pi u)}). */
    public static final float PEAK_OMEGA = 9f;
    /** Biggest arm acceleration fed in, rad/s^2 (a slow-motion or a very fast attack speed never kicks the chain over). */
    public static final float MAX_ARM_ACCEL = 240f;
    /** The longest frame time simulated, seconds; a longer gap (a pause, a lag spike, another world) restarts the chain at rest. */
    public static final float RESET_GAP = 0.5f;
    public static final float MAX_STEP = 0.05f;
    public static final float SUBSTEP = 1f / 60f;
    public static final int MAX_SUBSTEPS = 4;
    /** Beyond this distance (blocks) a weapon is drawn rigid: nothing is simulated, nothing is stored. */
    public static final float LOD_DISTANCE = 24f;
    public static final float LOD_DISTANCE_SQ = LOD_DISTANCE * LOD_DISTANCE;
    /** Share of a bone's angle limit the idle sway may use. */
    public static final float IDLE_FRACTION = 0.25f;
    /** The out-of-plane flutter: radians per (rad/s) of the bone's velocity, divided by w. */
    public static final float FLUTTER = 0.35f;

    /** Arm angular acceleration (rad/s^2) at swing progress {@code u} (0..1) when the progress advances {@code rate} per second; 0 outside the swing. */
    public static float armAccel(float u, float rate) {
        if (!(u > 0f) || !(u < 1f) || !(rate > 0f)) return 0f;
        float a = PEAK_OMEGA * (float) Math.PI * (float) Math.cos(Math.PI * u) * rate;
        return Math.max(-MAX_ARM_ACCEL, Math.min(MAX_ARM_ACCEL, a));
    }

    /** How many substeps a frame of {@code dt} seconds takes. */
    public static int substeps(float dt) {
        return Math.max(1, Math.min(MAX_SUBSTEPS, (int) Math.ceil(dt / SUBSTEP)));
    }

    /**
     * Advances the chain by {@code dt} seconds (at most {@link #MAX_STEP}, the caller resets after {@link #RESET_GAP}). Bone 0 hangs on the weapon, bone i on bone i-1.
     *
     * @param th     bone angles (rad), in and out
     * @param thv    bone angular velocities (rad/s), in and out
     * @param armAcc the arm's angular acceleration for this frame ({@link #armAccel})
     */
    public static void step(float[] th, float[] thv, int n, float[] w, float[] zeta, float[] gain, float[] maxRad, float armAcc, float dt) {
        if (!(dt > 0f)) return;
        dt = Math.min(dt, MAX_STEP);
        int sub = substeps(dt);
        float h = dt / sub;
        for (int s = 0; s < sub; s++) {
            float acc = armAcc;
            for (int i = 0; i < n; i++) {
                float a = -w[i] * w[i] * th[i] - 2f * zeta[i] * w[i] * thv[i] - gain[i] * acc;
                float v = thv[i] + a * h;
                float x = th[i] + v * h;
                float m = maxRad[i];
                if (x > m) {
                    x = m;
                    if (v > 0f) v = 0f;
                } else if (x < -m) {
                    x = -m;
                    if (v < 0f) v = 0f;
                }
                if (Float.isNaN(x) || Float.isNaN(v)) {
                    x = 0f;
                    v = 0f;
                }
                th[i] = x;
                thv[i] = v;
                acc += a;
            }
        }
    }

    /** The idle sway of bone {@code i} (-1..1): a slow wave running down the chain, so a held weapon is never dead still; {@code move} (0..1) is how much the holder walks. */
    public static float idle(float timeSec, int i, float phase, float move) {
        float amp = 0.35f + 0.65f * Math.max(0f, Math.min(1f, move));
        return amp * (float) Math.sin(timeSec * 2.6f - i * 0.9f + phase);
    }

    /** True when a squared distance is inside the level-of-detail range. */
    public static boolean near(float distSq) {
        return distSq <= LOD_DISTANCE_SQ;
    }
}
