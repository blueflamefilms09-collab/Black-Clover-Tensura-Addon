package com.newuniverse.nusmp.spiritcontrol;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the throttles are what keep combat mode cheap on a public server, so they are pinned: the server ceiling (at most N casts in any span of a window, however they are timed), the per-player ask gap (per
 * UUID, never a shared "current player", bounded in size), and the per-tick decision budget.
 */
class SpiritThrottleTest {
    @Test
    void theRateWindowNeverLetsMoreThanMaxThroughInAnyWindow() {
        RateWindow w = new RateWindow(5, 20);
        int ok = 0;
        for (long t = 0; t < 20; t++) if (w.tryAcquire(t)) ok++;
        assertEquals(5, ok, "five casts in the first second, whatever the timing");
        assertEquals(5, w.used(19));
        assertFalse(w.tryAcquire(19));
        assertTrue(w.tryAcquire(20), "the first one has left the window");
        // a worst case sweep: over a long run no window of 20 ticks holds more than 5
        RateWindow x = new RateWindow(5, 20);
        long[] at = new long[400];
        int n = 0;
        for (long t = 0; t < 400; t++) if (x.tryAcquire(t)) at[n++] = t;
        for (int i = 0; i + 5 < n; i++) assertTrue(at[i + 5] - at[i] >= 20, "six casts inside one second at " + at[i]);
    }

    @Test
    void aWindowOfOneIsAGap() {
        RateWindow gap = new RateWindow(1, 30);
        assertTrue(gap.tryAcquire(100));
        assertFalse(gap.tryAcquire(129));
        assertTrue(gap.tryAcquire(130));
        assertEquals(1, gap.max());
        assertEquals(30, gap.windowTicks());
    }

    @Test
    void theAskGapIsPerPlayer() {
        PlayerGap g = new PlayerGap();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertTrue(g.allow(a, 100, 10));
        assertFalse(g.allow(a, 105, 10), "a second ask inside the gap");
        assertTrue(g.allow(b, 105, 10), "another player is not held up by a");
        assertTrue(g.allow(a, 110, 10));
        g.forget(a);
        assertTrue(g.allow(a, 111, 10), "forgotten at logout");
        assertFalse(g.allow(null, 0, 10));
    }

    @Test
    void theAskMapStaysBounded() {
        PlayerGap g = new PlayerGap();
        for (int i = 0; i < PlayerGap.MAX_ENTRIES * 4; i++) g.allow(UUID.randomUUID(), i, 10);
        assertTrue(g.size() <= PlayerGap.MAX_ENTRIES);
        g.clear();
        assertEquals(0, g.size());
    }

    @Test
    void theTickBudgetResetsEveryTick() {
        TickBudget b = new TickBudget();
        int taken = 0;
        for (int i = 0; i < 50; i++) if (b.take(7, 8)) taken++;
        assertEquals(8, taken);
        assertEquals(8, b.used(7));
        assertTrue(b.take(8, 8), "a new tick, a new budget");
        assertEquals(1, b.used(8));
        assertEquals(0, b.used(9));
    }
}
