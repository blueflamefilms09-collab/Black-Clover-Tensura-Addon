package com.newuniverse.nusmp.avatar.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A goal is asked "can you start?" only on every other tick, on the parity that belongs to the entity: the turn of a rhythm must come round for both parities. The
 * plain modulus (the bug this replaces) does not: it is true only on even counts, which half of the Avatars never see.
 */
class TickSlotTest {
    /** How many asked ticks (of one parity) in a stretch of {@code span} ticks are the Avatar's turn. */
    private static int turns(int parity, int period, int span) {
        int n = 0;
        for (int t = parity; t < span; t += 2) if (TickSlot.on(t, period)) n++;
        return n;
    }

    @Test
    void bothParitiesGetExactlyOneTurnPerPeriod() {
        for (int period : new int[]{4, 20, 40, 60}) {
            for (int parity = 0; parity < 2; parity++) {
                assertEquals(10, turns(parity, period, period * 10), "period " + period + " parity " + parity);
            }
        }
    }

    @Test
    void thePlainModulusFailsForOddParity() {
        int hits = 0;
        for (int t = 1; t < 400; t += 2) if (t % 20 == 0) hits++;
        assertEquals(0, hits, "this is the latent bug: an odd-parity entity never sees tickCount % 20 == 0");
    }

    @Test
    void tinyPeriodsStillMakeSense() {
        assertTrue(TickSlot.on(0, 4));
        assertTrue(TickSlot.on(1, 4));
        assertFalse(TickSlot.on(2, 4));
        for (int t = 0; t <= 40; t++) {
            assertEquals(TickSlot.on(t, 4), TickSlot.on(t, 1), "period 1 counts as 4 at " + t);
            assertEquals(TickSlot.on(t, 4), TickSlot.on(t, 0), "period 0 counts as 4 at " + t);
            assertEquals(TickSlot.on(t, 4), TickSlot.on(t, -7), "a negative period counts as 4 at " + t);
        }
    }
}
