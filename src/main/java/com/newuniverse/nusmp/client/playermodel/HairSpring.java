package com.newuniverse.nusmp.client.playermodel;

/**
 * Why: hair that hangs from a head has to move like hair: the lock lags behind the head when it starts or stops, streams backwards when the body runs, swings away from a turn, follows gravity
 * when the head nods, and whips when the body lands. This is the whole of that, as a chain of 2 to 4 damped springs (one per segment of the lock) in plain floats with no game class, so the
 * numbers can be tested. Each segment holds a pitch (a swing forwards or backwards, radians, positive = the tip goes to the BACK) and a roll (a swing sideways, positive = the tip goes to the
 * entity's RIGHT, the model's -x), both measured against its parent segment, so the whole lock bends like a rope and the tip lags the root.
 *
 * <p>Use: build one chain per lock with {@link #HairSpring(int, Params)}, then once per frame call {@link #step} with the time since the last call (in ticks) and the two angles the whole lock is
 * pulled to ({@link HairPhysics#target}); read {@link #pitch}/{@link #roll} per segment when drawing. {@link #kick} adds an impulse (a landing). The integration is semi-implicit Euler in
 * sub-steps of at most half a tick with the angles clamped, so it cannot blow up whatever the frame time.
 */
public final class HairSpring {
    /**
     * How a lock behaves.
     *
     * @param stiffness   spring constant of the ROOT segment per tick squared (higher = stays closer to its target), the segments below get softer by {@code soften} each
     * @param soften      each segment is this fraction as stiff as the one above it (0.5..1)
     * @param damping     velocity loss per tick of every segment (0 = rings forever, 1 = dead beat)
     * @param maxAngle    largest relative angle of one segment, radians (the share of the lock's target each segment takes is {@link #share})
     */
    public record Params(float stiffness, float soften, float damping, float maxAngle) {
        public static final Params DEFAULT = new Params(0.16f, 0.72f, 0.16f, 0.62f);
        /** A short stiff tuft or a spike (it hardly swings). */
        public static final Params STIFF = new Params(0.42f, 0.8f, 0.3f, 0.3f);
        /** A heavy braid or a long lock. */
        public static final Params HEAVY = new Params(0.12f, 0.7f, 0.12f, 0.7f);
    }

    private final int n;
    private final Params params;
    private final float[] pitch, roll, vp, vr, k;

    public HairSpring(int segments, Params params) {
        this.n = Math.max(1, Math.min(4, segments));
        this.params = params == null ? Params.DEFAULT : params;
        this.pitch = new float[n];
        this.roll = new float[n];
        this.vp = new float[n];
        this.vr = new float[n];
        this.k = new float[n];
        float s = this.params.stiffness();
        for (int i = 0; i < n; i++) {
            k[i] = s;
            s *= this.params.soften();
        }
    }

    public int segments() { return n; }

    /** The pitch of segment {@code i} against its parent, radians (positive: the tip goes to the back). */
    public float pitch(int i) { return pitch[i]; }

    /** The roll of segment {@code i} against its parent, radians (positive: the tip goes to the entity's right). */
    public float roll(int i) { return roll[i]; }

    /** Puts the chain back to rest. */
    public void reset() {
        for (int i = 0; i < n; i++) pitch[i] = roll[i] = vp[i] = vr[i] = 0f;
    }

    /** The share of the whole lock's angle a segment takes: the tip moves most. Sums to 1. */
    public static float share(int i, int n) {
        if (n == 1) return 1f;
        float total = 0f;
        for (int j = 0; j < n; j++) total += 1f + 0.55f * j;
        return (1f + 0.55f * i) / total;
    }

    /**
     * Advances the chain with one target for the whole lock.
     *
     * @param dtTicks     time since the last step in ticks (clamped to 0..4)
     * @param targetPitch the pitch the whole lock is pulled to (radians, summed over the chain)
     * @param targetRoll  the roll the whole lock is pulled to
     */
    public void step(float dtTicks, float targetPitch, float targetRoll) {
        float[] tp = new float[n], tr = new float[n];
        java.util.Arrays.fill(tp, targetPitch);
        java.util.Arrays.fill(tr, targetRoll);
        step(dtTicks, tp, tr);
    }

    /**
     * Advances the chain with a target per segment: the same as the scalar form, but a segment that hangs from the body takes the pull computed from the body's lean and one that hangs
     * from the head the pull computed from the head's nod. {@code targetPitch[i]} is the whole lock's pull as seen from segment {@code i}'s anchor; the segment takes its {@link #share}.
     */
    public void step(float dtTicks, float[] targetPitch, float[] targetRoll) {
        float dt = Math.max(0f, Math.min(4f, dtTicks));
        if (dt <= 0f) return;
        int subs = Math.max(1, (int) Math.ceil(dt / 0.5f));
        float h = dt / subs;
        float damp = (float) Math.pow(1.0 - params.damping(), h);
        float max = params.maxAngle();
        for (int s = 0; s < subs; s++) {
            for (int i = 0; i < n; i++) {
                float tp = clamp(targetPitch[i] * share(i, n), max), tr = clamp(targetRoll[i] * share(i, n), max);
                // softer segments lag further behind the same target, which is what makes the tip whip
                vp[i] = (vp[i] + k[i] * (tp - pitch[i]) * h) * damp;
                vr[i] = (vr[i] + k[i] * (tr - roll[i]) * h) * damp;
                pitch[i] = clamp(pitch[i] + vp[i] * h, max);
                roll[i] = clamp(roll[i] + vr[i] * h, max);
            }
        }
    }

    /**
     * An impulse (a landing, a hit): adds to the angular speed of every segment, alternating in sign down the chain so the lock whips. {@code strength} is radians per tick (0.05 is a small hop,
     * 0.25 a hard landing).
     */
    public void kick(float strength, float sidewaysShare) {
        float s = Math.max(-0.6f, Math.min(0.6f, strength));
        for (int i = 0; i < n; i++) {
            float sign = (i & 1) == 0 ? 1f : -1f;
            vp[i] += s * sign * (0.6f + 0.4f * i);
            vr[i] += s * sidewaysShare * -sign * (0.6f + 0.4f * i);
        }
    }

    /** The total angle of the chain up to and including segment {@code i} (what the tip of that segment is turned by against the head). */
    public float cumulativePitch(int i) {
        float t = 0f;
        for (int j = 0; j <= i && j < n; j++) t += pitch[j];
        return t;
    }

    public float cumulativeRoll(int i) {
        float t = 0f;
        for (int j = 0; j <= i && j < n; j++) t += roll[j];
        return t;
    }

    private static float clamp(float v, float max) { return v < -max ? -max : Math.min(v, max); }
}
