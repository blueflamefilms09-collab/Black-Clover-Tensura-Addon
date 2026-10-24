package com.newuniverse.nusmp.companion;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the rapport tiers, the gift outcomes, the daily allowances, the ownership gates and the pouch rules are the numbers of the whole bond system; each is pinned here with the values the design states. */
class CompanionRulesTest {
    @Test
    void tiersStartAtThreeTenAndTwentyFive() {
        assertEquals(CompanionRules.STRANGER, CompanionRules.tier(0));
        assertEquals(CompanionRules.STRANGER, CompanionRules.tier(2));
        assertEquals(CompanionRules.ACQUAINTANCE, CompanionRules.tier(3));
        assertEquals(CompanionRules.ACQUAINTANCE, CompanionRules.tier(9));
        assertEquals(CompanionRules.FRIEND, CompanionRules.tier(10));
        assertEquals(CompanionRules.FRIEND, CompanionRules.tier(24));
        assertEquals(CompanionRules.CLOSE, CompanionRules.tier(25));
        assertEquals(CompanionRules.CLOSE, CompanionRules.tier(99));
        assertEquals(CompanionRules.STRANGER, CompanionRules.tier(-5));
    }

    @Test
    void rapportStaysBetweenZeroAndNinetyNine() {
        assertEquals(0, CompanionRules.addRapport(0, -1));
        assertEquals(99, CompanionRules.addRapport(98, 5));
        assertEquals(99, CompanionRules.clampRapport(Long.MAX_VALUE));
        assertEquals(0, CompanionRules.clampRapport(Long.MIN_VALUE));
        assertEquals(7, CompanionRules.addRapport(5, 2));
    }

    @Test
    void giftClassesFollowTheListsThenFood() {
        assertEquals(CompanionRules.Gift.HATED, CompanionRules.classify(true, true, true, true));
        assertEquals(CompanionRules.Gift.LOVED, CompanionRules.classify(true, false, false, false));
        assertEquals(CompanionRules.Gift.ACCEPTED, CompanionRules.classify(false, true, false, false));
        assertEquals(CompanionRules.Gift.FOOD, CompanionRules.classify(false, false, false, true));
        assertEquals(CompanionRules.Gift.REFUSED, CompanionRules.classify(false, false, false, false));
        assertFalse(CompanionRules.Gift.REFUSED.taken());
        assertTrue(CompanionRules.Gift.HATED.taken());
    }

    @Test
    void giftRapportMatchesTheDesign() {
        assertEquals(5, CompanionRules.giftRapport(CompanionRules.Gift.LOVED, 0));
        assertEquals(5, CompanionRules.giftRapport(CompanionRules.Gift.LOVED, 1));
        assertEquals(1, CompanionRules.giftRapport(CompanionRules.Gift.LOVED, 2));
        assertEquals(1, CompanionRules.giftRapport(CompanionRules.Gift.LOVED, 50));
        assertEquals(2, CompanionRules.giftRapport(CompanionRules.Gift.FOOD, 9));
        assertEquals(2, CompanionRules.giftRapport(CompanionRules.Gift.ACCEPTED, 0));
        assertEquals(-1, CompanionRules.giftRapport(CompanionRules.Gift.HATED, 0));
        assertEquals(0, CompanionRules.giftRapport(CompanionRules.Gift.REFUSED, 0));
    }

    @Test
    void dailyAllowancesResetWithTheGameDay() {
        assertEquals(0, CompanionRules.day(0));
        assertEquals(0, CompanionRules.day(23999));
        assertEquals(1, CompanionRules.day(24000));
        assertEquals(0, CompanionRules.day(-5));
        assertEquals(2, CompanionRules.lovedToday(4, 2, 4));
        assertEquals(0, CompanionRules.lovedToday(3, 2, 4));
        assertEquals(0, CompanionRules.lovedToday(4, -3, 4));
        assertTrue(CompanionRules.fiveEarns(-1, 0));
        assertFalse(CompanionRules.fiveEarns(7, 7));
        assertTrue(CompanionRules.fiveEarns(6, 7));
    }

    @Test
    void unknownSocialKindsReadAsNone() {
        assertEquals(CompanionRules.NONE, CompanionRules.cleanKind(0));
        assertEquals(CompanionRules.FIVE, CompanionRules.cleanKind(1));
        assertEquals(CompanionRules.NONE, CompanionRules.cleanKind(2));
        assertEquals(CompanionRules.NONE, CompanionRules.cleanKind(-1));
    }

    @Test
    void stancesAreOfferedToTheOwnerOnly() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertTrue(CompanionRules.stanceOffered(true, a, a));
        assertFalse(CompanionRules.stanceOffered(true, a, b));
        assertTrue(CompanionRules.stanceOffered(true, null, b));
        assertFalse(CompanionRules.stanceOffered(false, a, a));
        assertFalse(CompanionRules.stanceOffered(true, null, null));
    }

    @Test
    void thePouchNeedsOwnerOrAFriendWhileNobodys() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertTrue(CompanionRules.pouchOpen(a, a, 0));
        assertFalse(CompanionRules.pouchOpen(a, b, 3));
        assertFalse(CompanionRules.pouchOpen(null, b, CompanionRules.ACQUAINTANCE));
        assertTrue(CompanionRules.pouchOpen(null, b, CompanionRules.FRIEND));
        assertFalse(CompanionRules.pouchOpen(null, null, 3));
    }

    @Test
    void thePouchHelpsBelowTwoFifthsAndAtMostEveryThirtySeconds() {
        assertTrue(CompanionRules.usePouchNow(true, true, 7.9f, 20f, 10_000, 0));
        assertFalse(CompanionRules.usePouchNow(true, true, 8.0f, 20f, 10_000, 0));
        assertFalse(CompanionRules.usePouchNow(true, true, 3f, 20f, 10_000, 9_500));
        assertTrue(CompanionRules.usePouchNow(true, true, 3f, 20f, 10_000, 9_400));
        assertFalse(CompanionRules.usePouchNow(true, false, 3f, 20f, 10_000, 0));
        assertFalse(CompanionRules.usePouchNow(false, true, 3f, 20f, 10_000, 0));
        assertFalse(CompanionRules.usePouchNow(true, true, 0f, 20f, 10_000, 0));
        assertTrue(CompanionRules.usePouchNow(true, true, 3f, 20f, 100, 5_000), "a clock that went back does not block her forever");
    }

    @Test
    void aPotionBeatsFoodAndTheBetterFoodBeatsTheWorse() {
        assertEquals(-1, CompanionRules.bestHeal(new int[]{0, 0}, new int[]{9, 9}));
        assertEquals(1, CompanionRules.bestHeal(new int[]{CompanionRules.FOOD_KIND, CompanionRules.FOOD_KIND}, new int[]{4, 8}));
        assertEquals(0, CompanionRules.bestHeal(new int[]{CompanionRules.FOOD_KIND, CompanionRules.FOOD_KIND}, new int[]{8, 8}));
        assertEquals(2, CompanionRules.bestHeal(new int[]{CompanionRules.FOOD_KIND, CompanionRules.FOOD_KIND, CompanionRules.POTION_KIND}, new int[]{20, 20, 1}));
        assertEquals(-1, CompanionRules.bestHeal(new int[0], new int[0]));
    }
}
