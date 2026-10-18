package com.newuniverse.nusmp.skill.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the summon cap, the Ultimate being strictly stronger and the lifetime that cleans summons up must not regress. */
class SummonRulesTest {
    @Test
    void theCapGrowsWithTierAndNeverExceedsTheHardCeiling() {
        assertEquals(3, SummonRules.maxSummons(3, 6, Tier.BASE));
        assertEquals(6, SummonRules.maxSummons(3, 6, Tier.MASTERED));
        assertEquals(12, SummonRules.maxSummons(3, 6, Tier.ULTIMATE));
        assertEquals(SummonRules.HARD_CAP, SummonRules.maxSummons(50, 50, Tier.ULTIMATE));
        assertEquals(1, SummonRules.maxSummons(0, 0, Tier.BASE));
        // a mastered cap below the base cap in a bad config must not shrink the mastered tier
        assertEquals(5, SummonRules.maxSummons(5, 2, Tier.MASTERED));
    }

    @Test
    void theUltimateIsStrongerThanTheMasteredBaseForEveryCard() {
        for (SummonRules.Card c : new SummonRules.Card[]{SummonRules.Card.BEAST, SummonRules.Card.STONE}) {
            assertTrue(SummonRules.health(c, Tier.ULTIMATE) > SummonRules.health(c, Tier.MASTERED), c + " health");
            assertTrue(SummonRules.damage(c, Tier.ULTIMATE) > SummonRules.damage(c, Tier.MASTERED), c + " damage");
            assertTrue(SummonRules.health(c, Tier.MASTERED) > SummonRules.health(c, Tier.BASE), c + " mastered health");
            assertTrue(SummonRules.lifetimeTicks(c, Tier.ULTIMATE) > SummonRules.lifetimeTicks(c, Tier.MASTERED));
        }
        assertEquals(3, SummonRules.mobCount(SummonRules.Card.BEAST, Tier.ULTIMATE));
        assertEquals(1, SummonRules.mobCount(SummonRules.Card.BEAST, Tier.MASTERED));
        assertTrue(SummonRules.scale(SummonRules.Card.STONE, Tier.ULTIMATE) > 1.0);
    }

    @Test
    void theNewCardsBelongToTheUltimateOnly() {
        assertFalse(SummonRules.available(SummonRules.Card.LEGION, Tier.MASTERED));
        assertFalse(SummonRules.available(SummonRules.Card.WRAITH, Tier.BASE));
        assertTrue(SummonRules.available(SummonRules.Card.LEGION, Tier.ULTIMATE));
        assertTrue(SummonRules.available(SummonRules.Card.BEAST, Tier.BASE));
        // append-only enum: the stored mode indexes keep their meaning
        assertEquals(0, SummonRules.Card.BEAST.ordinal());
        assertEquals(1, SummonRules.Card.STONE.ordinal());
        assertEquals(2, SummonRules.Card.LEGION.ordinal());
        assertEquals(3, SummonRules.Card.WRAITH.ordinal());
    }

    @Test
    void allowedNeverGoesNegativeOrOverTheWant() {
        assertEquals(3, SummonRules.allowed(0, 6, 3));
        assertEquals(2, SummonRules.allowed(4, 6, 3));
        assertEquals(0, SummonRules.allowed(6, 6, 3));
        assertEquals(0, SummonRules.allowed(9, 6, 3));
        assertEquals(0, SummonRules.allowed(0, 6, -2));
        assertEquals(5, SummonRules.allowed(-3, 12, 5));
    }

    @Test
    void summonsVanishWhenTheirTimeIsUpOrTheirOwnerIsGone() {
        assertFalse(SummonRules.shouldVanish(100, 500, true, true));
        assertTrue(SummonRules.shouldVanish(500, 500, true, true));
        assertTrue(SummonRules.shouldVanish(100, 500, false, true));
        assertTrue(SummonRules.shouldVanish(100, 500, true, false));
        assertFalse(SummonRules.shouldVanish(10_000_000, 0, true, true), "an unset lifetime never expires on its own");
    }

    @Test
    void theWraithCardIsShortLived() {
        assertTrue(SummonRules.lifetimeTicks(SummonRules.Card.WRAITH, Tier.ULTIMATE) <= 2400);
    }

    @Test
    void excessAndLeash() {
        assertEquals(0, SummonRules.excess(3, 6));
        assertEquals(2, SummonRules.excess(8, 6));
        assertEquals(0, SummonRules.leashAction(5));
        assertEquals(1, SummonRules.leashAction(20));
        assertEquals(2, SummonRules.leashAction(40));
    }

    @Test
    void biggerCardsCostAndWaitMore() {
        assertEquals(500.0, SummonRules.cost(SummonRules.Card.BEAST, 500));
        assertEquals(1500.0, SummonRules.cost(SummonRules.Card.LEGION, 500));
        assertTrue(SummonRules.cooldown(SummonRules.Card.LEGION, 100) > SummonRules.cooldown(SummonRules.Card.BEAST, 100));
        assertTrue(SummonRules.masteryGain(SummonRules.Card.LEGION) > SummonRules.masteryGain(SummonRules.Card.BEAST));
    }
}
