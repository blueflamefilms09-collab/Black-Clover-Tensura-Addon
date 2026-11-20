package com.newuniverse.nusmp.boss.god;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Leon study fix 5: the party table for every boss, under one combined cap with the Black Clover x3 and the floor scale. Lucius's own table is untouched. */
class PartyCapTest {
    @Test
    void theTableIsTheStatedOne() {
        double[] h = {0.70, 1.00, 1.15, 1.30}, d = {0.80, 1.00, 1.06, 1.12};
        for (int n = 1; n <= 4; n++) {
            assertEquals(h[n - 1], ArenaScaling.healthCombined(n, 1.0), 1e-12, n + " fighters, health");
            assertEquals(d[n - 1], ArenaScaling.damageCombined(n, 1.0), 1e-12, n + " fighters, damage");
        }
        assertEquals(1.30, ArenaScaling.healthCombined(9, 1.0), "more than four count as four");
        assertEquals(0.70, ArenaScaling.healthCombined(0, 1.0), "nobody counts as one");
    }

    @Test
    void lucius_oldMethodsAreKept() {
        assertEquals(0.30, ArenaScaling.healthModifier(4), 1e-12);
        assertEquals(1.12, ArenaScaling.damage(4));
    }

    @Test
    void aSmallPartyIsNeverRaisedByTheCap() {
        assertEquals(0.70, ArenaScaling.healthCombined(1, 7.5), "solo stays x0.70 on the deepest Black Clover boss");
        assertEquals(0.80, ArenaScaling.damageCombined(1, 4.8));
        assertEquals(1.00, ArenaScaling.healthCombined(2, 7.5));
    }

    @Test
    void theWholeStackNeverPassesTheStatedMaximum() {
        for (double others : new double[]{1.0, 1.5, 3.0, 4.0, 7.5, 8.0, 12.0}) {
            for (int n = 1; n <= 6; n++) {
                assertTrue(others * ArenaScaling.healthCombined(n, others) <= Math.max(ArenaScaling.MAX_TOTAL_HEALTH, others) + 1e-9, "health " + others + " x " + n);
                assertTrue(others * ArenaScaling.damageCombined(n, others) <= Math.max(ArenaScaling.MAX_TOTAL_DAMAGE, others) + 1e-9, "damage " + others + " x " + n);
                assertTrue(ArenaScaling.healthCombined(n, others) >= Math.min(1.0, ArenaScaling.health(n)), "never below the table's own lowering");
            }
        }
    }

    @Test
    void theBlackCloverTimesFloorCaseIsCutBack() {
        assertEquals(8.0 / 7.5, ArenaScaling.healthCombined(4, 3.0 * 2.5), 1e-12, "x3 on the deepest floor leaves a little room");
        assertEquals(5.0 / 4.8, ArenaScaling.damageCombined(4, 3.0 * 1.6), 1e-12);
        assertEquals(1.0, ArenaScaling.healthCombined(4, 9.0), "an already over-the-maximum stack gets no party raise");
        assertEquals(1.30, ArenaScaling.healthCombined(4, 3.0), 1e-12, "x3 alone leaves the full x1.30 (x3.9 in total)");
    }

    @Test
    void badInputIsSafe() {
        assertEquals(1.30, ArenaScaling.healthCombined(4, Double.NaN), 1e-12);
        assertEquals(1.30, ArenaScaling.healthCombined(4, 0.2), 1e-12, "a factor under 1 on the others counts as 1");
    }
}
