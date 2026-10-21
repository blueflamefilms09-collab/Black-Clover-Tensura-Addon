package com.newuniverse.nusmp.dungeon.placed;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The truth tables of the player-placed exemption. */
class PlacedRulesTest {
    @Test
    void theExemptionIsActiveOnlyInTheLabyrinthWithTheSwitchOn() {
        assertTrue(PlacedRules.active(true, true));
        assertFalse(PlacedRules.active(false, true), "switched off");
        assertFalse(PlacedRules.active(true, false), "the arenas and the Overworld are never exempt");
        assertFalse(PlacedRules.active(false, false));
    }

    @Test
    void onlyAPlayersPlacementIsRemembered() {
        assertTrue(PlacedRules.remembers(true, true));
        assertFalse(PlacedRules.remembers(true, false), "a mob, a dispenser or a spell");
        assertFalse(PlacedRules.remembers(false, true), "exemption off");
    }

    @Test
    void aRemovalIsLetThroughOnlyForAMarkedBlockWhileActive() {
        assertTrue(PlacedRules.letsRemoval(true, true));
        assertFalse(PlacedRules.letsRemoval(true, false), "a generated block stays held");
        assertFalse(PlacedRules.letsRemoval(false, true));
    }

    @Test
    void aBuildersWriteDropsTheMarkOnlyInsideAnInternalSpan() {
        assertTrue(PlacedRules.forgetOnBuild(true, true, 1));
        assertTrue(PlacedRules.forgetOnBuild(true, true, 3));
        assertFalse(PlacedRules.forgetOnBuild(true, true, 0), "an ordinary write is not the builder");
        assertFalse(PlacedRules.forgetOnBuild(true, false, 1), "nothing to drop");
        assertFalse(PlacedRules.forgetOnBuild(false, true, 1));
    }

    @Test
    void theClientStartsACrackOnlyForAMirroredMark() {
        assertTrue(PlacedRules.clientMayCrack(true, true));
        assertFalse(PlacedRules.clientMayCrack(true, false));
        assertFalse(PlacedRules.clientMayCrack(false, true));
    }

    @Test
    void thePruneNoticeIsRateLimited() {
        assertTrue(PlacedRules.pruneNoticeDue(1000L, 0L, 200L), "never told before");
        assertFalse(PlacedRules.pruneNoticeDue(1100L, 1000L, 200L));
        assertTrue(PlacedRules.pruneNoticeDue(1200L, 1000L, 200L));
        assertTrue(PlacedRules.pruneNoticeDue(50L, 1000L, 200L), "a clock that went backwards (another world) is due");
    }
}
