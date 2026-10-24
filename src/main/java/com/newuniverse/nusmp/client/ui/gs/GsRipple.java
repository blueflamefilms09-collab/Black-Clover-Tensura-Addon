package com.newuniverse.nusmp.client.ui.gs;

/**
 * The slime ripple of a hovered button: up to {@link #MAX_RINGS} expanding rings, started when the pointer enters and one tick-step apart, each
 * ring growing and fading over {@link #LIFE} ticks. Pure timeline maths with no object per ring: the button keeps one float (the time the
 * pointer entered) and asks for ring i.
 */
public final class GsRipple {
    private GsRipple() {}

    public static final int MAX_RINGS = 3;
    /** Ticks a ring lives, and the delay between rings. */
    public static final float LIFE = 22f, STEP = 4f;
    /** Radius in GUI pixels: starts at R0 and grows by R_PER_TICK each tick. */
    public static final float R0 = 3f, R_PER_TICK = 1.55f;

    /** Age of ring i, or a negative number when it has not started yet. */
    public static float age(float now, float enteredAt, int ring) { return now - enteredAt - ring * STEP; }

    public static boolean alive(float age) { return age >= 0f && age < LIFE; }

    /** Ring radius for an age. */
    public static float radius(float age) { return R0 + Math.max(0f, age) * R_PER_TICK; }

    /** Ring opacity 0..1 for an age (full at the start, fading to nothing). */
    public static float alpha(float age) {
        if (!alive(age)) return 0f;
        float u = 1f - age / LIFE;
        return u * u;
    }

    /** How many rings are alive at all for an entry time (cheap early out: nothing to draw once the last ring ended). */
    public static boolean anyAlive(float now, float enteredAt) {
        return now - enteredAt < LIFE + (MAX_RINGS - 1) * STEP && now >= enteredAt;
    }
}
