package com.newuniverse.nusmp.grimoire;

import com.newuniverse.nusmp.blackclover.GrimoireCover;

/**
 * Canon leaves, mana and level matter (docs/grimoire_canon_leaves.md). Pure, finite, smooth and monotone. The wielder's level is the mod's existing level notion, the EP: a "band" is one power of ten of
 * EP above 1,000 (1,000 EP = band 0, 10k = 1, 100k = 2, 1M = 3, the same ladder as BalanceLaw.epScale). A cover asks for a band (three-leaf 0, four-leaf 1, five-leaf 2, black 2.5, god 3):
 * under it the spells cost more mana (up to the setting, default x1.5), above it a lower cover gets a small damage bonus (up to the setting, default +12%). Neither can go below x1.
 */
public final class GrimoireMastery {
    private GrimoireMastery() {}

    public static final double DEFAULT_MAX_COST = 1.5, DEFAULT_MAX_BONUS = 0.12;

    /** Bands of EP above 1,000; 0 for anything at or below it, NaN, infinity or a negative number; at most 6. */
    public static double band(double ep) {
        if (!Double.isFinite(ep) || ep <= 1000.0) return 0.0;
        return Math.min(6.0, Math.log10(ep) - 3.0);
    }

    /** The band a cover asks of its wielder. */
    public static double requiredBand(GrimoireCover c) {
        if (c == null) return 0.0;
        double base = Math.max(0, c.tier - 3);
        return c == GrimoireCover.GOD_TIER ? base + 1.0 : c == GrimoireCover.BLACK_MAGIC ? base + 0.5 : base;
    }

    /** The mana cost factor of holding a cover above the wielder's band: 1 when the wielder has the band, rising smoothly towards maxCost (clamped to 1..3) the further under it they are. */
    public static double costFactor(GrimoireCover c, double ep, double maxCost) {
        double m = Double.isFinite(maxCost) ? Math.max(1.0, Math.min(3.0, maxCost)) : 1.0;
        double over = Math.max(0.0, requiredBand(c) - band(ep));
        return 1.0 + (m - 1.0) * (1.0 - Math.exp(-over / 2.0));
    }

    /** The damage factor of a wielder above the band of the cover: 1 at the band, rising smoothly towards 1 + maxBonus (clamped to 0..1) the further above it they are. */
    public static double damageFactor(GrimoireCover c, double ep, double maxBonus) {
        double b = Double.isFinite(maxBonus) ? Math.max(0.0, Math.min(1.0, maxBonus)) : 0.0;
        double surplus = Math.max(0.0, band(ep) - requiredBand(c));
        return 1.0 + b * (1.0 - Math.exp(-surplus / 2.0));
    }
}
