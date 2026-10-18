package com.newuniverse.nusmp.heroweapon.weapon.bow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: "the draw time sets the power" is the Bow's one rule of feel. The curve is the vanilla bow's (so it feels like a bow), a tap must not shoot, a draw
 * past full must not shoot harder than full, and no bonus (strength, Law Fanatic, Pride) may lift a plain arrow above the ordinary budget.
 */
class DrawMathTest {
    @Test
    void theCurveIsTheVanillaBowsAndStopsAtOne() {
        assertEquals(0.0, DrawMath.fraction(0, false), 1e-9);
        assertEquals((0.25 + 1.0) / 3.0, DrawMath.fraction(10, false), 1e-9, "half a draw is 0.4167, not 0.5");
        assertEquals(1.0, DrawMath.fraction(20, false), 1e-9);
        assertEquals(1.0, DrawMath.fraction(200, false), 1e-9, "never more than full");
        assertEquals(0.0, DrawMath.fraction(-5, false), 1e-9);
        double last = -1;
        for (int t = 0; t <= 30; t++) {
            double f = DrawMath.fraction(t, false);
            assertTrue(f >= last, "the curve never falls");
            last = f;
        }
    }

    @Test
    void quickdrawReachesFullEarlier() {
        assertEquals(1.0, DrawMath.fraction(16, true), 1e-9, "16 ticks is a full draw with Quickdraw");
        assertTrue(DrawMath.fraction(15, true) < 1.0);
        assertTrue(DrawMath.fraction(10, true) > DrawMath.fraction(10, false));
    }

    @Test
    void aTapDoesNotShootButAShortDrawDoes() {
        assertFalse(DrawMath.enough(DrawMath.fraction(2, false)), "2 ticks is a tap");
        assertTrue(DrawMath.enough(DrawMath.fraction(3, false)), "3 ticks is the shortest shot (as with the vanilla bow)");
        assertTrue(DrawMath.enough(0.1));
        assertFalse(DrawMath.enough(0.09));
    }

    @Test
    void rangeGrowsWithTheDrawAndFarsightAddsButNeverPastTheSniper() {
        assertEquals(16.0, DrawMath.range(0.0, false), 1e-9);
        assertEquals(48.0, DrawMath.range(1.0, false), 1e-9);
        assertEquals(32.0, DrawMath.range(0.5, false), 1e-9);
        assertEquals(48.0 * 1.3, DrawMath.range(1.0, true), 1e-9);
        assertTrue(DrawMath.range(1.0, true) <= BowNumbers.SNIPER_RANGE);
        assertEquals(48.0, DrawMath.range(5.0, false), 1e-9, "a fraction above 1 is clamped");
    }

    @Test
    void costIsProportionalToTheDraw() {
        assertEquals(BowNumbers.BASIC_COST, DrawMath.cost(1.0), 1e-12);
        assertEquals(BowNumbers.BASIC_COST / 2, DrawMath.cost(0.5), 1e-12);
        assertEquals(0.0, DrawMath.cost(-1.0), 1e-12);
        assertEquals(BowNumbers.BASIC_COST, DrawMath.cost(9.0), 1e-12);
    }

    @Test
    void rawDamageFollowsTheDrawAndTheStrengthAndNeverBreaksTheBudget() {
        assertEquals(7.0, DrawMath.raw(1.0, 0, 1.0, 1.0), 1e-9);
        assertEquals(3.5, DrawMath.raw(0.5, 0, 1.0, 1.0), 1e-9);
        assertEquals(7.0 * 1.4, DrawMath.raw(1.0, 10, 1.0, 1.0), 1e-9, "ten points of strength is +40%");
        assertEquals(12.0, DrawMath.raw(1.0, 10, 1.15, 1.2), 1e-9, "7 x 1.4 x 1.15 x 1.2 = 13.5 is cut to the ordinary budget");
        assertEquals(7.0 * 1.15, DrawMath.raw(1.0, 0, 1.15, 1.0), 1e-9);
        assertEquals(7.0, DrawMath.raw(1.0, -4, 0.5, 0.5), 1e-9, "a bonus below 1 or a negative strength never weakens a plain arrow");
        for (double f = 0.0; f <= 1.0; f += 0.1)
            for (int stat = 0; stat <= 14; stat += 2)
                assertTrue(DrawMath.raw(f, stat, BowNumbers.LAW_BONUS, BowNumbers.PRIDE_BONUS) <= BowNumbers.BASIC_MAX_RAW + 1e-9);
    }
}
