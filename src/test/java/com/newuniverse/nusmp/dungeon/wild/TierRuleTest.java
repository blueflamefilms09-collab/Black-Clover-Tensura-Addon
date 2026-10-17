package com.newuniverse.nusmp.dungeon.wild;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Why: the economy grades the Magic Stone of every kill by the tier of the creature, and the tier is decided by five entity-type tags; a wrong
 * order of the veto, the legendary mark and the three tier tags would silently pay the wrong stone. The rule is pure ({@link TierRule}), so every
 * combination of the five tags is checked here without the game.
 */
class TierRuleTest {
    @Test
    void tierComesFromTheTagsAndTheVetoWins() {
        assertEquals(0, TierRule.tierFor(true, true, true, true, true), "excluded beats every list");
        assertEquals(3, TierRule.tierFor(false, false, false, true, false), "legendary counts as 3");
        assertEquals(3, TierRule.tierFor(true, true, true, false, false), "the highest listed tier wins");
        assertEquals(3, TierRule.tierFor(false, false, true, false, false));
        assertEquals(2, TierRule.tierFor(true, true, false, false, false));
        assertEquals(2, TierRule.tierFor(false, true, false, false, false));
        assertEquals(1, TierRule.tierFor(true, false, false, false, false));
        assertEquals(0, TierRule.tierFor(false, false, false, false, false), "in no list = not a monster of the dungeon");
    }

    @Test
    void everyCombinationOfTagsGivesTheHighestOrZero() {
        for (int bits = 0; bits < 32; bits++) {
            boolean t1 = (bits & 1) != 0, t2 = (bits & 2) != 0, t3 = (bits & 4) != 0, legend = (bits & 8) != 0, excluded = (bits & 16) != 0;
            int expected = excluded ? 0 : (legend || t3) ? 3 : t2 ? 2 : t1 ? 1 : 0;
            assertEquals(expected, TierRule.tierFor(t1, t2, t3, legend, excluded), "combination " + bits);
        }
    }
}
