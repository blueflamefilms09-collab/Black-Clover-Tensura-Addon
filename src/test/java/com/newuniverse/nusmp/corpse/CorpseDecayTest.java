package com.newuniverse.nusmp.corpse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Decay stage math and the bleed: stages 0..3 by quarter, health only goes down. */
class CorpseDecayTest {
    @Test
    void stagesByQuarter() {
        assertEquals(0, CorpseDecay.stage(0.0));
        assertEquals(0, CorpseDecay.stage(0.24));
        assertEquals(1, CorpseDecay.stage(0.25));
        assertEquals(2, CorpseDecay.stage(0.5));
        assertEquals(3, CorpseDecay.stage(0.75));
        assertEquals(3, CorpseDecay.stage(1.0));
        assertEquals(3, CorpseDecay.stage(7.0));
        assertEquals(0, CorpseDecay.stage(-1.0));
    }

    @Test
    void fractionFromTheClock() {
        assertEquals(0.5, CorpseDecay.fraction(100, 700, 1200), 1e-9);
        assertEquals(0.0, CorpseDecay.fraction(100, 50, 1200), 1e-9);
        assertEquals(1.0, CorpseDecay.fraction(0, 5000, 1200), 1e-9);
        assertEquals(2, CorpseDecay.stage(0, 600, 1200));
    }

    @Test
    void bleedNeverHeals() {
        double hp = 40;
        for (int s = 0; s < 200; s++) {
            double next = CorpseDecay.afterBleed(hp, 40, 1200);
            assertTrue(next <= hp);
            hp = next;
        }
        assertEquals(0.5, hp, 1e-9, "the clock ends the body, not the bleed");
        double mid = 40;
        for (int s = 0; s < 60; s++) mid = CorpseDecay.afterBleed(mid, 40, 1200);
        assertEquals(16.0, mid, 1e-6, "60 percent is gone after the full life");
        assertEquals(0.5, CorpseDecay.afterBleed(0.5, 40, 1200), 1e-9);
        assertEquals(0.3, CorpseDecay.afterBleed(0.3, 40, 1200), 1e-9, "below the floor it is left alone, never raised");
    }

    @Test
    void sixtyPercentGoneAtTheEnd() {
        assertEquals(24.0, CorpseDecay.bleedPerSecond(40, 1200) * 60, 1e-9);
        assertEquals(0.0, CorpseDecay.bleedPerSecond(0, 1200));
    }

    @Test
    void amplifierClampsAndAlphaGrows() {
        assertEquals(3, CorpseDecay.amplifier(99));
        assertEquals(0, CorpseDecay.stageOf(-4));
        for (int s = 0; s < 3; s++) assertTrue(CorpseDecay.overlayAlpha(s) < CorpseDecay.overlayAlpha(s + 1));
        assertEquals(1.3, CorpseDecay.DAMAGE_MULT, 1e-9);
    }
}
