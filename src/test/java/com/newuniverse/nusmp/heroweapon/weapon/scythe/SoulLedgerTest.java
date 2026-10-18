package com.newuniverse.nusmp.heroweapon.weapon.scythe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: the souls are the Scythe's loop, so their rules are pinned: they start at none, each gain counts up to the cap and no further, they fade exactly two
 * minutes after the last one was gathered (a new gain restarts the clock), spending takes them all, and a faded ledger spends nothing.
 */
class SoulLedgerTest {
    @Test
    void gainsCountUpToTheCap() {
        SoulLedger l = new SoulLedger();
        assertEquals(0, l.souls(0));
        for (int i = 1; i <= ScytheNumbers.SOUL_CAP; i++) {
            assertTrue(l.gain(1, ScytheNumbers.SOUL_CAP, i), "soul " + i);
            assertEquals(i, l.souls(i));
        }
        assertFalse(l.gain(1, ScytheNumbers.SOUL_CAP, 11), "nothing is gained at the cap");
        assertEquals(ScytheNumbers.SOUL_CAP, l.souls(11));
        assertTrue(l.gain(1, ScytheNumbers.SOUL_CAP_DRAGON, 12), "the Dragon Soul cap is higher");
        assertEquals(ScytheNumbers.SOUL_CAP + 1, l.souls(12));
        assertFalse(l.gain(0, 99, 13));
        assertFalse(l.gain(-1, 99, 13));
    }

    @Test
    void theyFadeTwoMinutesAfterTheLastGain() {
        SoulLedger l = new SoulLedger();
        l.gain(3, 10, 1000);
        assertEquals(3, l.souls(1000 + ScytheNumbers.SOUL_FADE_TICKS), "still there on the last tick");
        assertEquals(0, l.souls(1000 + ScytheNumbers.SOUL_FADE_TICKS + 1), "gone one tick later");
        l.gain(2, 10, 5000);
        l.gain(1, 10, 6000);
        assertEquals(3, l.souls(6000 + ScytheNumbers.SOUL_FADE_TICKS), "a new gain restarts the clock for all");
        assertTrue(l.fade(6000 + ScytheNumbers.SOUL_FADE_TICKS + 1));
        assertFalse(l.fade(6000 + ScytheNumbers.SOUL_FADE_TICKS + 2), "nothing left to fade");
    }

    @Test
    void spendingTakesEverythingAndAFadedLedgerSpendsNothing() {
        SoulLedger l = new SoulLedger();
        l.gain(4, 10, 100);
        assertEquals(4, l.spend(150));
        assertEquals(0, l.souls(150));
        assertEquals(0, l.spend(160));
        l.gain(5, 10, 200);
        assertEquals(0, l.spend(200 + ScytheNumbers.SOUL_FADE_TICKS + 5), "they faded before they could be spent");
        l.gain(2, 10, 9000);
        l.reset();
        assertEquals(0, l.souls(9000));
    }
}
