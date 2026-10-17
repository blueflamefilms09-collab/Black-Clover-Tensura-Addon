package com.newuniverse.nusmp.labyrinth.mega.gen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Why: the Landing rule decides where every player lands when they walk through a Gate, and its corners (nothing playable, a hole in the ids, a player who
 * remembers a deeper Landing than exists, sneaking, the switch off) are exactly the ones a plain read of the code gets wrong, so each is pinned here.
 */
class LandingRuleTest {
    @Test
    void nothingPlayableMeansNoAnswer() {
        assertEquals(-1, LandingRule.choose(true, false, 5, new int[0]));
        assertEquals(-1, LandingRule.choose(true, false, 0, null));
    }

    @Test
    void aNewcomerStartsAtTheFirstSector() {
        assertEquals(0, LandingRule.choose(true, false, 0, new int[] {0, 1, 2}));
        assertEquals(0, LandingRule.choose(true, false, -3, new int[] {0, 1, 2}));
    }

    @Test
    void thePlayerIsTakenToTheDeepestLandingTheyReached() {
        assertEquals(2, LandingRule.choose(true, false, 2, new int[] {0, 1, 2, 3}));
        assertEquals(3, LandingRule.choose(true, false, 3, new int[] {3, 2, 1, 0}));
    }

    @Test
    void aRememberedLandingBeyondTheNewestIsClampedToTheNewest() {
        assertEquals(3, LandingRule.choose(true, false, 40, new int[] {0, 1, 2, 3}));
    }

    @Test
    void aSectorThatIsStillBeingBuiltIsSkippedForTheNextLowerOne() {
        // sector 2 is missing from the playable list (generating or failed): the player who reached 2 lands in 1
        assertEquals(1, LandingRule.choose(true, false, 2, new int[] {0, 1, 3}));
        assertEquals(0, LandingRule.choose(true, false, 2, new int[] {0, 3}));
    }

    @Test
    void ifEveryPlayableSectorIsDeeperThanTheMemoryTheFirstPlayableOneIsUsed() {
        assertEquals(4, LandingRule.choose(true, false, 1, new int[] {4, 5}));
    }

    @Test
    void sneakingAlwaysMeansTheStart() {
        assertEquals(0, LandingRule.choose(true, true, 3, new int[] {0, 1, 2, 3}));
        assertEquals(2, LandingRule.choose(true, true, 3, new int[] {2, 3}));
    }

    @Test
    void withTheSwitchOffEveryEntryIsTheStart() {
        assertEquals(0, LandingRule.choose(false, false, 3, new int[] {0, 1, 2, 3}));
    }

    @Test
    void theInputArrayIsNotChanged() {
        int[] ids = {3, 1, 2};
        LandingRule.choose(true, false, 2, ids);
        assertEquals(3, ids[0]);
        assertEquals(1, ids[1]);
        assertEquals(2, ids[2]);
    }
}
