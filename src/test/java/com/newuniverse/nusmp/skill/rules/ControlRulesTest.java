package com.newuniverse.nusmp.skill.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: one rule shortens every hold on a boss or a player (3.47.0); it must never lengthen one, never go past the cap and never turn a hold into nothing. */
class ControlRulesTest {
    @Test
    void bossesAndPlayersAreHeldShorter() {
        assertEquals(100, ControlRules.controlTicks(100, false, false));
        assertEquals(50, ControlRules.controlTicks(100, false, true));
        assertEquals(40, ControlRules.controlTicks(100, true, false));
        assertEquals(40, ControlRules.controlTicks(100, true, true), "a boss counts as a boss before it counts as a player");
    }

    @Test
    void neverNegativeNeverPastTheCapNeverZeroForARealHold() {
        assertEquals(0, ControlRules.controlTicks(0, false, false));
        assertEquals(0, ControlRules.controlTicks(-10, true, true));
        assertEquals(1, ControlRules.controlTicks(1, true, false), "a hold of a tick still holds a tick");
        assertEquals(ControlRules.MAX_HOLD, ControlRules.controlTicks(100000, false, false));
        for (int base = 1; base <= 1000; base += 7) {
            for (int mask = 0; mask < 4; mask++) {
                int t = ControlRules.controlTicks(base, (mask & 1) != 0, (mask & 2) != 0);
                assertTrue(t >= 1 && t <= ControlRules.MAX_HOLD && t <= base, "base " + base + " mask " + mask + " -> " + t);
            }
        }
    }
}
