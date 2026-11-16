package com.newuniverse.nusmp.balance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EpMasteryTest {
    @Test
    void noMasteryIsX1AndFullMasteryIsTheWholeScale() {
        assertEquals(1.0, EpMastery.ramp(6.0, 0.0, true), 1e-9);
        assertEquals(6.0, EpMastery.ramp(6.0, 1.0, true), 1e-9);
        assertEquals(3.5, EpMastery.ramp(6.0, 0.5, true), 1e-9);
    }

    @Test
    void switchOffOrNoScaleChangesNothing() {
        assertEquals(6.0, EpMastery.ramp(6.0, 0.0, false), 1e-9);
        assertEquals(1.0, EpMastery.ramp(1.0, 0.3, true), 1e-9);
    }

    @Test
    void badMasteryIsClamped() {
        assertEquals(1.0, EpMastery.ramp(4.0, Double.NaN, true), 1e-9);
        assertEquals(4.0, EpMastery.ramp(4.0, 9.0, true), 1e-9);
        assertEquals(1.0, EpMastery.ramp(4.0, -2.0, true), 1e-9);
    }
}
