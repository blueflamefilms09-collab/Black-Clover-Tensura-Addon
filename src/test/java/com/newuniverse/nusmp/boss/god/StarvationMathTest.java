package com.newuniverse.nusmp.boss.god;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Magicule Starvation: a percent a second, pulses, and a floor that keeps it from ever being a way to kill. */
class StarvationMathTest {
    @Test
    void onePercentASecondIsAFiftiethOfAPercentAPerTick() {
        assertEquals(0.5, StarvationMath.perTick(1000.0, 1.0), 1e-9);
        assertEquals(10.0, StarvationMath.perTick(1000.0, 1.0) * 20, 1e-9);
    }

    @Test
    void aZeroOrNegativeRateDrainsNothing() {
        assertEquals(0.0, StarvationMath.perTick(1000.0, 0.0));
        assertEquals(0.0, StarvationMath.perTick(1000.0, -3.0));
        assertEquals(0.0, StarvationMath.perTick(0.0, 1.0));
        assertEquals(0.0, StarvationMath.pulse(1000.0, 0.0));
    }

    @Test
    void aPulseIsFivePercentOfTheMaximum() {
        assertEquals(50.0, StarvationMath.pulse(1000.0, 5.0), 1e-9);
    }

    @Test
    void theDrainLowersThePool() {
        assertEquals(940.0, StarvationMath.apply(1000.0, 1000.0, 60.0), 1e-9);
    }

    @Test
    void theDrainStopsAtTheFloorAboveTheBlackOutLine() {
        double floor = 1000.0 * StarvationMath.FLOOR_FRACTION;
        assertEquals(floor, StarvationMath.apply(100.0, 1000.0, 5000.0), 1e-9);
        assertTrue(floor > 1000.0 * 0.005, "above the mana law's 0.5% collapse line");
        double p = 1000.0;
        for (int i = 0; i < 100000; i++) p = StarvationMath.apply(p, 1000.0, StarvationMath.perTick(1000.0, 1.0));
        assertEquals(floor, p, 1e-9);
        assertTrue(p >= 0.0);
    }

    @Test
    void aPoolAlreadyUnderTheFloorIsNeverRaisedNorLowered() {
        assertEquals(3.0, StarvationMath.apply(3.0, 1000.0, 50.0));
        assertEquals(0.0, StarvationMath.apply(0.0, 1000.0, 50.0));
    }

    @Test
    void garbageIsLeftAlone() {
        assertEquals(500.0, StarvationMath.apply(500.0, 0.0, 10.0));
        assertEquals(500.0, StarvationMath.apply(500.0, 1000.0, -10.0));
        assertTrue(Double.isNaN(StarvationMath.apply(Double.NaN, 1000.0, 10.0)));
    }
}
