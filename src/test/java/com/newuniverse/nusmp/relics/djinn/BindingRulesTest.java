package com.newuniverse.nusmp.relics.djinn;

import org.junit.jupiter.api.Test;

import static com.newuniverse.nusmp.relics.djinn.BindingRules.*;
import static org.junit.jupiter.api.Assertions.*;

/** The binding ritual: the price, the trial, the checklist order. */
class BindingRulesTest {
    private static Inputs ok() { return new Inputs(true, false, true, false, true, true, true, 1.0, 3, false); }

    @Test
    void theEpCostIsAQuarterButNeverBelowTheFloor() {
        assertEquals(25.0, epCost(100, 100, EP_FRACTION), 1e-9);
        assertEquals(12.5, epCost(100, 100, EP_FRACTION / 2), 1e-9, "a failed binding costs half");
        assertEquals(3.75, epCost(15, 100, EP_FRACTION), 1e-9, "a quarter of what is left");
        assertEquals(2.0, epCost(12, 100, EP_FRACTION), 1e-9, "only what keeps 10% of the maximum");
        assertEquals(0.0, epCost(10, 100, EP_FRACTION), 1e-9);
        assertEquals(0.0, epCost(5, 100, EP_FRACTION), 1e-9);
        assertEquals(0.0, epCost(0, 100, EP_FRACTION));
        assertEquals(0.0, epCost(50, 0, EP_FRACTION));
        for (double cur = 0; cur <= 100; cur += 3.7) assertTrue(cur - epCost(cur, 100, EP_FRACTION) >= Math.min(cur, 10) - 1e-9);
    }

    @Test
    void theTrialIsBetweenFiveAndThirtyPercent() {
        assertEquals(0.05, failChance(0, false), 1e-9);
        assertEquals(0.275, failChance(9, false), 1e-9);
        assertEquals(0.05, failChance(9, true), 1e-9, "Arcane and Saint stages are trusted");
        double prev = 0;
        for (int s = 0; s <= 9; s++) {
            double c = failChance(s, false);
            assertTrue(c >= prev && c >= MIN_FAIL && c <= MAX_FAIL);
            prev = c;
        }
        assertEquals(failChance(9, false), failChance(40, false), "stage numbers are clamped");
        assertTrue(fails(0.3, 0.29));
        assertFalse(fails(0.3, 0.30));
        assertEquals(1, failedCrystals());
    }

    @Test
    void theChecklistRefusesInOrderAndPaysNothing() {
        assertEquals(Refusal.NONE, check(ok()));
        assertEquals(Refusal.ALREADY_OWNED, check(new Inputs(false, true, false, true, false, false, false, 0, 0, false)));
        assertEquals(Refusal.ALREADY_HOLDS_DJINN, check(new Inputs(true, true, false, true, false, false, false, 0, 0, false)));
        assertEquals(Refusal.HOUSEHOLD, check(new Inputs(true, false, false, true, false, false, false, 0, 0, false)));
        assertEquals(Refusal.EXCLUSIVE_BOND, check(new Inputs(true, false, false, false, false, false, false, 0, 0, false)));
        assertEquals(Refusal.NO_VESSEL, check(new Inputs(true, false, true, false, false, false, false, 0, 0, false)));
        assertEquals(Refusal.VESSEL_TAKEN, check(new Inputs(true, false, true, false, true, false, false, 0, 0, false)));
        assertEquals(Refusal.NOT_ATTUNED, check(new Inputs(true, false, true, false, true, true, false, 0, 0, false)));
        assertEquals(Refusal.BAR_NOT_FULL, check(new Inputs(true, false, true, false, true, true, true, 0.98, 3, false)));
        assertEquals(Refusal.NO_CRYSTALS, check(new Inputs(true, false, true, false, true, true, true, 1.0, 2, false)));
    }

    @Test
    void creativePlayersSkipOnlyTheBarAndTheCrystals() {
        assertEquals(Refusal.NONE, check(new Inputs(true, false, true, false, true, true, true, 0.0, 0, true)));
        assertEquals(Refusal.NO_VESSEL, check(new Inputs(true, false, true, false, false, true, true, 1.0, 3, true)));
        assertEquals(Refusal.ALREADY_OWNED, check(new Inputs(false, false, true, false, true, true, true, 1.0, 3, true)));
    }
}
