package com.newuniverse.nusmp.relics.order;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The scan budget: at most sixteen stacks per tick, never a stall, never a skipped item, and players are staggered. */
class ScanCursorTest {
    @Test
    void aTickTakesAsManyItemsAsFitTheBudget() {
        int[] cost = new int[40];
        java.util.Arrays.fill(cost, 1);
        ScanCursor c = new ScanCursor(cost);
        assertEquals(16, c.take(ScanCursor.PER_TICK));
        assertEquals(16, c.take(ScanCursor.PER_TICK));
        assertEquals(8, c.take(ScanCursor.PER_TICK));
        assertTrue(c.done());
        assertEquals(0, c.take(ScanCursor.PER_TICK));
    }

    @Test
    void aBigItemCountsForTheStacksInsideIt() {
        ScanCursor c = new ScanCursor(new int[]{1, 10, 10, 1});
        assertEquals(2, c.take(16), "1 + 10 fit, the next 10 does not");
        assertEquals(2, c.take(16), "10 + 1");
        assertTrue(c.done());
    }

    @Test
    void anItemBiggerThanTheBudgetStillGoesThroughAlone() {
        ScanCursor c = new ScanCursor(new int[]{28, 1});
        assertEquals(1, c.take(16));
        assertEquals(1, c.take(16));
        assertTrue(c.done());
    }

    @Test
    void everyItemIsTakenExactlyOnceWhateverTheCosts() {
        int[] cost = new int[200];
        for (int i = 0; i < cost.length; i++) cost[i] = 1 + (i * 7) % 29;
        ScanCursor c = new ScanCursor(cost);
        int total = 0, ticks = 0;
        while (!c.done()) {
            int before = c.position();
            int n = c.take(ScanCursor.PER_TICK);
            assertTrue(n >= 1);
            assertEquals(before + n, c.position());
            if (n > 1) {
                int spent = 0;
                for (int i = before; i < before + n; i++) spent += cost[i];
                assertTrue(spent <= ScanCursor.PER_TICK, "a multi-item tick stays inside the budget");
            }
            total += n;
            ticks++;
            assertTrue(ticks < 1000);
        }
        assertEquals(200, total);
    }

    @Test
    void anEmptyListIsDoneAtOnce() {
        assertTrue(new ScanCursor(new int[0]).done());
        assertTrue(new ScanCursor(null).done());
    }

    @Test
    void playersStartTheirScansInDifferentTicks() {
        int interval = ScanCursor.PLAYER_INTERVAL;
        assertEquals(100, interval);
        Set<Integer> startedAt = new HashSet<>();
        for (int id = 0; id < 20; id++) {
            int starts = 0;
            for (long tick = 0; tick < interval; tick++) if (ScanCursor.shouldStart(tick, id, interval)) { starts++; startedAt.add((int) tick); }
            assertEquals(1, starts, "each player scans once per interval");
        }
        assertEquals(20, startedAt.size(), "twenty players, twenty different ticks");
    }

    @Test
    void staggerSurvivesNegativeAndHugeValues() {
        assertTrue(ScanCursor.shouldStart(100, 0, 100));
        assertTrue(ScanCursor.shouldStart(0, -100, 100));
        long big = Long.MAX_VALUE - 1;
        int hits = 0;
        for (long t = big - 200; t < big; t++) if (ScanCursor.shouldStart(t, 3, 100)) hits++;
        assertEquals(2, hits);
    }
}
