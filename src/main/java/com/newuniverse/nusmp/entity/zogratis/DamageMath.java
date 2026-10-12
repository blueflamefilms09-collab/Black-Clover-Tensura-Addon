package com.newuniverse.nusmp.entity.zogratis;

/**
 * The arithmetic behind {@code ZogratisDamage} (pure). A Zogratis hit starts from the move's base damage, is multiplied by the
 * boss's power tuning, and then by how strong the victim's existence is (their EP): a fresh player takes the base, a player in the
 * millions of EP takes up to three times that. The balance law then adds its sliver of the victim's max health and caps one hit at
 * a share of a player's max health, so a strong player feels it and nobody is one-shot.
 */
public final class DamageMath {
    private DamageMath() {}

    public static final double MAX_VICTIM_FACTOR = 3.0;

    /** x1 up to 1,000 EP, then +0.5 per decade of EP, at most x3 (10 million EP). */
    public static double victimFactor(double ep) {
        double decades = Math.log10(Math.max(1, ep)) - 3;
        return Math.min(MAX_VICTIM_FACTOR, 1 + 0.5 * Math.max(0, decades));
    }

    /** Base damage x power damage multiplier x a per-boss scale x the victim's EP factor. */
    public static double raw(double base, double powerMultiplier, double bossScale, double victimEp) {
        return Math.max(0, base) * powerMultiplier * bossScale * victimFactor(victimEp);
    }
}
