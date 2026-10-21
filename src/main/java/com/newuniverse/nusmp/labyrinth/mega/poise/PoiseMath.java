package com.newuniverse.nusmp.labyrinth.mega.poise;

/**
 * Why: the numbers behind a poise bar must be the same in a unit test and in the game, so they are small pure functions. A bearer (an elite or a squad captain) has a CAPACITY that
 * grows with its health and its tier; a blow drains it by the damage it deals (less for an arrow: a skirmisher's bow must not break a captain); super armor takes a share off the
 * blow it holds out. Capacity is derived from health but the damage a blow does is never a share of health (the owner's rule: no percentage-of-max-health damage).
 */
public final class PoiseMath {
    private PoiseMath() {}

    /** The capacity of a bar: {@code health * share + perTier * tier}, times the captain bonus, clamped to the tuning's range. A non-finite input gives the minimum. */
    public static double maxPoise(double maxHealth, int tier, boolean captain, PoiseTuning t) {
        double h = Double.isNaN(maxHealth) || Double.isInfinite(maxHealth) ? 0.0 : Math.max(0.0, maxHealth);
        double v = h * t.healthShare() + t.perTier() * Math.max(1, tier);
        if (captain) v *= t.captainMult();
        return Math.max(t.minMax(), Math.min(t.maxMax(), v));
    }

    /** What a blow of this damage takes off the bar: at least 1 (a hit always counts), a projectile less. */
    public static double poiseDamage(double damage, boolean projectile, PoiseTuning t) {
        double d = Double.isNaN(damage) ? 0.0 : Math.max(0.0, damage);
        double v = projectile ? d * t.projectileMult() : d;
        return Math.max(1.0, v);
    }

    /** The damage that gets through super armor (the whole blow when the armor is down). */
    public static double damageThrough(double amount, boolean armored, PoiseTuning t) {
        double a = Double.isNaN(amount) ? 0.0 : Math.max(0.0, amount);
        return armored ? a * (1.0 - t.armorReduction()) : a;
    }

    /** How much of the bar comes back in {@code ticks} ticks of regeneration. */
    public static double regen(double max, int ticks, PoiseTuning t) {
        return Math.max(0.0, max) * t.regenFrac() * Math.max(0, ticks) / 20.0;
    }

    /** The wind-up (ticks) a telegraphed attack really gets: never under its own minimum, then shortened by desperation. */
    public static int wind(int abilityWind, int telegraphMin, boolean desperate, PoiseTuning t) {
        return Desperation.wind(Math.max(abilityWind, telegraphMin), desperate, t);
    }
}
