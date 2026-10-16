package com.newuniverse.nusmp.boss.god;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The player-count table for his health and true damage. */
class ArenaScalingTest {
    @Test
    void healthIsSeventyOneHundredOneFifteenOneThirty() {
        assertEquals(0.70, ArenaScaling.health(1));
        assertEquals(1.00, ArenaScaling.health(2));
        assertEquals(1.15, ArenaScaling.health(3));
        assertEquals(1.30, ArenaScaling.health(4));
        assertEquals(1.30, ArenaScaling.health(9), "four or more all count as four");
    }

    @Test
    void zeroOrNegativeCountsAsOnePlayer() {
        assertEquals(ArenaScaling.health(1), ArenaScaling.health(0));
        assertEquals(ArenaScaling.health(1), ArenaScaling.health(-3));
        assertEquals(ArenaScaling.damage(1), ArenaScaling.damage(0));
    }

    @Test
    void damageGrowsMoreSlowlyThanHealthAndNeverShrinksWithMorePlayers() {
        double last = 0;
        for (int n = 1; n <= 6; n++) {
            assertTrue(ArenaScaling.damage(n) >= last, "n=" + n);
            last = ArenaScaling.damage(n);
            assertTrue(ArenaScaling.damage(n) <= ArenaScaling.health(n) + 0.35, "n=" + n);
        }
        assertEquals(1.0, ArenaScaling.damage(2), "two players are the reference");
        assertTrue(ArenaScaling.damage(4) < ArenaScaling.health(4));
    }

    @Test
    void theModifierIsTheFactorMinusOne() {
        assertEquals(-0.30, ArenaScaling.healthModifier(1), 1e-9);
        assertEquals(0.0, ArenaScaling.healthModifier(2), 1e-9);
        assertEquals(0.30, ArenaScaling.healthModifier(5), 1e-9);
    }
}
