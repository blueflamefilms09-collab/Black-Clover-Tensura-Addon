package com.newuniverse.nusmp.heroweapon.weapon.mirror;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the gleam feeds the Prism Ray, so its rules are proved exactly: the stored light grows, is capped, fades only after a quiet spell, and is spent whole. */
class GleamMeterTest {
    private static GleamMeter meter() { return new GleamMeter(100, 200, 10); }

    @Test
    void itGrowsByWhatIsAddedAndStopsAtTheCap() {
        GleamMeter g = meter();
        assertEquals(0, g.value(0));
        assertEquals(12, g.add(10, 12));
        assertEquals(24, g.add(11, 12));
        g.add(12, 500);
        assertEquals(100, g.value(12));
    }

    @Test
    void itDoesNotFadeForTheFirstTwoHundredTicksThenLosesAPointEveryTen() {
        GleamMeter g = meter();
        g.add(0, 50);
        assertEquals(50, g.value(200));
        assertEquals(50, g.value(209));
        assertEquals(49, g.value(210));
        assertEquals(40, g.value(300));
        assertEquals(0, g.value(10_000));
    }

    @Test
    void aNewGainStartsTheQuietSpellAgainFromWhatIsLeft() {
        GleamMeter g = meter();
        g.add(0, 50);
        assertEquals(40, g.value(300));
        assertEquals(52, g.add(300, 12), "faded to 40, plus 12");
        assertEquals(52, g.value(499));
        assertEquals(51, g.value(510));
    }

    @Test
    void spendingTakesEverythingAndReturnsIt() {
        GleamMeter g = meter();
        g.add(0, 37);
        assertEquals(37, g.spendAll(5));
        assertEquals(0, g.value(5));
        assertEquals(0, g.spendAll(6));
    }

    @Test
    void zeroAndNegativeGainsChangeNothing() {
        GleamMeter g = meter();
        g.add(0, 20);
        g.add(100, 0);
        g.add(100, -5);
        assertEquals(20, g.value(100));
        assertEquals(19, g.value(210), "the quiet spell still runs from the last real gain");
    }

    @Test
    void resetEmptiesIt() {
        GleamMeter g = meter();
        g.add(0, 80);
        g.reset();
        assertEquals(0, g.value(1));
    }
}
