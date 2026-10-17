package com.newuniverse.nusmp.arc.spade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The burst rule shared by Re-Structure and the Specimen thread: a share of his health inside a window, not chip damage. */
class BurstWatchTest {
    @Test
    void trippedOnlyWhenTheLimitIsReachedInsideTheWindow() {
        BurstWatch w = new BurstWatch();
        w.begin(100, 24, 150);
        assertTrue(w.open(100));
        w.add(105, 60);
        w.add(110, 60);
        assertFalse(w.tripped(), "120 of 150");
        assertEquals(120.0, w.damage(), 1e-9);
        w.add(120, 40);
        assertTrue(w.tripped());
        assertFalse(w.open(121), "a tripped watch is no longer open");
    }

    @Test
    void chipDamageSpreadOverTimeNeverTrips() {
        BurstWatch w = new BurstWatch();
        w.begin(0, 24, 100);
        for (int t = 0; t < 24; t += 4) w.add(t, 10);
        assertFalse(w.tripped(), "60 damage inside the window");
        w.add(24, 500);
        assertFalse(w.tripped(), "the window ended at tick 24: later damage does not count");
        assertFalse(w.open(24));
    }

    @Test
    void notArmedMeansNothingCounts() {
        BurstWatch w = new BurstWatch();
        w.add(5, 1000);
        assertFalse(w.tripped());
        assertEquals(0.0, w.damage(), 1e-9);
        w.begin(0, 10, 50);
        w.clear();
        w.add(1, 1000);
        assertFalse(w.tripped());
        assertFalse(w.open(1));
    }

    @Test
    void beginRearmsCleanly() {
        BurstWatch w = new BurstWatch();
        w.begin(0, 10, 50);
        w.add(1, 80);
        assertTrue(w.tripped());
        w.begin(200, 10, 50);
        assertFalse(w.tripped());
        assertEquals(0.0, w.damage(), 1e-9);
        w.add(201, -4);
        w.add(201, 0);
        assertEquals(0.0, w.damage(), 1e-9, "damage that is not positive is ignored");
    }
}
