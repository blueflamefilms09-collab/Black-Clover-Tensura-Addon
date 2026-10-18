package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Argonaut (Bell Cranel) is charged by HOLDING the key, one bell chime per second up to five, and released to strike. The Ultimate,
 * Hero's Argonaut, charges ten bells and adds a piercing beam. The charge count, the damage per charge and the beam length are pure
 * functions of the held ticks, so they are unit-tested instead of discovered by playing.
 */
public final class ChargeRules {
    private ChargeRules() {}

    public static final int TICKS_PER_CHARGE = 20;

    /** Append-only strike kinds (mode index): the Unique has only STRIKE; the Ultimate adds BEAM. */
    public enum Strike { STRIKE, BEAM }

    public static int maxCharge(Tier tier) { return tier == Tier.ULTIMATE ? 10 : 5; }

    /** Charges built after {@code heldTicks} of holding (0 until the first full second). */
    public static int chargeAt(int heldTicks, Tier tier) {
        if (heldTicks <= 0) return 0;
        return Math.min(maxCharge(tier), heldTicks / TICKS_PER_CHARGE);
    }

    /** How long the key may be held: the charge time plus two seconds of slack. */
    public static int maxHeldTicks(Tier tier) { return maxCharge(tier) * TICKS_PER_CHARGE + 40; }

    /** Damage multiplier of the tier. The Ultimate is twice a mastered Unique's strike and a fresh Unique's is the baseline. */
    public static double tierFactor(Tier tier) { return tier.pick(1.0, 1.5, 3.0); }

    public static double damage(double perCharge, int charge, Tier tier, boolean ownMastered) {
        return Math.max(0, perCharge) * Math.max(0, charge) * tierFactor(tier) * tier.masteryScale(ownMastered);
    }

    /** Reach of the melee strike (blocks). */
    public static double strikeReach(Tier tier) { return tier.pick(6.0, 6.0, 8.0); }

    /** Cosine of the half-angle of the strike cone. */
    public static double strikeCone(Tier tier) { return tier == Tier.ULTIMATE ? 0.45 : 0.6; }

    public static double knockback(int charge) { return 0.5 + charge * 0.3; }

    /** Length of Hero's Beam (blocks): grows with the charge, capped. */
    public static double beamLength(int charge) { return SkillMath.clamp(12.0 + 3.0 * charge, 12.0, 42.0); }

    public static double beamRadius(int charge) { return 1.0 + charge * 0.08; }

    /** The beam hits a little harder than the same charge's strike because it pierces everything. */
    public static double beamFactor() { return 1.25; }

    /** The mastery earned: the charge, only if the strike hit something. */
    public static int masteryGain(int charge, int hits) { return hits > 0 ? Math.max(1, charge) : 0; }

    /** True when the held key has crossed into a new full charge (a chime should ring). */
    public static boolean chimeNow(int heldTicks, Tier tier) {
        return heldTicks > 0 && heldTicks % TICKS_PER_CHARGE == 0 && heldTicks / TICKS_PER_CHARGE <= maxCharge(tier);
    }

    public static int modes(Tier tier) { return tier == Tier.ULTIMATE ? 2 : 1; }
}
