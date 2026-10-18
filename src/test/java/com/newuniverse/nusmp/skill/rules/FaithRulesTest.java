package com.newuniverse.nusmp.skill.rules;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Faith points are saved data; their arithmetic must never go negative or overflow, and the Ultimate must be cheaper. */
class FaithRulesTest {
    @Test
    void theStoredKeyIsTheOriginalOne() {
        assertEquals("Faith", FaithRules.FAITH_KEY);
    }

    @Test
    void faithNeverGoesNegativeOrOverTheCeiling() {
        assertEquals(0, FaithRules.clamp(-5));
        assertEquals(FaithRules.MAX_FAITH, FaithRules.clamp(Long.MAX_VALUE));
        assertEquals(7, FaithRules.spend(10, 3));
        assertEquals(0, FaithRules.spend(2, 9));
        assertEquals(12, FaithRules.add(10, 2));
        assertEquals(10, FaithRules.add(10, -4), "a negative gain is ignored");
        assertEquals(FaithRules.MAX_FAITH, FaithRules.add(FaithRules.MAX_FAITH, 50));
    }

    @Test
    void prayingYieldsMoreWithTier() {
        assertEquals(1, FaithRules.prayGain(Tier.BASE));
        assertTrue(FaithRules.prayGain(Tier.MASTERED) > FaithRules.prayGain(Tier.BASE));
        assertEquals(3, FaithRules.prayGain(Tier.ULTIMATE), "the Goddess triples prayer");
    }

    @Test
    void theGoddessBestowsForHalfTheFaithAndHalfTheWait() {
        assertEquals(10, FaithRules.bestowCost(10, Tier.BASE));
        assertEquals(5, FaithRules.bestowCost(10, Tier.ULTIMATE));
        assertEquals(1, FaithRules.bestowCost(1, Tier.ULTIMATE), "never free unless configured free");
        assertEquals(0, FaithRules.bestowCost(0, Tier.ULTIMATE));
        assertEquals(12000, FaithRules.bestowCooldown(12000, Tier.BASE));
        assertEquals(6000, FaithRules.bestowCooldown(12000, Tier.ULTIMATE));
        assertTrue(FaithRules.canSpend(10, 10));
        assertFalse(FaithRules.canSpend(9, 10));
        assertTrue(FaithRules.canSpend(0, 0));
    }

    @Test
    void theGiftPoolHasEachValidUnownedIdOnceInOrder() {
        List<String> cfg = Arrays.asList("tensura:a", "tensura:b", " tensura:a ", "", null, "tensura:c", "bad:x");
        Set<String> has = Set.of("tensura:b");
        Set<String> exists = Set.of("tensura:a", "tensura:b", "tensura:c");
        List<String> pool = FaithRules.giftPool(cfg, has::contains, exists::contains);
        assertEquals(List.of("tensura:a", "tensura:c"), pool);
        assertTrue(FaithRules.giftPool(null, s -> false, s -> true).isEmpty());
    }

    @Test
    void pickIndexIsAlwaysInRange() {
        assertEquals(-1, FaithRules.pickIndex(0, 5));
        for (int roll : new int[]{0, 1, -1, Integer.MIN_VALUE, Integer.MAX_VALUE, 12345}) {
            int i = FaithRules.pickIndex(4, roll);
            assertTrue(i >= 0 && i < 4, "roll " + roll + " gave " + i);
        }
    }

    @Test
    void theUltimateOnlyHasABlessingRadius() {
        assertEquals(0.0, FaithRules.blessingRadius(Tier.MASTERED));
        assertTrue(FaithRules.blessingRadius(Tier.ULTIMATE) > 0);
        assertTrue(FaithRules.MIRACLE_COST > FaithRules.BLESSING_COST);
    }
}
