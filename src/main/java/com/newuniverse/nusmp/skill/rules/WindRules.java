package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Airiel (Ais Wallenstein's wind enchantment) is a timed buff plus a dash, and the buff is what makes the dash and projectile defence
 * better. Durations, amplifiers, dash power and the Tempest's shield against arrows are pure numbers kept here and tested.
 */
public final class WindRules {
    private WindRules() {}

    public static final int TEMPEST = 0, DASH = 1;

    public static int tempestTicks(Tier tier) { return tier.pick(600, 1200, 1200); }
    public static int speedAmp(Tier tier) { return tier.pick(1, 2, 2); }
    public static int strengthAmp(Tier tier) { return tier.pick(0, 1, 1); }

    /** Share of arrow and thrown-weapon damage the Tempest's wind turns aside. */
    public static double projectileShield(Tier tier) { return tier.pick(0.5, 0.65, 0.65); }

    public static double dashPower(Tier tier) { return tier.pick(1.8, 2.6, 2.6); }

    /** Dash magicule cost: a third of the Tempest's. */
    public static double dashCost(double tempestCost) { return tempestCost / 3.0; }

    /** Dash cooldown in ticks; wrapped in the Tempest it recovers twice as fast. */
    public static int dashCooldown(boolean tempestActive) { return tempestActive ? 20 : 40; }

    public static boolean tempestActive(long now, long until) { return now < until; }
}
