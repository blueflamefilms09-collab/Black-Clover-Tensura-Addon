package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Unbreakable Will lets you survive one killing blow per long cooldown; Immortal Will, its Ultimate, rises at full health, much more
 * often, and answers with a shockwave and a second mode. Whether a death may be refused, what health you return with and how long you must
 * wait are life-or-death rules, so they live here and are tested.
 */
public final class WillRules {
    private WillRules() {}

    public static final int READY = 0, RALLY = 1;

    /** Share of maximum health you stand up with. */
    public static double restoredShare(Tier t) { return t.pick(0.3, 0.5, 1.0); }

    /** Cooldown before the will can refuse death again (ticks, before the pack multiplier). */
    public static int cooldownTicks(Tier t) { return t.pick(12000, 6000, 3600); }

    /** Ticks of damage resistance after rising. */
    public static int shieldTicks(Tier t) { return t == Tier.ULTIMATE ? 200 : 100; }
    public static int shieldAmp(Tier t) { return t == Tier.ULTIMATE ? 4 : 3; }

    /** Whether the will may refuse this death. */
    public static boolean mayRefuse(boolean onCooldown, boolean bypassesInvulnerability) { return !onCooldown && !bypassesInvulnerability; }

    /** The Immortal Will's shockwave (Ultimate only). */
    public static double shockRadius(Tier t) { return t == Tier.ULTIMATE ? 8.0 : 0.0; }
    public static double shockDamage() { return 18.0; }
    public static double shockKnockback() { return 1.6; }

    /** Rally (Immortal Will mode 1): heal a share of max health and cleanse. */
    public static double rallyShare() { return 0.4; }
    public static double rallyCost() { return 1200; }
    public static int rallyCooldown() { return 1800; }

    public static int modes(Tier t) { return t == Tier.ULTIMATE ? 2 : 1; }
}
