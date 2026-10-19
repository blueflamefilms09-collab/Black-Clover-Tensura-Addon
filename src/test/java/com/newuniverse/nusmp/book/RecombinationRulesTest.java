package com.newuniverse.nusmp.book;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the four pages Recombination Magic had keep their ids and order (saves and mastery bits), the four new ones follow; the Black Bull's numbers (scale, Ultra form, cannons, armour cut, heal,
 * snare) stay inside the bounds the balance law relies on, and the Ultra bit of the prop's state word does not collide with the state bits.
 */
class RecombinationRulesTest {
    @Test
    void oldPagesKeepTheirIdsAndOrder() {
        assertEquals(List.of("mana_corkscrew", "bulwark", "room_swap", "raging_black_bull"), RecombinationRules.OLD_IDS);
        assertEquals(List.of("house_armour", "room_rebuild", "ultra_giant_bull", "plank_snare"), RecombinationRules.NEW_IDS);
        var all = new HashSet<String>(RecombinationRules.OLD_IDS);
        all.addAll(RecombinationRules.NEW_IDS);
        assertEquals(8, all.size());
    }

    @Test
    void ultraBullIsOnePointSixTimesBigger() {
        assertEquals(1.1f, RecombinationRules.bullScale(1.0, false), 1e-6);
        assertEquals(1.1f * 1.6f, RecombinationRules.bullScale(1.0, true), 1e-5);
        assertEquals(1.34f, RecombinationRules.bullScale(50.0, false), 1e-5);
        for (double ep = 1; ep <= 12; ep += 0.5) assertTrue(RecombinationRules.bullScale(ep, true) > RecombinationRules.bullScale(ep, false));
    }

    @Test
    void ultraFormHitsHarderAndFiresMore() {
        assertTrue(RecombinationRules.ramDamage(true) > RecombinationRules.ramDamage(false));
        assertTrue(RecombinationRules.cannonDamage(true) > RecombinationRules.cannonDamage(false));
        assertTrue(RecombinationRules.cannonShots(true, 0) > RecombinationRules.cannonShots(false, 0));
        assertTrue(RecombinationRules.cannonShots(true, 4) <= 5, "a volley is capped at five targets");
        assertTrue(RecombinationRules.ULTRA_CANNON_COOLDOWN < RecombinationRules.CANNON_COOLDOWN);
    }

    @Test
    void ultraBitDoesNotTouchTheStateWord() {
        assertEquals(0, RecombinationRules.ULTRA_BIT & 0x3FFFF, "bits 0-17 are the state word");
        assertEquals(0, RecombinationRules.ULTRA_BIT & (0x7FF << 19), "bits 19-29 are the Ultra start time");
    }

    @Test
    void armourCutAndHealStayBounded() {
        assertEquals(0.25f, RecombinationRules.armourCut(1.0), 1e-6);
        assertEquals(0.38f, RecombinationRules.armourCut(50.0), 1e-6);
        assertEquals(6f, RecombinationRules.roomHeal(1.0), 1e-6);
        assertTrue(RecombinationRules.roomHeal(100.0) <= 6f + 1.5f * 8f + 1e-6);
    }

    @Test
    void snareHoldsABossForAQuarter() {
        assertEquals(60, RecombinationRules.snareTicks(false, 60));
        assertEquals(15, RecombinationRules.snareTicks(true, 60));
        assertEquals(8, RecombinationRules.snareTicks(true, 5));
        assertFalse(RecombinationRules.snareTicks(true, 60) > RecombinationRules.snareTicks(false, 60));
    }

    @Test
    void summonLifeCappedAtFiftyPercentMore() {
        assertEquals(1200, RecombinationRules.summonTicks(1200, 1.0));
        assertEquals(1800, RecombinationRules.summonTicks(1200, 99.0));
    }
}
