package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.RuneCharge.Charge;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 3.6.0: the price follows the charge: a tap about 40 %, a full hold 100 %, never more than the page's normal price, the 3.4.0 cast and the setting off at 100 %. */
class RuneCostTest {
    private static final int MIN = RuneCharge.DEFAULT_MIN_HOLD, MAX = RuneCharge.DEFAULT_MAX_HOLD;
    private static final double D = RuneCost.DEFAULT_MIN;

    @Test
    void aTapPaysTheMinimumAndAFullHoldPaysEverything() {
        assertEquals(0.40, RuneCost.fraction(RuneCharge.of(0, MIN, MAX), D), 1e-9);
        assertEquals(1.0, RuneCost.fraction(RuneCharge.of(MAX, MIN, MAX), D), 1e-9);
        assertEquals(1.0, RuneCost.fraction(RuneCharge.of(RuneCharge.HOLD_CAP, MIN, MAX), D), 1e-9);
    }

    @Test
    void theChargedRangeRisesFromAboutAHalfToTheFullPrice() {
        double atMin = RuneCost.fraction(RuneCharge.of(MIN, MIN, MAX), D);
        assertTrue(atMin > 0.5 && atMin < 0.6, "the first charged tick is about 54 %: " + atMin);
        double mid = RuneCost.fraction(RuneCharge.of((MIN + MAX) / 2, MIN, MAX), D);
        assertTrue(mid > atMin && mid < 1.0);
    }

    @Test
    void thePriceNeverFallsWithALongerHoldAndNeverPassesTheNormalPrice() {
        double last = 0.0;
        for (int h = 0; h <= RuneCharge.HOLD_CAP; h++) {
            double f = RuneCost.fraction(RuneCharge.of(h, MIN, MAX), D);
            assertTrue(f >= last - 1e-12, "the price must not fall at " + h);
            assertTrue(f <= 1.0 && f >= D - 1e-12);
            last = f;
        }
    }

    @Test
    void theCastWithoutAMeasuredHoldPaysTheNormalPrice() {
        assertEquals(1.0, RuneCost.fraction(Charge.LEGACY, D), 0.0);
        assertEquals(1.0, RuneCost.fraction(null, D), 0.0);
    }

    @Test
    void theSettingOffGivesTheNormalPriceAndTheMinimumIsKeptInRange() {
        assertEquals(500.0, RuneCost.scaled(500.0, RuneCharge.of(0, MIN, MAX), D, false), 0.0);
        assertEquals(200.0, RuneCost.scaled(500.0, RuneCharge.of(0, MIN, MAX), D, true), 1e-9);
        assertEquals(RuneCost.MIN_LOW, RuneCost.minFraction(-3.0), 0.0);
        assertEquals(RuneCost.MIN_HIGH, RuneCost.minFraction(7.0), 0.0);
        assertEquals(RuneCost.DEFAULT_MIN, RuneCost.minFraction(Double.NaN), 0.0);
        assertEquals(1.0, RuneCost.fraction(RuneCharge.of(0, MIN, MAX), 1.0), 1e-9, "a minimum of 1 turns the scaling off");
    }

    @Test
    void aPriceOfNothingStaysNothingAndNeverGoesNegative() {
        assertEquals(0.0, RuneCost.scaled(0.0, RuneCharge.of(0, MIN, MAX), D, true), 0.0);
        assertEquals(0.0, RuneCost.scaled(-5.0, RuneCharge.of(0, MIN, MAX), D, true), 0.0);
        assertEquals(0.0, RuneCost.scaled(Double.NaN, RuneCharge.of(0, MIN, MAX), D, true), 0.0);
    }

    @Test
    void theLevelIsZeroForATapOneForAFullHoldAndInTheMiddleForTheOldSpell() {
        assertEquals(0.0, RuneCost.level(RuneCharge.of(0, MIN, MAX)), 1e-12);
        assertEquals(1.0, RuneCost.level(RuneCharge.of(MAX, MIN, MAX)), 1e-12);
        double old = RuneCost.level(Charge.LEGACY);
        assertTrue(old > 0.2 && old < 0.8, "the 3.4.0 spell sits in the middle: " + old);
        assertEquals(0.0, RuneCost.level(null), 0.0);
    }
}
