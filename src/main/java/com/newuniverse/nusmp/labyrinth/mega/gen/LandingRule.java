package com.newuniverse.nusmp.labyrinth.mega.gen;

import java.util.Arrays;

/**
 * Why: "which Landing does a player arrive at" is a rule with several corners (a sector that is still being built is not a place yet, a player may have been
 * to a Landing that an admin has since regenerated, sneaking means "take me back to the start", the config can switch the whole idea off), and a rule with
 * corners belongs where it can be tested without a game. {@code MegaTravel} reads the saved sectors and the player's remembered depth, hands this class two
 * plain numbers and a list, and teleports to whatever sector id comes back.
 */
public final class LandingRule {
    private LandingRule() {}

    /**
     * The id of the sector whose Landing to enter, or -1 when no sector can be entered yet.
     *
     * @param lastLanding the {@code lastLanding} config switch: false always means the first playable sector
     * @param sneaking    the player is sneaking: the explicit "start again from the beginning"
     * @param deepest     the deepest Landing the player ever stood in (their remembered sector id; 0 or less for a newcomer)
     * @param playable    the ids of the sectors a player can stand in (ACTIVE or later), in any order
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
