package com.newuniverse.nusmp.dungeon.nexus.relic;

/**
 * The Amulet of the Juggernaut's Blindness halves the wearer's natural mana regeneration. It does it with a modifier on Tensura's magicule
 * regeneration multiplier ({@link AmuletHandler}); this is the fallback for a player who has no such attribute: look at the magicule store once
 * a second and take back half of what it rose by, if the rise is small enough to be natural regeneration (a big jump is a potion, a skill or the
 * Sheath's drink, and is left alone). Pure.
 */
final class RegenMuffle {
    private RegenMuffle() {}

    /** The most a store may rise between two looks, a second apart, and still be taken for natural regeneration: a share of the maximum. */
    static final double NATURAL_RISE = 0.02;
    /** The share of a natural rise the Amulet takes back. */
    static final double SHARE = 0.5;

    /** How much to take off a store that went from {@code before} to {@code after} (maximum {@code max}) since the last look. Never negative. */
    static double takeBack(double before, double after, double max) {
        double rise = after - before;
        if (!(max > 0.0) || !(rise > 0.0) || rise > max * NATURAL_RISE) return 0.0;
        return rise * SHARE;
    }
}
