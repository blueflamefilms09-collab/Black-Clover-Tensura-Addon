package com.newuniverse.nusmp.labyrinth.mega.gen;

import java.util.Arrays;

/**
 * Why: "which Landing does a player arrive at" is a rule with several corners (a sector that is still being built is not a place yet, a player may have been
 * to a Landing that an admin has since regenerated, sneaking means "take me back to the start", the config can switch the whole idea off), and a rule with
 * corners belongs where it can be tested without a game. {@code MegaTravel} reads the saved sectors and the player's remembered depth, hands this class two
 * plain numbers and a list, and teleports to whatever sector comes back. The numbers are positions in ONE road: since 1.55.0 the dungeon is two roads whose sector
 * ids interleave, so an id says nothing about depth and {@code MegaTravel} gives this rule the CHAIN INDICES of the road the player is entering (0 for its first sector).
 * The rule only needs numbers that grow with depth, so it is the same rule for a 1.54.0 single chain (where the chain index is the id) and for either road.
 */
public final class LandingRule {
    private LandingRule() {}

    /**
     * The chain index of the sector whose Landing to enter, or -1 when no sector can be entered yet.
     *
     * @param lastLanding the {@code lastLanding} config switch: false always means the first playable sector
     * @param sneaking    the player is sneaking: the explicit "start again from the beginning"
     * @param deepest     the deepest Landing the player ever stood in on this road (their remembered chain index; 0 or less for a newcomer)
     * @param playable    the chain indices of the road's sectors a player can stand in (ACTIVE or later), in any order
     */
    public static int choose(boolean lastLanding, boolean sneaking, int deepest, int[] playable) {
        if (playable == null || playable.length == 0) return -1;
        int[] ids = playable.clone();
        Arrays.sort(ids);
        if (!lastLanding || sneaking) return ids[0];
        int best = ids[0];
        for (int id : ids) {
            if (id <= deepest) best = id;
        }
        return best;
    }
}
