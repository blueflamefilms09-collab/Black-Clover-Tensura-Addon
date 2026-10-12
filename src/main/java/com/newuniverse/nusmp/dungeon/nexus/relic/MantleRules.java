package com.newuniverse.nusmp.dungeon.nexus.relic;

/**
 * What the Mantle of the Scavenger does to the Dungeon's food web (docs/living_dungeon.md section 7), by roster tier (the tag roster of
 * dungeon.wild.Wildlife: 1 scavengers, 2 prey and mid predators, 3 apex predators). Pure.
 */
public final class MantleRules {
    private MantleRules() {}

    /** Apex predators (tier 3) pay the wearer no mind. */
    public static final int IGNORED_FROM = 3;
    /** The wearer's physical blows do nothing to tier 2 and 3 creatures (Prey's Weakness). */
    public static final int NO_DAMAGE_FROM = 2;

    /** Does a creature of this tier ignore the wearer? Not while the Pheromone Mark is on. */
    public static boolean ignores(int tier, boolean marked) { return !marked && tier >= IGNORED_FROM; }

    /** Is a blow of this kind turned aside by the curse? */
    public static boolean turnsAside(boolean physical, int tier) { return physical && tier >= NO_DAMAGE_FROM; }
}
