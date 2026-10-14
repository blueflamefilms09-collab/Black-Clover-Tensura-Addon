package com.newuniverse.nusmp.anim;

/**
 * What the client knows about ONE entity's hold circle (pure Java: no Minecraft types, so it is unit tested). The server only sends signs
 * of life ({@link CastHold}); this object turns them into the circle's look on the game clock: when it opened, how big it is (eased between
 * the charge steps), whether it is fading, and the opacity. A new message never restarts the pop-open, the spin or the fade, and a
 * circle that is brought back while it fades eases up from where it was instead of jumping.
 */
public final class CastHoldView {
    /** Ticks the circle fades in over when it first opens. */
    public static final float FADE_IN = 5f;
    /** Ticks a circle that was fading takes to come back at full opacity. */
    public static final float RESUME = 4f;
    /** Ticks the circle takes to grow to the next charge step. */
    public static final float GROW = 6f;

    private final long born;
    private float fromPower, toPower;
    private long powerAt;
    private boolean ending;
    private long endsAt;
    private float resumeFrom;
    private long resumeAt = -1;

    private CastHoldView(long now, float power) {
        born = now;
        fromPower = toPower = power;
        powerAt = now;
    }

    /** The first START: the circle opens now. */
    public static CastHoldView open(long now, float power) { return new CastHoldView(now, power); }

    /** START / LEVEL / RENEW: a sign of life with the circle's current power. Brings a fading circle back. */
    public void renew(long now, float power) {
        if (ending) {
            resumeFrom = alpha(now, Float.MAX_VALUE);
            resumeAt = now;
            ending = false;
        }
        if (power != toPower) {
            fromPower = power(now);
            toPower = power;
            powerAt = now;
        }
    }

    /** END: hold through the wind-up and fade; the circle is gone 'duration' ticks from now. */
    public void end(long now, int duration) {
        ending = true;
        endsAt = now + duration;
    }

    public boolean ending() { return ending; }

    /** Ticks since the circle first opened (the spin and pop-open clock: never restarted by a renewal). */
    public float age(float t) { return t - born; }

    /** The circle's power at game time t, eased between the charge steps. */
    public float power(float t) {
        float k = Math.max(0f, Math.min(1f, (t - powerAt) / GROW));
        k = k * k * (3f - 2f * k);
        return fromPower + (toPower - fromPower) * k;
    }

    /**
     * The circle's opacity (0..1) at game time t. {@code ownerLeft} is how many ticks the newest message's effect still lives: a circle
     * that hears nothing fades over its last {@link CastHold#END_TAIL} ticks (a lost release leaves nothing behind); pass a large value to ignore it.
     */
    public float alpha(float t, float ownerLeft) {
        float in = Math.max(0f, Math.min(1f, (t - born) / FADE_IN));
        if (resumeAt >= 0) {
            float k = Math.max(0f, Math.min(1f, (t - resumeAt) / RESUME));
            in = resumeFrom + (1f - resumeFrom) * k;                  // brought back: ease up from where it was (the first fade-in is long over or already inside resumeFrom)
        }
        float out = ending ? (endsAt - t) / CastHold.END_TAIL : ownerLeft / CastHold.END_TAIL;
        return Math.min(in, Math.max(0f, Math.min(1f, out)));
    }
}
