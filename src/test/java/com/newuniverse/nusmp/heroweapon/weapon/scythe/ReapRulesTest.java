package com.newuniverse.nusmp.heroweapon.weapon.scythe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: these rules decide how hard the Scythe hits and how much it heals, and the owner bounds both, so each is pinned with numbers: the reap bonus belongs only
 * to a weak mob (never a player, never a boss, never a corpse), the armour bonus and the soul bonus stop at their caps, nothing passes the ultimate limit of 32,
 * and no heal passes a quarter of the wielder's health.
 */
class ReapRulesTest {
    @Test
    void onlyAWeakNonBossMobCanBeReaped() {
        assertTrue(ReapRules.isWeak(6.0, 20.0, false, false), "exactly 30 percent is weak");
        assertTrue(ReapRules.isWeak(1.0, 20.0, false, false));
        assertFalse(ReapRules.isWeak(6.2, 20.0, false, false), "31 percent is not");
        assertFalse(ReapRules.isWeak(1.0, 20.0, true, false), "a player is never reaped");
        assertFalse(ReapRules.isWeak(1.0, 500.0, false, true), "a boss is never reaped");
        assertFalse(ReapRules.isWeak(0.0, 20.0, false, false), "a dead foe is not weak");
        assertFalse(ReapRules.isWeak(5.0, 0.0, false, false), "a foe with no maximum is not weak");
    }

    @Test
    void theReapDoublesOnTheWeak() {
        assertEquals(10.0, ReapRules.reapRaw(false), 1e-9);
        assertEquals(20.0, ReapRules.reapRaw(true), 1e-9);
    }

    @Test
    void theProtectionBreakGrowsWithArmourUpToItsCap() {
        assertEquals(9.0, ReapRules.breakRaw(0.0), 1e-9);
        assertEquals(15.0, ReapRules.breakRaw(10.0), 1e-9);
        assertEquals(17.0, ReapRules.breakRaw(13.4), 1e-9, "13.4 armour reaches the cap of 8 more");
        assertEquals(17.0, ReapRules.breakRaw(500.0), 1e-9);
        assertEquals(9.0, ReapRules.breakRaw(-5.0), 1e-9);
    }

    @Test
    void theHarvestGrowsWithSoulsAndNeverPassesThirtyTwo() {
        assertEquals(16.0, ReapRules.harvestRaw(0, false, 10), 1e-9);
        assertEquals(26.0, ReapRules.harvestRaw(10, false, 10), 1e-9);
        assertEquals(26.0, ReapRules.harvestRaw(40, false, 10), 1e-9, "souls beyond the cap are not counted");
        assertEquals(30.0, ReapRules.harvestRaw(14, false, 14), 1e-9, "Dragon Soul holds 14");
        assertEquals(19.2, ReapRules.harvestRaw(0, true, 10), 1e-9, "20 percent more on the weak");
        assertEquals(31.2, ReapRules.harvestRaw(10, true, 10), 1e-9);
        assertEquals(32.0, ReapRules.harvestRaw(14, true, 14), 1e-9, "36 would pass the ultimate limit and is cut to 32");
        assertEquals(16.0, ReapRules.harvestRaw(-3, false, 10), 1e-9);
        for (int s = -2; s < 30; s++) {
            assertTrue(ReapRules.harvestRaw(s, true, ScytheNumbers.SOUL_CAP_DRAGON) <= 32.0 + 1e-9);
        }
    }

    @Test
    void healsAreSmallSharesAndNeverPassAQuarter() {
        assertEquals(0.6, ReapRules.reapHeal(1, 20.0), 1e-9, "3 percent of 20");
        assertEquals(2.4, ReapRules.reapHeal(4, 20.0), 1e-9, "12 percent of 20");
        assertEquals(2.4, ReapRules.reapHeal(6, 20.0), 1e-9, "capped at 12 percent");
        assertEquals(0.0, ReapRules.reapHeal(0, 20.0), 1e-9);
        assertEquals(0.0, ReapRules.reapHeal(-2, 20.0), 1e-9);
        assertEquals(0.4, ReapRules.harvestHeal(1, 20.0), 1e-9, "2 percent of 20");
        assertEquals(4.0, ReapRules.harvestHeal(10, 20.0), 1e-9, "20 percent of 20");
        assertEquals(4.0, ReapRules.harvestHeal(12, 20.0), 1e-9, "capped at 20 percent");
        for (int k = 0; k <= 12; k++) {
            assertTrue(ReapRules.reapHeal(k, 100.0) <= 25.0 + 1e-9);
            assertTrue(ReapRules.harvestHeal(k, 100.0) <= 25.0 + 1e-9);
        }
    }
}
