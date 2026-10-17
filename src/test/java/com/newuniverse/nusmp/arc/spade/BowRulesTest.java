package com.newuniverse.nusmp.arc.spade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The King's order to kneel: who is spared, who is crushed, and what each costs. */
class BowRulesTest {
    @Test
    void crouchingAtTheFallOrJustBeforeItCountsAsABow() {
        assertTrue(BowRules.bowed(100, 100, 4), "crouching at the fall");
        assertTrue(BowRules.bowed(100, 96, 4), "four ticks of lag are allowed");
        assertFalse(BowRules.bowed(100, 95, 4), "five is too long ago");
        assertFalse(BowRules.bowed(100, -1, 4), "never seen crouching");
        assertFalse(BowRules.bowed(100, 130, 4), "a crouch from the future is no crouch");
        assertTrue(BowRules.bowed(100, 100, 0) && !BowRules.bowed(100, 99, 0), "no grace: only at the fall");
        assertTrue(BowRules.bowed(100, 100, -3), "a negative grace is none");
    }

    @Test
    void theBestExcuseWins() {
        assertEquals(BowRules.Verdict.UNBOWED, BowRules.verdict(true, true, true));
        assertEquals(BowRules.Verdict.UNBOWED, BowRules.verdict(true, false, false));
        assertEquals(BowRules.Verdict.POCKET, BowRules.verdict(false, true, true));
        assertEquals(BowRules.Verdict.BOWED, BowRules.verdict(false, false, true));
        assertEquals(BowRules.Verdict.CRUSHED, BowRules.verdict(false, false, false));
    }

    @Test
    void anOrdinaryBowCrushesOnlyTheOnesWhoDidNotBow() {
        for (BowRules.Verdict v : BowRules.Verdict.values()) {
            boolean crushed = v == BowRules.Verdict.CRUSHED;
            assertEquals(crushed ? 1.0 : 0.0, BowRules.bowFactor(v), 1e-12, v.name());
            assertEquals(crushed, BowRules.pinnedByBow(v), v.name());
        }
    }

    @Test
    void theUltimateAsksForMoreThanACrouch() {
        assertEquals(1.0, BowRules.kneelFactor(BowRules.Verdict.CRUSHED), 1e-12);
        assertEquals(1.0, BowRules.kneelFactor(BowRules.Verdict.BOWED), 1e-12, "a crouch is no shelter from a falling sky");
        assertEquals(0.40, BowRules.kneelFactor(BowRules.Verdict.UNBOWED), 1e-12);
        assertEquals(0.25, BowRules.kneelFactor(BowRules.Verdict.POCKET), 1e-12);
        assertTrue(BowRules.pinnedByKneel(BowRules.Verdict.CRUSHED));
        assertFalse(BowRules.pinnedByKneel(BowRules.Verdict.POCKET));
        assertFalse(BowRules.pinnedByKneel(BowRules.Verdict.UNBOWED));
        assertTrue(BowRules.kneelFactor(BowRules.Verdict.POCKET) < BowRules.kneelFactor(BowRules.Verdict.UNBOWED), "the pocket is the better shelter");
    }
}
