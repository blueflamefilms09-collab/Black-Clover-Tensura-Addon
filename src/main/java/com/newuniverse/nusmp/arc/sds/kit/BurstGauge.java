package com.newuniverse.nusmp.arc.sds.kit;

/**
 * Why: "break it with a burst" is the common counter of the Sins' concentration moves (Merlin's Infinity fields, Gowther's nightmare, Diane's dance, King's
 * pollen). The rule is always the same: a share of the boss's maximum health dealt inside a short window triggers it, anti-magic counts double, and it
 * cannot trigger again for a while (otherwise one strong party could keep the boss staggered for the whole fight). This pure gauge holds the window and the
 * cool-down and says "now" exactly once per burst. The threshold is passed in per blow because it is a fraction of a maximum that can change (buffs,
 * config), not a constant.
 */
public final class BurstGauge {
    private final DamageWindow window;
    private final int cooldown;
    private long readyAt = Long.MIN_VALUE;

    /** A gauge that looks back {@code spanTicks} ticks and, once it has triggered, stays quiet for {@code cooldownTicks}. */
    public BurstGauge(int spanTicks, int cooldownTicks) {
        this.window = new DamageWindow(spanTicks);
        this.cooldown = Math.max(0, cooldownTicks);
    }

    /**
     * Records a blow. Returns true when the window now holds at least {@code threshold} (and the cool-down is over): the window is emptied and the
     * cool-down starts. {@code anti} doubles the blow (anti-magic cuts concentration twice as deep).
     */
    public boolean hit(long now, float amount, float threshold, boolean anti) {
        float total = window.add(now, anti ? amount * 2f : amount);
        if (!(threshold > 0f) || total < threshold || now < readyAt) return false;
        window.clear();
        readyAt = now + cooldown;
        return true;
    }

    /** What the window holds at {@code now} (for a progress readout). */
    public float held(long now) { return window.total(now); }

    /** Forgets the window and the cool-down (a fresh fight). */
    public void reset() {
        window.clear();
        readyAt = Long.MIN_VALUE;
    }

    /** Ticks until the gauge can trigger again, 0 if it can. */
    public int cooldownLeft(long now) { return (int) Math.max(0L, readyAt == Long.MIN_VALUE ? 0L : readyAt - now); }
}
