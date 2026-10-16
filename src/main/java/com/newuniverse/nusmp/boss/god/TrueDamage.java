package com.newuniverse.nusmp.boss.god;

/**
 * Why: against players with enormous armour, Existence Points and regeneration, a spell that deals "base damage" is either nothing or a
 * one-shot depending on the build. The owner wants his spells to deal a <i>share of the victim's maximum health</i> as true damage
 * (armour does not matter), growing with how heavy the move is: 1.5% for a chip, up to 6% for a finisher (1.45.0 had 3% to 12%, halved in 1.46.0 to make the fight fair). This pure class is the table
 * (tier of a move, the percent, the guaranteed-critical factor, the cap of one hit at 25% of a player's maximum so nothing here ever
 * one-shots a full-health player) and the arithmetic; the shared pressure cap of the final arc (18% a second in the god tier) still applies on top, where
 * the damage arrives. The same percent, scaled by a factor, is how much of the victim's magicule pool a hit drains (the Tensura "EP"
 * pool that is cheap to reach).
 */
public final class TrueDamage {
    private TrueDamage() {}

    /** The tiers of a move, lightest to heaviest, with the percent of the victim's maximum health each takes. */
    public enum Tier {
        CHIP(1.5), LIGHT(2.25), MEDIUM(3.25), HEAVY(4.5), FINISHER(6.0);

        public final double percent;

        Tier(double percent) { this.percent = percent; }
    }

    /** One hit never takes more than this share of the victim's maximum health, whatever the tier, the scale and the critical. */
    public static final double SINGLE_HIT_CAP = 0.25;
    /** The factor of a guaranteed critical (the next hit after a Time Stasis Trap). */
    public static final float CRIT_FACTOR = 1.5f;

    /**
     * The tier of a move from the base damage the old code gave it (the numbers of ArcSteps and the move table: a bolt 5, the pin 9,
     * the spatial strike 12, a light step 20, the Soul Touch 26). Bands: up to 6.5 chip, 11 light, 16 medium, 22 heavy, above that finisher.
     */
    public static Tier tierOfBase(float base) {
        if (!(base > 0f) || base <= 6.5f) return Tier.CHIP;
        if (base <= 11f) return Tier.LIGHT;
        if (base <= 16f) return Tier.MEDIUM;
        if (base <= 22f) return Tier.HEAVY;
        return Tier.FINISHER;
    }

    /**
     * The true damage of a hit: the tier's percent of the victim's maximum health, times 'scale' (the config), times the critical factor
     * when 'crit', never more than {@link #SINGLE_HIT_CAP} of the maximum. 0 for a victim without maximum health or a scale of 0.
     */
    public static float amount(Tier tier, float victimMax, double scale, boolean crit) {
        if (!(victimMax > 0f) || !(scale > 0.0) || tier == null) return 0f;
        double raw = victimMax * tier.percent / 100.0 * scale * (crit ? CRIT_FACTOR : 1.0);
        return (float) Math.min(raw, victimMax * SINGLE_HIT_CAP);
    }

    /** The share of a maximum pool (magicule) a hit of this tier drains, as a fraction 0..1 (the percent times 'factor'; at most the single-hit cap). */
    public static double poolFraction(Tier tier, double factor) {
        if (tier == null || !(factor > 0.0)) return 0.0;
        return Math.min(SINGLE_HIT_CAP, tier.percent / 100.0 * factor);
    }
}
