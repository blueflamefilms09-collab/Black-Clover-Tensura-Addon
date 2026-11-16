package com.newuniverse.nusmp.balance;

/**
 * Why: the owner asked that the mod's damage stays good in the late game by growing with the player. {@link BalanceLaw#damage(net.minecraft.world.entity.LivingEntity,
 * net.minecraft.world.entity.LivingEntity, float, double)} multiplies every hit by this factor, read from the caster's max magicule: x1 at or below the baseline (10,000 by default), +1.25 per tenfold
 * above it (x2.25 at 100k, x3.5 at 1M, x4.75 at 10M) and never above the cap (x6 by default, reached at 100M). Pure, so it is tested without Minecraft; NaN, negative and zero inputs give x1.
 */
public final class LateGameScaling {
    private LateGameScaling() {}

    public static final double DEFAULT_BASELINE = 10_000.0;
    public static final double DEFAULT_CAP = 6.0;
    /** How much the factor grows per tenfold of max magicule above the baseline. */
    public static final double PER_DECADE = 1.25;

    /** The factor for a caster with {@code maxMagicule}; 1 when off, at or below the baseline, or for any bad input. */
    public static double factor(boolean enabled, double maxMagicule, double baseline, double cap) {
        if (!enabled || !(cap > 1.0) || !(baseline > 0.0) || !(maxMagicule > baseline)) return 1.0;
        double f = 1.0 + PER_DECADE * Math.log10(maxMagicule / baseline);
        return Double.isFinite(f) ? Math.min(cap, f) : cap;
    }
}
