package com.newuniverse.nusmp.heroweapon.weapon.staff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: the casting rhythm is the Vassal Staff's signature and it is only bookkeeping, so it is pinned tick by tick: a chain continues only inside the window,
 * it saturates at the documented number of stacks, a melee beat can extend a running chain but never start one, the haste of Fenrir Rod ends exactly when it
 * should and cannot be shortened by a later call, and a cleared rhythm is a fresh one.
 */
class CastRhythmTest {
    @Test
    void aChainContinuesInsideTheWindowAndRestartsOutsideIt() {
        CastRhythm r = new CastRhythm();
        assertEquals(0, r.stacks(0));
        r.cast(100);
        assertEquals(0, r.stacks(100), "the first cast has no earlier cast to count");
        r.cast(100 + StaffNumbers.RHYTHM_WINDOW);
        assertEquals(1, r.stacks(100 + StaffNumbers.RHYTHM_WINDOW), "inside the window the chain goes on");
        long late = 100 + StaffNumbers.RHYTHM_WINDOW + StaffNumbers.RHYTHM_WINDOW + 1;
        r.cast(late);
        assertEquals(0, r.stacks(late), "after a pause a new chain begins");
        assertEquals(0, r.stacks(late + StaffNumbers.RHYTHM_WINDOW + 1), "and the stacks fade when no spell follows");
    }

    @Test
    void theChainSaturates() {
        CastRhythm r = new CastRhythm();
        for (int i = 0; i < 12; i++) r.cast(i * 10L);
        assertEquals(StaffNumbers.RHYTHM_MAX, r.stacks(110));
        assertEquals(0.85, r.factor(110, true), 1e-9);
        assertEquals(1.0, r.factor(110, false), 1e-9, "no Quick Chant, no bonus");
    }

    @Test
    void aMeleeBeatExtendsARunningChainButNeverStartsOne() {
        CastRhythm r = new CastRhythm();
        r.beat(5);
        assertEquals(0, r.stacks(5));
        r.cast(10);
        r.beat(40);
        assertEquals(1, r.stacks(40), "the beat counted as a cast of the chain");
        r.beat(40 + StaffNumbers.RHYTHM_WINDOW + 1);
        assertEquals(0, r.stacks(40 + StaffNumbers.RHYTHM_WINDOW + 1), "a beat after the window does nothing");
    }

    @Test
    void theHasteEndsExactlyAndOnlyGrows() {
        CastRhythm r = new CastRhythm();
        assertFalse(r.hasted(0));
        r.haste(200);
        assertTrue(r.hasted(199));
        assertFalse(r.hasted(200));
        assertEquals(0.65, r.factor(150, false), 1e-9);
        r.haste(120);
        assertTrue(r.hasted(150), "a shorter second haste does not cut the first short");
        r.haste(300);
        assertTrue(r.hasted(299));
    }

    @Test
    void clearingGivesAFreshRhythm() {
        CastRhythm r = new CastRhythm();
        r.cast(0);
        r.cast(10);
        r.haste(500);
        r.clear();
        assertEquals(0, r.stacks(10));
        assertFalse(r.hasted(10));
        assertEquals(1.0, r.factor(10, true), 1e-9);
    }
}
