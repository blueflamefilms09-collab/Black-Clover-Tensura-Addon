package com.newuniverse.nusmp.arc.early;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Vetto's aura tears when a burst lands inside three seconds, so the sliding window of recent damage has to forget exactly on time. */
class BurstMeterTest {
    @Test
    void onlyTheWindowCounts() {
        BurstMeter m = new BurstMeter(60);
        m.record(0, 30);
        m.record(30, 40);
        assertEquals(70.0, m.total(30), 1e-9);
        assertEquals(70.0, m.total(59), 1e-9);
        assertEquals(40.0, m.total(60), 1e-9, "the first blow has aged out at exactly the window");
        assertEquals(0.0, m.total(90), 1e-9);
    }

    @Test
    void aTrickleNeverAddsUpToABurst() {
        BurstMeter m = new BurstMeter(60);
        for (int t = 0; t < 1000; t += 20) m.record(t, 10);
        assertTrue(m.total(980) <= 30.0 + 1e-9, "three blows at most live in a window: " + m.total(980));
    }

    @Test
    void blowsOfOneTickMergeAndNothingIsNegative() {
        BurstMeter m = new BurstMeter(10);
        m.record(5, 3);
        m.record(5, 4);
        m.record(5, -9);
        m.record(5, 0);
        assertEquals(7.0, m.total(5), 1e-9);
        m.clear();
        assertEquals(0.0, m.total(5), 1e-9);
    }
}
