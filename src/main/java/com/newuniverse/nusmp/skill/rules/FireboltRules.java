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
}
