package com.newuniverse.nusmp.grimoire;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.blackclover.GrimoireCover;

/**
 * Leaf power (docs/grimoire_leaf_power.md): how much stronger a cover makes every spell of its grimoire, on top of the small
 * {@link GrimoireCover#damage}. The ladder follows the anime: a three-leaf (and every other tier-3 cover) is the baseline, a four-leaf
 * is far stronger, a five-leaf far stronger again, and the two legendary covers stand above them.
 * Pure: the numbers come in as arguments, the setting is read by {@link #factor(GrimoireCover)} only. Server side use only (the hit is
 * decided on the server); the tooltip reads the same function.
 */
public final class GrimoireLeafPower {
    private GrimoireLeafPower() {}

    public static final double DEFAULT_FOUR = 2.5, DEFAULT_FIVE = 6.0, DEFAULT_BLACK = 10.0, DEFAULT_GOD = 14.0;

    /** The multiplier of a cover; 1 when off, for tier-3 covers and for a null cover. NaN or non-positive numbers read as 1. */
    public static double factor(GrimoireCover c, boolean enabled, double four, double five, double black, double god) {
        if (!enabled || c == null) return 1.0;
        double f;
        if (c == GrimoireCover.GOD_TIER) f = god;
        else if (c == GrimoireCover.BLACK_MAGIC) f = black;
        else if (c.tier >= 5) f = five;
        else if (c.tier == 4) f = four;
        else f = 1.0;
        return Double.isFinite(f) && f >= 1.0 ? f : 1.0;
    }

    /** The multiplier read from the SERVER settings (the defaults while the config is not loaded yet). */
    public static double factor(GrimoireCover c) {
        try {
            return factor(c, NUConfig.LEAF_POWER.get(), NUConfig.LEAF_FOUR.get(), NUConfig.LEAF_FIVE.get(), NUConfig.LEAF_BLACK.get(), NUConfig.LEAF_GOD.get());
        } catch (IllegalStateException notLoaded) {
            return factor(c, true, DEFAULT_FOUR, DEFAULT_FIVE, DEFAULT_BLACK, DEFAULT_GOD);
        }
    }
}
