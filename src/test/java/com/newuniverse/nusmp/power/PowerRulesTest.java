package com.newuniverse.nusmp.power;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PowerRulesTest {
    @Test void theLargestFactorWinsAndNothingMultiplies() {
        assertEquals(25.0, PowerRules.factor(25, 25, 25), 1e-9);
        assertEquals(40.0, PowerRules.factor(1, 40, 25), 1e-9);
        assertEquals(1.0, PowerRules.factor(1, 1, 1), 1e-9);
        assertEquals(1.0, PowerRules.factor(-5, Double.NaN, 0.2), 1e-9);
        assertEquals(1000.0, PowerRules.factor(1e9, 1, 1), 1e-9);
    }

    @Test void mobsTakeItWholeAndPlayersAreHeldAtTheCap() {
        assertEquals(250f, PowerRules.dealt(10f, 25, false, 20f, 0.4), 1e-4);
        assertEquals(8f, PowerRules.dealt(2f, 25, true, 20f, 0.4), 1e-4);       // 50 would kill; the cap is 40 % of 20
        assertEquals(10f, PowerRules.dealt(10f, 25, true, 20f, 0.4), 1e-4);     // already over the cap: left alone, never lowered
        assertEquals(10f, PowerRules.dealt(10f, 1, false, 20f, 0.4), 1e-4);
        assertEquals(0f, PowerRules.dealt(0f, 25, false, 20f, 0.4), 1e-4);
    }

    @Test void whatAWielderTakesNeverGrows() {
        assertEquals(5f, PowerRules.taken(10f, 0.5), 1e-4);
        assertEquals(10f, PowerRules.taken(10f, 3.0), 1e-4);
        assertEquals(0f, PowerRules.taken(10f, -1.0), 1e-4);
    }
}
