package com.newuniverse.nusmp.book;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The Spirit Dive upkeep: a creative player with an empty pool keeps the dive (it used to end on the first second), the others pay, and the reason is named. */
class SpiritDiveUpkeepTest {
    @Test
    void creativeWithEmptyPoolHolds() {
        assertEquals(SpiritDiveUpkeep.Verdict.HOLD, SpiritDiveUpkeep.verdict(true, true, 0.0, SpiritDiveUpkeep.cost(5000)));
    }

    @Test
    void survivalPaysOrEnds() {
        double c = SpiritDiveUpkeep.cost(5000);
        assertEquals(100.0, c, 1e-9);
        assertEquals(SpiritDiveUpkeep.Verdict.HOLD, SpiritDiveUpkeep.verdict(true, false, 100.0, c));
        assertEquals(SpiritDiveUpkeep.Verdict.NO_MAGICULE, SpiritDiveUpkeep.verdict(true, false, 99.9, c));
    }

    @Test
    void closedGateEndsEvenInCreative() {
        assertEquals(SpiritDiveUpkeep.Verdict.GATE_CLOSED, SpiritDiveUpkeep.verdict(false, true, 1e9, 1.0));
        assertEquals(true, SpiritDiveUpkeep.Verdict.GATE_CLOSED.reason().contains("gate"));
        assertEquals(true, SpiritDiveUpkeep.Verdict.NO_MAGICULE.reason().contains("magicule"));
    }
}
