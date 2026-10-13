package com.newuniverse.nusmp.dungeon.sentient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The fallback blow: once in two seconds, in reach, never right after the mob's own brain, and in water only both wet. */
class StrikeRulesTest {
    @Test
    void aMobInReachStrikesOnceInTwoSeconds() {
        long now = 1000;
        assertTrue(StrikeRules.mayStrike(now, 0, true, false, false, false, false));
        long next = StrikeRules.nextStrike(now);
        assertEquals(now + 40, next);
        assertFalse(StrikeRules.mayStrike(now + 39, next, true, false, false, false, false));
        assertTrue(StrikeRules.mayStrike(now + 40, next, true, false, false, false, false));
    }

    @Test
    void outOfReachOrJustAfterItsOwnBrainStruckItDoesNot() {
        assertFalse(StrikeRules.mayStrike(1000, 0, false, false, false, false, false));
        assertFalse(StrikeRules.mayStrike(1000, 0, true, true, false, false, false));
    }

    @Test
    void aMobOfTheWaterNeedsBothInTheWaterOrToTouch() {
        assertFalse(StrikeRules.mayStrike(1000, 0, true, false, true, false, false));
        assertTrue(StrikeRules.mayStrike(1000, 0, true, false, true, true, false));
        assertTrue(StrikeRules.mayStrike(1000, 0, true, false, true, false, true));
        assertTrue(StrikeRules.mayStrike(1000, 0, true, false, false, false, false), "a land mob is not held by it");
    }

    @Test
    void aJuggernautSeesTwiceAsFar() {
        assertEquals(32.0, StrikeRules.sight(false), 1e-9);
        assertEquals(64.0, StrikeRules.sight(true), 1e-9);
    }

    @Test
    void aTargetIsKeptUntilItIsOneAndAHalfSightsAway() {
        double s = StrikeRules.SIGHT;
        assertTrue(StrikeRules.keep(true, 47.9 * 47.9, s));
        assertFalse(StrikeRules.keep(true, 48.1 * 48.1, s));
        assertFalse(StrikeRules.keep(false, 1.0, s), "not a valid player: never kept");
    }
}
