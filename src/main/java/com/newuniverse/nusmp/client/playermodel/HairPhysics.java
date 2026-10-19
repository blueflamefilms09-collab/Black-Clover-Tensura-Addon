package com.newuniverse.nusmp.client.playermodel;

/**
 * Why: {@link HairSpring} needs to be told where a lock is being pulled; this turns what the entity does (its speed, its acceleration, its turn, the nod of its head, the lean of its body) into
 * that pull, in plain floats, in the model's frame, so it can be unit-tested and tuned in one place. Angles are radians with the sign convention of the spring: pitch positive = the tip goes to
 * the back, roll positive = the tip goes to the entity's right.
 * <ul>
 *   <li><b>wind</b>: a moving body streams the lock backwards, in proportion to its forward speed (capped);</li>
 *   <li><b>inertia</b>: speeding up lags the lock backwards, slowing down throws it forwards; a sideways acceleration lags it the other way; a turn swings it away from the turn;</li>
 *   <li><b>gravity</b>: the lock hangs straight down in the world, so it turns against the nod of the head (or the lean of the body it hangs from) by {@code gravityFollow};</li>
 *   <li><b>idle</b>: a slow breathing sway so a standing character is never frozen.</li>
 * </ul>
 */
public final class HairPhysics {
    private HairPhysics() {}

    /** What the entity is doing this frame, in the entity's own frame. */
    public record Input(float headPitch, float headRoll, float bodyPitch, float speedForward, float accelForward, float accelLeft, float yawRate, float age) {
        public static final Input STILL = new Input(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
    }

    /**
     * How strongly one lock reacts.
     *
     * @param wind          pitch per block/tick of forward speed
     * @param accel         pitch per block/tick^2 of forward acceleration (and roll per block/tick^2 of sideways acceleration)
     * @param turn          roll per radian/tick of turning
     * @param gravityFollow 0..1: how fully the lock stays hanging straight when the head or body tilts
     * @param fromBody      true: the lock hangs from the body (follows its lean), false: from the head (follows its nod and roll)
     * @param idle          radians of the standing sway
     * @param phase         offset of that sway so neighbouring locks do not move in step
     */
    public record Tuning(float wind, float accel, float turn, float gravityFollow, boolean fromBody, float idle, float phase) {
        public static final Tuning HEAD_LOCK = new Tuning(1.5f, 9f, 0.9f, 0.85f, false, 0.035f, 0f);
        public static final Tuning BODY_LOCK = new Tuning(1.9f, 10f, 0.9f, 0.9f, true, 0.03f, 1.3f);
        /** Stiff hair on the head (a mane, spikes): hardly any gravity, little wind. */
        public static final Tuning STIFF = new Tuning(0.5f, 3f, 0.35f, 0.15f, false, 0.01f, 0.6f);

        public Tuning withPhase(float p) { return new Tuning(wind, accel, turn, gravityFollow, fromBody, idle, p); }

        /** The same lock as seen from a segment that hangs from the body (it follows the lean of the body, not the nod of the head). */
        public Tuning asBody() { return new Tuning(wind, accel, turn, Math.max(gravityFollow, 0.6f), true, idle, phase); }

        /** The tuning a hair chain's {@code tuning} word names ({@code head}, {@code body}, {@code stiff}) with its phase. */
        public static Tuning named(String word, float phase) {
            return switch (word == null ? "head" : word) {
                case "body" -> BODY_LOCK.withPhase(phase);
                case "stiff" -> STIFF.withPhase(phase);
                default -> HEAD_LOCK.withPhase(phase);
            };
        }
    }

    /** The largest pitch the wind alone may cause, radians. */
    public static final float MAX_WIND = 0.75f;
    /** The largest angle inertia may add, radians. */
    public static final float MAX_INERTIA = 0.6f;

    /** Fills {@code out[0]} with the target pitch and {@code out[1]} with the target roll of a whole lock. */
    public static void target(Input in, Tuning t, float[] out) {
        float wind = clamp(in.speedForward() * t.wind(), MAX_WIND);
        float inertia = clamp(in.accelForward() * t.accel(), MAX_INERTIA);
        float sideInertia = clamp(in.accelLeft() * t.accel(), MAX_INERTIA);
        float swing = clamp(in.yawRate() * t.turn(), MAX_INERTIA);
        float tilt = t.fromBody() ? in.bodyPitch() : in.headPitch();
        float gravityPitch = -tilt * t.gravityFollow();
        float gravityRoll = t.fromBody() ? 0f : -in.headRoll() * t.gravityFollow();
        float sway = (float) Math.sin(in.age() * 0.09f + t.phase()) * t.idle();
        float swayRoll = (float) Math.sin(in.age() * 0.071f + t.phase() * 1.7f + 1.1f) * t.idle() * 0.7f;
        out[0] = wind + inertia + gravityPitch + sway;                       // forward acceleration lags the tip to the back (+)
        out[1] = sideInertia + swing + gravityRoll + swayRoll;               // leftward acceleration and a left turn lag the tip to the right (+)
    }

    /**
     * The kick a landing gives, radians per tick, from the vertical speed (blocks per tick, negative = falling) the body had just before it touched down: 0 for a step, about 0.08 for a short
     * drop and capped at 0.3.
     */
    public static float landingKick(float fallSpeed) {
        float v = -fallSpeed;
        if (v < 0.18f) return 0f;
        return Math.min(0.3f, (v - 0.18f) * 0.45f + 0.04f);
    }

    /** A kick for a jump: weaker, from the upward speed of the take-off. */
    public static float jumpKick(float upSpeed) {
        if (upSpeed < 0.3f) return 0f;
        return Math.min(0.12f, (upSpeed - 0.3f) * 0.35f + 0.02f);
    }

    private static float clamp(float v, float max) { return v < -max ? -max : Math.min(v, max); }
}
