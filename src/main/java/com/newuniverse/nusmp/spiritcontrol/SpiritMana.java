package com.newuniverse.nusmp.spiritcontrol;

/**
 * Why: a spirit's own small mana pool for combat mode, as a pure object (no Minecraft types, no per-tick work: it refills lazily from the game time when asked). The maximum and the regeneration grow with the
 * bond (trust 0..100): the config gives a base and an amount per trust point. One object per player and spirit, created the first time combat mode asks for it and dropped at logout.
 */
public final class SpiritMana {
    private double max, regenPerSecond, current;
    private long last;

    public SpiritMana(double max, double regenPerSecond, long now) {
        this.max = Math.max(1.0, max);
        this.regenPerSecond = Math.max(0.0, regenPerSecond);
        this.current = this.max;
        this.last = now;
    }

    /** The maximum for a bond: {@code base + perTrust * trust} (trust held to 0..100). */
    public static double maxFor(double base, double perTrust, int trust) { return Math.max(1.0, base + perTrust * clampTrust(trust)); }

    /** The regeneration per second for a bond. */
    public static double regenFor(double base, double perTrust, int trust) { return Math.max(0.0, base + perTrust * clampTrust(trust)); }

    public static int clampTrust(int trust) { return Math.max(0, Math.min(100, trust)); }

    /** The bond changed (or the config did): new limits; the pool keeps what it holds, cut to the new maximum. */
    public void configure(double newMax, double newRegen) {
        this.max = Math.max(1.0, newMax);
        this.regenPerSecond = Math.max(0.0, newRegen);
        if (current > max) current = max;
    }

    /** Refills for the time that passed since the last call (a clock that went backwards only moves the stamp). */
    public void advance(long now) {
        if (now > last) current = Math.min(max, current + regenPerSecond * (now - last) / 20.0);
        last = now;
    }

    public double current() { return current; }

    public double max() { return max; }

    public double regenPerSecond() { return regenPerSecond; }

    /** Filled fraction 0..1. */
    public double fill() { return max <= 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, current / max)); }

    public boolean canSpend(double amount) { return amount <= 0.0 || current + 1e-9 >= amount; }

    /** Takes {@code amount}; false (and nothing taken) when the pool holds less. */
    public boolean spend(double amount) {
        if (amount <= 0.0) return true;
        if (!canSpend(amount)) return false;
        current = Math.max(0.0, current - amount);
        return true;
    }

    public void refill() { current = max; }
}
