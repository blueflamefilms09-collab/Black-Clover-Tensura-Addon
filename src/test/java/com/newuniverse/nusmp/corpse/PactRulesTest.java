package com.newuniverse.nusmp.corpse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The pact: window, one use each, three allies, no boss, no second revive while decaying. */
class PactRulesTest {
    private static final long TEN_MIN = 12000;

    @Test
    void casterIsSavedOnceInsideTheWindow() {
        PactRules<Integer> p = new PactRules<>();
        assertEquals(PactRules.Verdict.NO_PACT, p.check(1, false, false, 0));
        assertTrue(p.arm(1, 0, TEN_MIN));
        assertFalse(p.arm(1, 5, TEN_MIN), "already armed");
        assertEquals(PactRules.Verdict.REVIVE, p.check(1, false, false, 100));
        assertEquals(Integer.valueOf(1), p.commit(1, 100, 1800));
        assertEquals(PactRules.Verdict.STILL_DECAYING, p.check(1, false, false, 200));
        assertEquals(PactRules.Verdict.ALREADY_USED, p.check(1, false, false, 1900), "decay over, but the one use is spent");
    }

    @Test
    void expiredPactSavesNobody() {
        PactRules<Integer> p = new PactRules<>();
        p.arm(1, 0, TEN_MIN);
        assertEquals(PactRules.Verdict.EXPIRED, p.check(1, false, false, TEN_MIN));
        assertNull(p.commit(1, TEN_MIN, 100));
    }

    @Test
    void atMostThreeAllies() {
        PactRules<Integer> p = new PactRules<>();
        p.arm(1, 0, TEN_MIN);
        assertEquals(PactRules.Tap.SELF, p.tap(1, 1, 0));
        assertEquals(PactRules.Tap.ADDED, p.tap(1, 2, 0));
        assertEquals(PactRules.Tap.ALREADY, p.tap(1, 2, 0));
        assertEquals(PactRules.Tap.ADDED, p.tap(1, 3, 0));
        assertEquals(PactRules.Tap.ADDED, p.tap(1, 4, 0));
        assertEquals(PactRules.Tap.FULL, p.tap(1, 5, 0));
        assertEquals(PactRules.Verdict.NO_PACT, p.check(5, false, false, 10));
        assertEquals(PactRules.Verdict.REVIVE, p.check(4, false, false, 10));
        assertEquals(Integer.valueOf(1), p.commit(4, 10, 100));
    }

    @Test
    void tapWithoutPactIsRefused() {
        assertEquals(PactRules.Tap.NO_PACT, new PactRules<Integer>().tap(1, 2, 0));
    }

    @Test
    void neverBossNeverUnsaveable() {
        PactRules<Integer> p = new PactRules<>();
        p.arm(1, 0, TEN_MIN);
        assertEquals(PactRules.Verdict.BOSS, p.check(1, true, false, 10));
        assertEquals(PactRules.Verdict.UNSAVEABLE, p.check(1, false, true, 10));
        assertEquals(PactRules.Verdict.REVIVE, p.check(1, false, false, 10), "the refusals changed nothing");
    }

    @Test
    void pruneEndsDecayAndRespawnClearsIt() {
        PactRules<Integer> p = new PactRules<>();
        p.arm(1, 0, TEN_MIN);
        p.commit(1, 0, 100);
        assertTrue(p.isDecaying(1, 99));
        assertEquals(java.util.List.of(1), p.prune(100));
        assertFalse(p.isDecaying(1, 100));
        p.arm(2, 0, TEN_MIN);
        p.commit(2, 0, 1000);
        p.clearDecay(2);
        assertFalse(p.isDecaying(2, 5));
    }
}
