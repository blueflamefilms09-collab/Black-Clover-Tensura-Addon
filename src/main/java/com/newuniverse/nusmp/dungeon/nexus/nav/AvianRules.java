package com.newuniverse.nusmp.dungeon.nexus.nav;

/**
 * The Anti-Bird Compass is a rare drop of the avian scavengers of the Upper Catacombs (docs/living_dungeon.md section 3): a 4% chance
 * when a player kills one, 1.5% more for each level of Looting on the weapon, never above 25%. Pure.
 */
public final class AvianRules {
    private AvianRules() {}

    public static final double BASE = 0.04, PER_LOOTING = 0.015, CEILING = 0.25;

    /** The chance of the drop for a killer whose weapon has this Looting level (a negative level counts as none). */
    public static double chance(int looting) { return Math.min(CEILING, BASE + PER_LOOTING * Math.max(0, looting)); }

    /** Does this roll (0 up to but not including 1) win the drop? */
    public static boolean drops(int looting, double roll) { return roll < chance(looting); }
}
