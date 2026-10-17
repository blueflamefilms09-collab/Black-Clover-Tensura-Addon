package com.newuniverse.nusmp.dungeon.wild;

/**
 * Why: the one rule that turns the entity-type tags of a creature into its tier (0 = not a monster of the dungeon, 1 to 3 = how strong), as
 * plain booleans so it can be tested without the game. {@link Wildlife} reads the tags ({@code nusmp:dungeon/tier1}, {@code tier2}, {@code tier3},
 * {@code legendary}, {@code excluded}) and asks this; the economy asks {@code Wildlife.tierOf} to grade the Magic Stone a kill leaves. It used to be
 * {@code Roster.tierFor}, one method of the Living Dungeon's roster class; the roster went with the Living Dungeon (1.58.0), the rule stayed. Pure.
 */
public final class TierRule {
    private TierRule() {}

    /** The tier a creature has from its tags: 0 if it is vetoed or in none, legendary counts as 3, the highest listed tier wins. */
    public static int tierFor(boolean tier1, boolean tier2, boolean tier3, boolean legendary, boolean excluded) {
        if (excluded) return 0;
        if (legendary || tier3) return 3;
        if (tier2) return 2;
        return tier1 ? 1 : 0;
    }
}
