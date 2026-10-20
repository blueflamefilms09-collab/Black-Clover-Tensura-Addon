package com.newuniverse.nusmp.perf;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the throttle, stagger and level-of-detail decisions of the performance pass are pure functions every caller shares ({@link PerfMath}); this pins their answers: a period of 1 is every
 * tick, each id owns exactly one tick of its period, ids spread over the period, the level of detail steps at the documented distances (1, 3/4, 1/2, cull), the squared version agrees with the plain
 * one, and the aura rule keeps all of them near, the most recent few in the middle and none far away.
 */
class PerfMathTest {
    @Test
    void clampAndInterval() {
        assertEquals(5, PerfMath.clamp(5, 1, 10));
        assertEquals(1, PerfMath.clamp(-3, 1, 10));
        assertEquals(10, PerfMath.clamp(99, 1, 10));
        assertEquals(PerfMath.MIN_INTERVAL, PerfMath.interval(0));
        assertEquals(PerfMath.MAX_INTERVAL, PerfMath.interval(100000));
        assertEquals(20, PerfMath.interval(20));
    }

    @Test
    void aPeriodOfOneOrLessIsEveryTick() {
        for (long t = 0; t < 50; t++) {
            assertTrue(PerfMath.due(t, 1, 0));
            assertTrue(PerfMath.due(t, 0, 3));
            assertTrue(PerfMath.dueFor(t, -4, 7));
        }
    }

    @Test
    void anIdOwnsExactlyOneTickOfEachPeriod() {
        for (int period : new int[]{2, 5, 10, 20, 40}) {
            for (int id : new int[]{0, 1, 7, 19, 123456, -1, -77}) {
                int hits = 0;
                for (long t = 1000; t < 1000 + period; t++) if (PerfMath.dueFor(t, period, id)) hits++;
                assertEquals(1, hits, "period " + period + " id " + id);
                int next = 0;
                for (long t = 1000 + period; t < 1000 + 2L * period; t++) if (PerfMath.dueFor(t, period, id)) next++;
                assertEquals(1, next);
            }
        }
    }

    @Test
    void theSameIdIsDueEveryPeriodTicks() {
        long first = -1, second = -1;
        for (long t = 0; t < 100; t++) {
            if (PerfMath.dueFor(t, 20, 13)) {
                if (first < 0) first = t;
                else if (second < 0) second = t;
            }
        }
        assertEquals(20, second - first);
    }

    @Test
    void idsSpreadOverThePeriodSoACrowdDoesNotWorkOnOneTick() {
        int period = 20;
        int[] perTick = new int[period];
        for (int id = 0; id < 400; id++) {
            for (int t = 0; t < period; t++) if (PerfMath.dueFor(t, period, id)) perTick[t]++;
        }
        for (int n : perTick) assertEquals(20, n);
    }

    @Test
    void negativeTicksAndPhasesStayInRange() {
        assertTrue(PerfMath.phaseOf(-1, 20) >= 0 && PerfMath.phaseOf(-1, 20) < 20);
        assertEquals(19, PerfMath.phaseOf(-1, 20));
        assertTrue(PerfMath.due(-20, 20, 0));
        assertFalse(PerfMath.due(-19, 20, 0));
    }

    @Test
    void fitNeverGoesOutsideZeroAndWanted() {
        assertEquals(0, PerfMath.fit(0, 10));
        assertEquals(0, PerfMath.fit(5, 0));
        assertEquals(0, PerfMath.fit(5, -3));
        assertEquals(3, PerfMath.fit(5, 3));
        assertEquals(5, PerfMath.fit(5, 50));
    }

    @Test
    void lodDetailStepsAtTheDocumentedDistances() {
        assertEquals(1f, PerfMath.lodDetail(0, 96, 256));
        assertEquals(1f, PerfMath.lodDetail(96, 96, 256));
        assertEquals(0.75f, PerfMath.lodDetail(96.5, 96, 256));
        assertEquals(0.75f, PerfMath.lodDetail(176, 96, 256));
        assertEquals(0.5f, PerfMath.lodDetail(176.5, 96, 256));
        assertEquals(0.5f, PerfMath.lodDetail(256, 96, 256));
        assertEquals(0f, PerfMath.lodDetail(256.01, 96, 256));
        // far at or below near: a single cut
        assertEquals(1f, PerfMath.lodDetail(50, 60, 60));
        assertEquals(0f, PerfMath.lodDetail(61, 60, 60));
    }

    @Test
    void theSquaredLodAgreesWithThePlainOne() {
        for (double d = 0; d < 300; d += 0.37) {
            assertEquals(PerfMath.lodDetail(d, 64, 160), PerfMath.lodDetailSq(d * d, 64, 160), "distance " + d);
        }
    }

    @Test
    void auraCountKeepsAllNearSomeInTheMiddleNoneFar() {
        double near = 48, far = 96;
        assertEquals(5, PerfMath.auraCount(5, 10 * 10, near, far, 2));
        assertEquals(5, PerfMath.auraCount(5, 48 * 48, near, far, 2));
        assertEquals(2, PerfMath.auraCount(5, 60 * 60, near, far, 2));
        assertEquals(1, PerfMath.auraCount(1, 60 * 60, near, far, 2));
        assertEquals(0, PerfMath.auraCount(5, 60 * 60, near, far, 0));
        assertEquals(0, PerfMath.auraCount(5, 97 * 97, near, far, 2));
        assertEquals(0, PerfMath.auraCount(0, 1, near, far, 2));
    }

    @Test
    void theNewestAurasAreTheOnesKept() {
        List<long[]> windows = new ArrayList<>();
        windows.add(new long[]{100, 500});    // oldest
        windows.add(new long[]{300, 500});
        windows.add(new long[]{200, 500});
        windows.add(new long[]{400, 500});    // newest
        windows.add(new long[]{450, 120});    // already over at tick 130: not counted
        long cut = PerfMath.nthNewestStart(windows, 130f, 2);
        assertEquals(300, cut);
        int kept = 0;
        for (long[] w : windows) if (130f < w[1] && w[0] >= cut) kept++;
        assertEquals(2, kept);
        // asking for more than there are keeps everything
        long all = PerfMath.nthNewestStart(windows, 130f, 10);
        for (long[] w : windows) if (130f < w[1]) assertTrue(w[0] >= all);
    }

    @Test
    void aTieOnTheStartTickIsKeptOrDroppedTogether() {
        List<long[]> windows = new ArrayList<>();
        windows.add(new long[]{10, 99});
        windows.add(new long[]{10, 99});
        windows.add(new long[]{10, 99});
        windows.add(new long[]{5, 99});
        long cut = PerfMath.nthNewestStart(windows, 0f, 2);
        int kept = 0;
        for (long[] w : windows) if (w[0] >= cut) kept++;
        assertEquals(3, kept);                    // the three that started together; never splits them
    }

    @Test
    void msPerSecondIsTheProduct() {
        assertEquals(2.0, PerfMath.msPerSecond(20, 100_000));
    }
}
