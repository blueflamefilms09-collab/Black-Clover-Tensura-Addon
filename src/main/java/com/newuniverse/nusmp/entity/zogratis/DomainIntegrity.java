package com.newuniverse.nusmp.entity.zogratis;

/**
 * How much punishment Spatial Mana Domination takes before it shatters (pure, unit tested). Every point of damage that lands on
 * Zenon while the cube stands is also damage to the cube; anti-magic counts double, because cutting spatial magic is what
 * anti-magic is for. Once the total reaches the cube's integrity the domain breaks at once: the counter to the ultimate is to hurt
 * the man who cast it, from inside or outside the walls.
 */
public final class DomainIntegrity {
    /** Anti-magic (a demon sword, the Anti-Magic Lord) cuts the cube this many times as deep as an ordinary blow. */
    public static final double ANTI_MAGIC_FACTOR = 2.0;
    public static final double MIN_FRACTION = 0.08, MAX_FRACTION = 0.14;

    private final double max;
    private double taken;

    public DomainIntegrity(double max) { this.max = Math.max(1, max); }

    /** The integrity of a cube cast by a boss of 'bossMaxHealth' at 'power': 8% of its health at 60%, 14% at the Gates. */
    public static double maxFor(double bossMaxHealth, int power) {
        double t = (Math.max(60, Math.min(100, power)) - 60) / 40.0;
        return Math.max(1, bossMaxHealth * (MIN_FRACTION + (MAX_FRACTION - MIN_FRACTION) * t));
    }

    /** Takes a blow of 'amount' (anti-magic counts double). True if this blow broke the cube (only ever once). */
    public boolean hit(double amount, boolean antiMagic) {
        if (broken() || !(amount > 0)) return false;
        taken += amount * (antiMagic ? ANTI_MAGIC_FACTOR : 1.0);
        return broken();
    }

    public boolean broken() { return taken >= max; }
    public double max() { return max; }
    public double taken() { return Math.min(max, taken); }

    /** 1 = untouched, 0 = shattered. */
    public double fraction() { return Math.max(0, 1 - taken / max); }
}
