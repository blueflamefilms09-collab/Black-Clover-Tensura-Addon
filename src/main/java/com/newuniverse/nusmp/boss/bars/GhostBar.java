package com.newuniverse.nusmp.boss.bars;

/**
 * Why: the "damage lag" bar: when the boss is hit the fill drops at once but a pale chip stays where the health was and then drains more slowly, so a player sees how big the hit was. Pure
 * easing, driven by the frame time in ticks: the ghost follows the health instantly UP (a heal never leaves a chip), holds for {@link #HOLD_TICKS} after the last hit, then eases down at a
 * share of the gap per tick (never slower than {@link #MIN_RATE} per tick, so the last sliver also goes) and never passes below the health.
 */
public final class GhostBar {
    public static final double HOLD_TICKS = 12.0;
    public static final float FRACTION = 0.07f, MIN_RATE = 0.004f;

    private float ghost = 1f, hp = 1f;
    private double holdUntil = Double.NEGATIVE_INFINITY, last = Double.NaN;

    /** Starts at a value with no chip (a bar that has just appeared). */
    public void reset(float value) {
        ghost = hp = clamp(value);
        holdUntil = Double.NEGATIVE_INFINITY;
        last = Double.NaN;
    }

    /** The frame at {@code time} (ticks, with the partial tick) with the boss at health {@code health} (0..1). */
    public void update(double time, float health) {
        float h = clamp(health);
        double dt = Double.isNaN(last) ? 0.0 : Math.max(0.0, Math.min(20.0, time - last));
        last = time;
        if (h > hp + 1e-6f && h >= ghost) {                       // healed (or filled): the chip is gone
            ghost = h;
        } else if (h < hp - 1e-6f) {                              // a hit: the chip holds again
            holdUntil = time + HOLD_TICKS;
        }
        hp = h;
        if (ghost < hp) ghost = hp;
        if (ghost > hp && time >= holdUntil) {
            float step = Math.max(MIN_RATE, (ghost - hp) * FRACTION) * (float) dt;
            ghost = Math.max(hp, ghost - step);
        }
    }

    public float ghost() { return ghost; }

    public float health() { return hp; }

    /** The share of the bar the chip covers (ghost - health), 0 when none. */
    public float chip() { return Math.max(0f, ghost - hp); }

    private static float clamp(float v) { return v < 0f ? 0f : Math.min(1f, v); }
}
