package com.newuniverse.nusmp.perf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the probe is how the owner SEES which part is slow, and it must cost nothing while it is off. This pins both: off by default and {@code begin()} answers 0 and {@code end} records nothing;
 * on, a section counts its calls and times; the rolling statistics keep a window and the totals keep everything; the report orders sections by total time, caps at n, and filters by side
 * prefix; a reset clears; switching it on starts clean. It puts the switch back the way it found it.
 */
class PerfProbeTest {
    @Test
    void offByDefaultAndFreeWhenOff() {
        boolean was = PerfProbe.enabled();
        try {
            PerfProbe.setEnabled(false);
            PerfProbe.Section s = PerfProbe.section("s.test.off");
            long t0 = PerfProbe.begin();
            assertEquals(0L, t0);
            PerfProbe.end(s, t0);
            assertEquals(0L, s.row().calls());
        } finally {
            PerfProbe.setEnabled(was);
        }
    }

    @Test
    void aSectionIsTheSameObjectEveryTime() {
        assertSame(PerfProbe.section("s.test.same"), PerfProbe.section("s.test.same"));
    }

    @Test
    void onItCountsCallsAndTime() {
        boolean was = PerfProbe.enabled();
        try {
            PerfProbe.setEnabled(true);
            PerfProbe.Section s = PerfProbe.section("s.test.on");
            for (int i = 0; i < 3; i++) {
                long t0 = PerfProbe.begin();
                assertTrue(t0 != 0L);
                PerfProbe.end(s, t0);
            }
            assertEquals(3L, s.row().calls());
            assertTrue(s.row().totalNanos() >= 0L);
        } finally {
            PerfProbe.setEnabled(was);
        }
    }

    @Test
    void switchingOnStartsClean() {
        boolean was = PerfProbe.enabled();
        try {
            PerfProbe.setEnabled(true);
            PerfProbe.Section s = PerfProbe.section("s.test.clean");
            PerfProbe.end(s, 1L);
            PerfProbe.setEnabled(false);
            assertTrue(s.row().calls() >= 1L);          // kept for the report while off
            PerfProbe.setEnabled(true);                 // off -> on: reset
            assertEquals(0L, s.row().calls());
        } finally {
            PerfProbe.setEnabled(was);
        }
    }

    @Test
    void rollingStatKeepsAWindowAndTheTotals() {
        RollingStat r = new RollingStat(4);
        for (int i = 1; i <= 8; i++) r.add(i * 100L);
        assertEquals(8L, r.count());
        assertEquals(3600L, r.totalNanos());
        assertEquals(800L, r.maxNanos());
        assertEquals(650.0, r.rollingAvgNanos());        // the last four: 500 600 700 800
        assertEquals(800L, r.rollingMaxNanos());
        assertEquals(450.0, r.avgNanos());
        r.reset();
        assertEquals(0L, r.count());
        assertEquals(0.0, r.rollingAvgNanos());
    }

    @Test
    void theMaximumInTheWindowForgetsAnOldSpike() {
        RollingStat r = new RollingStat(3);
        r.add(9000);
        r.add(1);
        r.add(2);
        r.add(3);
        assertEquals(9000L, r.maxNanos());
        assertEquals(3L, r.rollingMaxNanos());
    }

    @Test
    void aNegativeSampleIsCountedAsZero() {
        RollingStat r = new RollingStat(2);
        r.add(-50);
        assertEquals(1L, r.count());
        assertEquals(0L, r.totalNanos());
    }

    @Test
    void theReportIsOrderedByTotalTimeCappedAndFilteredBySide() {
        PerfProbe.Section a = PerfProbe.section("s.rep.a"), b = PerfProbe.section("s.rep.b"), c = PerfProbe.section("c.rep.c");
        PerfProbe.reset();
        a.add(100);
        b.add(300);
        b.add(300);
        c.add(9999);
        List<PerfProbe.Row> server = PerfProbe.top(10, "s.rep.");
        assertEquals(2, server.size());
        assertEquals("s.rep.b", server.get(0).name());
        assertEquals("s.rep.a", server.get(1).name());
        assertEquals(1, PerfProbe.top(1, "s.rep.").size());
        assertEquals("c.rep.c", PerfProbe.top(10, "c.rep.").get(0).name());
        List<String> lines = PerfProbe.report(10, "s.rep.");
        assertTrue(lines.get(0).startsWith("perf probe "));
        assertTrue(lines.size() == 3, lines.toString());
        assertTrue(lines.get(1).contains("s.rep.b"));
        PerfProbe.reset();
        assertEquals(0, PerfProbe.top(10, "s.rep.").size());
        assertFalse(PerfProbe.report(10, "s.rep.").isEmpty());
    }

    @Test
    void rowMsPerSecondIsTheShareOfWallClock() {
        PerfProbe.Row r = new PerfProbe.Row("x", 10, 2_000_000L, 200_000.0, 200_000.0, 300_000L, 300_000L);
        assertEquals(1.0, r.msPerSecond(2.0));
        assertEquals(0.0, r.msPerSecond(0.0));
    }
}
