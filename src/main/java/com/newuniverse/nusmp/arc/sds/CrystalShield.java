package com.newuniverse.nusmp.arc.sds;

/**
 * Why: the Diamond Shield is the barrier Diane raises out of the ground (a mask or a wall of crystal, the wiki sources differ; see docs/arc/diane.md), and a barrier is the
 * simplest rule in a fight and the oldest answer: it takes a fixed amount of damage and then it breaks. Nothing in a boss fight may heal the boss for nothing, so this is
 * not health: it is a pool that lasts eight seconds at most, that the party can see being worn down, that is gone when it is gone (nothing refills it but casting the move
 * again after its long cool-down), and whose breaking leaves her open. This pure object keeps the pool, the clock and the arithmetic of a blow against it.
 */
public final class CrystalShield {
    /** What one blow did to the shield. */
    public record Verdict(float passed, float absorbed, boolean broke) {}

    private long openUntil = Long.MIN_VALUE;
    private float pool;

    public void open(long now, int ticks, float capacity) {
        openUntil = now + Math.max(1, ticks);
        pool = Math.max(0f, capacity);
    }

    public boolean isOpen(long now) { return now < openUntil && pool > 0f; }

    public float pool(long now) { return isOpen(now) ? pool : 0f; }

    public void close() {
        openUntil = Long.MIN_VALUE;
        pool = 0f;
    }

    /** A blow of {@code amount}: the shield takes what it can; what is left passes. The shield that is used up is closed and says it broke. */
    public Verdict absorb(long now, float amount) {
        if (!isOpen(now) || !(amount > 0f)) return new Verdict(Math.max(0f, amount), 0f, false);
        float taken = Math.min(pool, amount);
        pool -= taken;
        boolean broke = pool <= 0f;
        if (broke) close();
        return new Verdict(amount - taken, taken, broke);
    }
}
