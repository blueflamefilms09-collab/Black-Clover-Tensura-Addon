package com.newuniverse.nusmp.boss.arc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The hover: bounded speeds, off when there is no ground, steady at the height. */
class HoverMathTest {
    @Test
    void risesWhenLowSinksWhenHighAndRestsAtTheHeight() {
        assertTrue(HoverMath.verticalSpeed(0.5) > 0);
        assertTrue(HoverMath.verticalSpeed(6.0) < 0);
        assertEquals(0.0, HoverMath.verticalSpeed(HoverMath.HEIGHT), 1e-9);
    }

    @Test
    void speedsAreBounded() {
        assertEquals(0.14, HoverMath.verticalSpeed(0.0), 1e-9, "rises at most 0.14 a tick");
        assertEquals(-0.20, HoverMath.verticalSpeed(HoverMath.PROBE), 1e-9, "sinks at most 0.20 a tick");
        assertEquals(3.0, HoverMath.HEIGHT, 0.0, "three blocks above the ground");
        for (double d = 0; d <= HoverMath.PROBE; d += 0.25) {
            double v = HoverMath.verticalSpeed(d);
            assertTrue(v <= HoverMath.MAX_RISE + 1e-9 && v >= -HoverMath.MAX_SINK - 1e-9);
        }
    }

    @Test
    void noGroundMeansNoHover() {
        assertFalse(HoverMath.canHover(Double.NaN));
        assertFalse(HoverMath.canHover(HoverMath.PROBE + 0.1));
        assertEquals(0.0, HoverMath.verticalSpeed(Double.NaN));
        assertEquals(0.0, HoverMath.verticalSpeed(50.0));
        assertTrue(HoverMath.canHover(0.0));
    }

    @Test
    void driftKeepsHimInTheBand() {
        assertTrue(HoverMath.drift(4.0) < 0);
        assertTrue(HoverMath.drift(30.0) > 0);
        assertEquals(0.0, HoverMath.drift(12.0));
        assertEquals(0.0, HoverMath.drift(Double.NaN));
        assertTrue(Math.abs(HoverMath.drift(0.0)) <= HoverMath.MAX_DRIFT);
        assertTrue(Math.abs(HoverMath.drift(500.0)) <= HoverMath.MAX_DRIFT);
    }
}
