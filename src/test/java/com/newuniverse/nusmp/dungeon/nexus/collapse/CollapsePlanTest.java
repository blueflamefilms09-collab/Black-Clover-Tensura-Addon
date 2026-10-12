package com.newuniverse.nusmp.dungeon.nexus.collapse;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The clock of the Treasury Collapse: three minutes, monotonic, bounded and the same every time. */
class CollapsePlanTest {
    @Test
    void theStagesFollowTheClockAndTheWipeEndsIt() {
        assertSame(CollapsePlan.Stage.CRACKS, CollapsePlan.stageAt(0));
        assertSame(CollapsePlan.Stage.CRACKS, CollapsePlan.stageAt(CollapsePlan.CRACKS_END - 1));
        assertSame(CollapsePlan.Stage.ROLLING, CollapsePlan.stageAt(CollapsePlan.CRACKS_END));
        assertSame(CollapsePlan.Stage.ROLLING, CollapsePlan.stageAt(CollapsePlan.ROLL_END - 1));
        assertSame(CollapsePlan.Stage.FINAL, CollapsePlan.stageAt(CollapsePlan.ROLL_END));
        assertSame(CollapsePlan.Stage.FINAL, CollapsePlan.stageAt(CollapsePlan.TOTAL - 1));
        assertSame(CollapsePlan.Stage.WIPE, CollapsePlan.stageAt(CollapsePlan.TOTAL));
        assertSame(CollapsePlan.Stage.WIPE, CollapsePlan.stageAt(Long.MAX_VALUE));
        assertEquals(3600L, CollapsePlan.TOTAL, "exactly three minutes");
        assertTrue(CollapsePlan.CRACKS_END < CollapsePlan.ROLL_END && CollapsePlan.ROLL_END < CollapsePlan.FRONT_END && CollapsePlan.FRONT_END < CollapsePlan.TOTAL);
        assertEquals(CollapsePlan.TOTAL, CollapsePlan.remaining(0));
        assertEquals(0L, CollapsePlan.remaining(CollapsePlan.TOTAL + 5000));
        assertEquals(0L, CollapsePlan.remaining(Long.MAX_VALUE));
    }

    @Test
    void theCountdownMarksAreTheOwnersAndInOrder() {
        int[] left = CollapsePlan.MARKS;
        assertArrayEquals(new int[]{3600, 2400, 1200, 600, 200, 100, 80, 60, 40, 20}, left, "3:00, 2:00, 1:00, 0:30, 0:10, then 0:05 down to 0:01");
        for (int i = 1; i < left.length; i++) assertTrue(left[i] < left[i - 1], "strictly counting down");
        assertEquals(CollapsePlan.TOTAL, left[0]);
    }

    @Test
    void aMarkIsSaidOnceAtItsTimeAndOnlyTheNewestAfterAStall() {
        assertEquals(0, CollapsePlan.latestDue(0, 0), "3:00 is said at the trigger");
        assertEquals(-1, CollapsePlan.latestDue(1, 1199));
        assertEquals(1, CollapsePlan.latestDue(1, 1200), "2:00 at 1200");
        assertEquals(1, CollapsePlan.latestDue(0, 1200), "two are due: the newest is said");
        assertEquals(9, CollapsePlan.latestDue(0, 3590), "after a long stall, 0:01 and not 3:00");
        assertEquals(-1, CollapsePlan.latestDue(10, 5000), "all said");
        assertEquals(9, CollapsePlan.latestDue(-5, 99999), "a bad index counts as the first");
        int next = 0, said = 0;
        for (long e = 0; e <= CollapsePlan.TOTAL + 10; e++) {
            int m = CollapsePlan.latestDue(next, e);
            if (m >= 0) {
                assertTrue(m >= next);
                assertEquals(e, CollapsePlan.TOTAL - CollapsePlan.MARKS[m], "ticked every tick, each mark comes exactly on time");
                next = m + 1;
                said++;
            }
        }
        assertEquals(CollapsePlan.MARKS.length, said);
    }

    @Test
    void theBudgetsOnlyRiseAndStayUnderTheirCaps() {
        int lastUnits = 0, lastEntities = 0;
        for (long e = 0; e <= CollapsePlan.TOTAL + 100; e++) {
            int u = CollapsePlan.sweepUnits(e), n = CollapsePlan.entityBudget(e);
            assertTrue(u >= lastUnits && n >= lastEntities, "monotonic at " + e);
            assertTrue(u <= CollapsePlan.SWEEP_TO && n <= CollapsePlan.ENTITIES_TO);
            if (e < CollapsePlan.CRACKS_END) assertTrue(u == 0 && n == 0, "nothing falls while it only cracks");
            lastUnits = u;
            lastEntities = n;
        }
        assertEquals(CollapsePlan.SWEEP_FROM, CollapsePlan.sweepUnits(CollapsePlan.CRACKS_END));
        assertEquals(CollapsePlan.SWEEP_TO, CollapsePlan.sweepUnits(CollapsePlan.TOTAL));
        assertEquals(CollapsePlan.ENTITIES_FROM, CollapsePlan.entityBudget(CollapsePlan.CRACKS_END));
        assertEquals(CollapsePlan.ENTITIES_TO, CollapsePlan.entityBudget(CollapsePlan.TOTAL));
        assertTrue(CollapsePlan.SWEEP_TO <= CollapsePlan.WIPE_UNITS, "the rolling fall is never faster than the wipe's own budget");
        assertTrue(CollapsePlan.ENTITIES_TO * FallLedger.WINDOW > CollapsePlan.MAX_ALIVE, "the cap is a real limit");
    }

    @Test
    void theFrontStartsLateSpeedsUpAndHasCoveredEverythingBeforeTheLastSeconds() {
        for (int open : new int[]{0, 1, 7, 399, 6814, 20000}) {
            int last = 0;
            for (long e = 0; e <= CollapsePlan.TOTAL + 10; e++) {
                int c = CollapsePlan.frontColumns(e, open);
                assertTrue(c >= last, "monotonic at " + e);
                assertTrue(c >= 0 && c <= open, "never more than there are");
                if (e <= CollapsePlan.CRACKS_END) assertEquals(0, c, "nothing is due while it cracks");
                last = c;
            }
            assertEquals(open, CollapsePlan.frontColumns(CollapsePlan.FRONT_END, open));
            assertEquals(open, CollapsePlan.frontColumns(Long.MAX_VALUE, open));
        }
        int open = 10_000;
        long half = (CollapsePlan.CRACKS_END + CollapsePlan.FRONT_END) / 2;
        assertTrue(CollapsePlan.frontColumns(half, open) < open / 2, "slow at first: less than half is down by the middle of the roll");
        assertTrue(CollapsePlan.TOTAL - CollapsePlan.FRONT_END >= 200, "the last ten seconds and more have nothing left to fall but the corridors");
    }

    @Test
    void thePersonalWarningLooksFourSecondsAhead() {
        int open = 6000;
        for (long e = 0; e <= CollapsePlan.TOTAL; e += 7) {
            int w = CollapsePlan.warnLimit(e, open);
            assertTrue(w >= CollapsePlan.frontColumns(e, open) && w <= open);
        }
        assertEquals(0, CollapsePlan.warnLimit(CollapsePlan.CRACKS_END - CollapsePlan.WARN_AHEAD, open), "no warning before the front can start");
        assertTrue(CollapsePlan.warnLimit(CollapsePlan.CRACKS_END - 1, open) > 0, "the last four seconds before the front starts are warned");
        assertEquals(80, CollapsePlan.WARN_AHEAD);
    }

    @Test
    void cracksComeFasterAndStopForTheLastHalfMinute() {
        assertEquals(14, CollapsePlan.crackInterval(0));
        assertEquals(5, CollapsePlan.crackInterval(CollapsePlan.CRACKS_END));
        assertEquals(5, CollapsePlan.crackInterval(CollapsePlan.TOTAL));
        int last = Integer.MAX_VALUE;
        for (long e = 0; e <= CollapsePlan.TOTAL; e++) {
            int i = CollapsePlan.crackInterval(e);
            assertTrue(i <= last && i >= 5 && i <= 14);
            last = i;
        }
        int cracks = 0;
        for (long e = 0; e < CollapsePlan.TOTAL; e++) {
            if (CollapsePlan.crackDue(e)) {
                cracks++;
                assertTrue(e < CollapsePlan.ROLL_END, "no cracks once the final half minute begins");
            }
        }
        assertTrue(cracks > 100 && cracks < 700, "a steady crackle, not a flood: " + cracks);
        assertFalse(CollapsePlan.crackDue(-1));
    }

    @Test
    void aThirdOfTheCracksShakeABlockLooseAndOnlyWhileItCracks() {
        int drips = 0, total = 0;
        for (long e = 0; e < CollapsePlan.CRACKS_END; e++) {
            if (!CollapsePlan.crackDue(e)) continue;
            total++;
            if (CollapsePlan.crackDrips(7, e)) drips++;
        }
        assertTrue(drips > total / 5 && drips < total / 2, drips + " of " + total);
        for (long e = CollapsePlan.CRACKS_END; e < CollapsePlan.TOTAL; e++) assertFalse(CollapsePlan.crackDrips(7, e), "the fall itself is the show after that");
    }

    @Test
    void theCorridorsGetABoundedDripThatLandsFiveSecondsBeforeTheEnd() {
        int n = 0;
        long last = -1;
        for (long e = 0; e <= CollapsePlan.TOTAL + 50; e++) {
            if (!CollapsePlan.dripDue(e)) continue;
            assertTrue(e >= CollapsePlan.DRIP_FROM && e < CollapsePlan.DRIP_UNTIL);
            assertEquals(0, e % CollapsePlan.DRIP_EVERY);
            n++;
            last = e;
        }
        assertEquals(48, n);
        assertTrue(n <= CollapsePlan.MAX_CORRIDOR_DRIPS);
        assertTrue(CollapsePlan.TOTAL - (last + CollapsePlan.DRIP_LEAD) >= 100, "the last block lands at least five seconds before the end");
        assertEquals(3, CollapsePlan.DRIP_DAMAGE, "a drip hits for 3 at the most");
        assertTrue(CollapsePlan.DRIP_LEAD >= 20, "a second of dust before it falls");
        assertTrue(CollapsePlan.FRONT_HIT <= CollapsePlan.DRIP_DAMAGE);
    }

    @Test
    void theScrambleIsStableAndPicksStayInRange() {
        Set<Long> seen = new HashSet<>();
        for (long a = 0; a < 40; a++) for (long b = 0; b < 40; b++) seen.add(CollapsePlan.mix(a, b));
        assertTrue(seen.size() > 1550, "few collisions");
        assertEquals(CollapsePlan.mix(3, 9), CollapsePlan.mix(3, 9));
        assertNotEquals(CollapsePlan.mix(3, 9), CollapsePlan.mix(9, 3));
        for (int n : new int[]{1, 2, 3, 120, 7000}) {
            for (long b = 0; b < 500; b++) {
                int p = CollapsePlan.pick(11, b, n);
                assertTrue(p >= 0 && p < n);
            }
        }
        assertEquals(0, CollapsePlan.pick(1, 2, 0), "nothing to pick from is the first");
        assertEquals(0, CollapsePlan.pick(1, 2, -4));
        int[] hist = new int[6];
        for (long b = 0; b < 6000; b++) hist[CollapsePlan.pick(5, b, 6)]++;
        for (int h : hist) assertTrue(h > 800 && h < 1200, "even enough: " + h);
    }
}
