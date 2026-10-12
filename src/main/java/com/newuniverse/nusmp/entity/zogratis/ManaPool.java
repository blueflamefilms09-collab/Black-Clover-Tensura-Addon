package com.newuniverse.nusmp.entity.zogratis;

/**
 * A boss's mana (pure). Below 60% power the pool is finite, so a boss that throws its big moves back to back runs dry and has to
 * wait for the pool to refill, which is the player's window. From 60% on the mana is unbound: nothing costs anything.
 */
public final class ManaPool {
    private double cap = 100, regenPerTick = 0.2, current = 100;
    private boolean infinite;

    public ManaPool(PowerCurve.Tuning tuning) {
        retune(tuning);
        current = cap;
    }

    /** A new power: the pool takes the new size and fills up (drawing out more of the devil's power refills the boss). */
    public void retune(PowerCurve.Tuning t) {
        cap = Math.max(1, t.manaCap());
        regenPerTick = Math.max(0, t.manaRegenPerSecond()) / 20.0;
        infinite = t.infiniteMana();
        current = cap;
    }

    /** Mana a move may spend now (infinite once the mana is unbound). */
    public double available() { return infinite ? Double.POSITIVE_INFINITY : current; }

    public boolean canAfford(double cost) { return infinite || current >= cost; }

    /** Takes 'cost' if it can be paid; the unbound pool pays anything for free. */
    public boolean spend(double cost) {
        if (infinite) return true;
        if (current < cost) return false;
        current -= Math.max(0, cost);
        return true;
    }

    public void tick() {
        if (!infinite && current < cap) current = Math.min(cap, current + regenPerTick);
    }

    public boolean infinite() { return infinite; }
    public double cap() { return cap; }
    public double current() { return infinite ? cap : current; }
    public double fraction() { return infinite ? 1.0 : current / cap; }

    /** Restores a saved level (clamped to the pool). */
    public void set(double value) { current = Math.max(0, Math.min(cap, value)); }
}
