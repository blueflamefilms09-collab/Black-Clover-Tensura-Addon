package com.newuniverse.nusmp.heroweapon.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: the kill experience (a quarter of the victim's health, five times for a boss, capped), the shock (never lethal) and the spacing rule (never spammed) are
 * small formulas whose edges are the ones a player would hit first: a boss with a huge health pool, a mob with almost no health, a shock at half a heart, a
 * message due again.
 */
class KillXpShockTest {
    @Test
    void aQuarterOfTheVictimsHealthAndFiveTimesThatForABoss() {
        assertEquals(5, KillXp.forKill(20, false, 0.25, 5.0, 500));
        assertEquals(25, KillXp.forKill(20, true, 0.25, 5.0, 500));
        assertEquals(2, KillXp.forKill(6, false, 0.25, 5.0, 500), "6 hp is 1.5 points, rounded to the nearest");
    }

    @Test
    void theCapStopsABossFromTakingAWeaponUpInOneBlow() {
        assertEquals(500, KillXp.forKill(100000, true, 0.25, 5.0, 500));
        assertEquals(500, KillXp.forKill(Double.POSITIVE_INFINITY, false, 0.25, 5.0, 500));
        assertEquals(10, KillXp.forKill(40, false, 0.25, 5.0, 10));
    }

    @Test
    void nothingForNothing() {
        assertEquals(0, KillXp.forKill(0, false, 0.25, 5.0, 500));
        assertEquals(0, KillXp.forKill(-4, true, 0.25, 5.0, 500));
        assertEquals(0, KillXp.forKill(Double.NaN, false, 0.25, 5.0, 500));
        assertEquals(0, KillXp.forKill(20, false, 0.0, 5.0, 500), "a zero share switches kill experience off");
        assertEquals(0, KillXp.forKill(20, false, 0.25, 5.0, 0), "a zero cap too");
    }

    @Test
    void aBossFactorBelowOneIsNotAPenalty() {
        assertEquals(5, KillXp.forKill(20, true, 0.25, 0.1, 500));
    }

    @Test
    void theShockNeverKills() {
        assertEquals(4.0f, ShockRules.damage(4.0f, 20.0f), 1e-6f);
        assertEquals(3.0f, ShockRules.damage(4.0f, 4.0f), 1e-6f, "at 4 health the shock leaves 1");
        assertEquals(0.5f, ShockRules.damage(4.0f, 1.5f), 1e-6f);
        assertEquals(0.0f, ShockRules.damage(4.0f, 1.0f), 1e-6f, "at health 1 the shock does nothing");
        assertEquals(0.0f, ShockRules.damage(4.0f, 0.2f), 1e-6f);
        assertEquals(0.0f, ShockRules.damage(-3.0f, 20.0f), 1e-6f, "a negative setting is no damage");
        for (float health = 0.1f; health <= 40f; health += 0.37f) {
            assertTrue(health - ShockRules.damage(4.0f, health) >= Math.min(1.0f, health) - 1e-4f, "health " + health);
        }
    }

    @Test
    void aRepeatingThingIsDueAgainAfterItsGap() {
        assertTrue(Spacing.due(100, Spacing.LONG_AGO, 60), "never happened: due");
        assertFalse(Spacing.due(100, 80, 60), "20 ticks ago, gap 60");
        assertTrue(Spacing.due(140, 80, 60), "exactly the gap");
        assertTrue(Spacing.due(10, 5000, 60), "the clock was set back: due");
        assertTrue(Spacing.due(5, 5, 0), "no gap: always due");
        assertTrue(Spacing.due(Long.MAX_VALUE / 2, Spacing.LONG_AGO, 1_000_000), "a huge clock and a never-recorded slot do not overflow");
    }
}
