package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Firebolt (Bell Cranel's chantless lightning-fire magic) is one instant shot; its damage, splash and reach grow with mastery. The
 * numbers live here, fed by the config damage, so "mastered doubles the damage" stays true and testable.
 */
public final class FireboltRules {
    private FireboltRules() {}

    /** Damage of the bolt: the config value, doubled when mastered. */
    public static double damage(double configDamage, Tier tier) { return Math.max(0, configDamage) * (tier == Tier.BASE ? 1.0 : 2.0); }

    /** Blast radius around the point of impact (blocks). */
    public static double splashRadius(Tier tier) { return tier.pick(2.0, 3.0, 3.0); }

    /** Share of the bolt's damage the splash does to creatures near the target. */
    public static double splashShare() { return 0.5; }

    public static double range(Tier tier) { return tier.pick(32.0, 48.0, 48.0); }

    public static int igniteSeconds(Tier tier) { return tier.pick(4, 6, 6); }

    /** Mastery earned per cast: only a shot that hit something teaches. */
    public static int masteryGain(boolean hit) { return hit ? 1 : 0; }

    /** A Firebolt carrying an Argonaut charge (wiki: "an Argonaut charged Firebolt"): damage grows by 0.8 of the base per bell (5 bells = 5x). 0 bells = the plain bolt. */
    public static double chargedFactor(int charge) { return 1.0 + 0.8 * Math.max(0, charge); }

    /** Extra blast radius of a charged Firebolt (blocks), capped at 3 (the target count is still capped by SkillTargets). */
    public static double chargedSplashBonus(int charge) { return Math.min(3.0, 0.3 * Math.max(0, charge)); }
}
