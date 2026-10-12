package com.newuniverse.nusmp.dungeon.nexus.collapse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Counts the falling blocks in the air without scanning for them. */
class FallLedgerTest {
    @Test
    void aBlockCountsAsAliveForTheWholeWindowAndThenNoMore() {
        FallLedger l = new FallLedger();
        assertEquals(0, l.alive(100));
        l.add(100, 5);
        assertEquals(5, l.alive(100));
        assertEquals(5, l.alive(100 + FallLedger.WINDOW - 1));
        assertEquals(0, l.alive(100 + FallLedger.WINDOW));
    }

    @Test
    void roomIsWhatIsLeftUnderTheCap() {
        FallLedger l = new FallLedger();
        l.add(10, 100);
        l.add(11, 90);
        assertEquals(190, l.alive(12));
        assertEquals(50, l.room(12, 240));
        l.add(12, 80);
        assertEquals(0, l.room(12, 240), "never negative");
        assertEquals(0, l.room(12, 100));
        assertEquals(240, l.room(12 + FallLedger.WINDOW + 2, 240), "all of it again once they have landed");
    }

    @Test
    void nothingOrANegativeNumberIsIgnored() {
        FallLedger l = new FallLedger();
        l.add(5, 0);
        l.add(5, -9);
        assertEquals(0, l.alive(5));
        l.add(5, 2);
        l.add(5, 3);
        assertEquals(5, l.alive(5), "several in one tick add up");
    }

    @Test
    void aSlotIsReusedOnlyAfterItsBlocksAreOld() {
        FallLedger l = new FallLedger();
        for (long t = 0; t < 1000; t++) {
            l.add(t, 2);
            assertEquals(2 * Math.min(t + 1, FallLedger.WINDOW), l.alive(t), "a sliding window at " + t);
        }
    }

    @Test
    void aClockThatJumpsOrRunsBackwardsCannotBreakIt() {
        FallLedger l = new FallLedger();
        l.add(500, 7);
        assertEquals(0, l.alive(499), "blocks from the future are not counted yet");
        l.add(10_000_000_000L, 4);
        assertEquals(4, l.alive(10_000_000_000L));
        l.reset();
        assertEquals(0, l.alive(10_000_000_000L));
        l.add(Long.MAX_VALUE / 2, 1);
        assertEquals(1, l.alive(Long.MAX_VALUE / 2));
    }
}
