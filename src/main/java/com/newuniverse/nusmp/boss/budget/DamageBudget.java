package com.newuniverse.nusmp.boss.budget;

/**
 * The per-second damage budget of ONE boss (pure, no Minecraft types, unit tested). One instance per entity, never shared: it holds
 * the health the boss may still lose in the current 20-tick window and its own tick counter.
 * <ul>
 *   <li>{@link #allow}: the only gate. Returns the amount that may go through: all of it while the budget lasts, the rest of the budget
 *       when the amount is larger (truncation, so the budget is spent exactly), 0 when the budget is exhausted.</li>
 *   <li>{@link #tick}: counts ticks; every {@link #WINDOW} ticks the budget is full again.</li>
 * </ul>
 */
public final class DamageBudget {
    /** One second. */
    public static final int WINDOW = 20;

    private float cap;
    private float left;
    private int ticks;

    public DamageBudget(float cap) {
        this.cap = sane(cap);
        this.left = this.cap;
    }

    private static float sane(float c) { return Float.isNaN(c) || c < 0f ? 0f : c; }

    /** The health that may be taken from the boss now: min(amount, what is left), 0 for a non-positive or NaN amount or an empty budget. */
    public float allow(float amount) {
        if (!(amount > 0f)) return 0f;                       // also false for NaN
        float got = amount >= left ? left : amount;
        left -= got;
        return got;
    }

    /** One tick; on the 20th the budget is restored. Returns true on the tick that reset it. */
    public boolean tick() {
        if (++ticks >= WINDOW) {
            reset();
            return true;
        }
        return false;
    }

    /** Full budget and a fresh window. */
    public void reset() {
        ticks = 0;
        left = cap;
    }

    /**
     * Changes the cap (the boss's phase or max health changed). A budget already spent stays spent: only the headroom moves, so a phase
     * change cannot be used to refill the window. The left amount never exceeds the new cap.
     */
    public void setCap(float newCap) {
        float c = sane(newCap);
        float spent = Math.max(0f, cap - left);
        cap = c;
        left = Math.max(0f, Math.min(c, c - spent));
    }

    public float cap() { return cap; }

    public float left() { return left; }

    public int ticksInWindow() { return ticks; }
}
