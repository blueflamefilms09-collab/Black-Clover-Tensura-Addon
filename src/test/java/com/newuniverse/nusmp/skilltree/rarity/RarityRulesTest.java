package com.newuniverse.nusmp.skilltree.rarity;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the rarity numbers are the owner's design (tier 1 weights 600/300/80/10/0, the Luck factor {@code min(20, 1 + 0.5 * luck * rank)}, the chance multiplier {@code min(10, 1 + 0.25 * looting + 0.05 *
 * luck)}, the Guardian's rarity by depth, the rift multiplier {@code min(3, 1 + 0.1 * luck)}) and a number wrong by a factor is invisible in a game that drops one book in a thousand kills. So this
 * is the rule written as examples, then the rates measured: 200,000 scripted draws (evenly spaced values, so the count of each rarity must land within one draw of the exact share, and a seeded random
 * run within five standard deviations). Luck and Looting are clamped and cleaned (NaN, infinity, negatives), common never changes with Luck, mythic never comes from a monster, and a missing dice or
 * config never throws.
 */
class RarityRulesTest {
    private static final int N = 200_000;

    private static Dice even(int i) { return () -> (i + 0.5) / N; }

    @Test
    void theLuckFactorGrowsWithTheRankAndIsCapped() {
        assertEquals(1.0, RarityRules.luckFactor(0, 50), 0.0, "common never changes with Luck");
        assertEquals(1.0, RarityRules.luckFactor(3, 0), 0.0);
        assertEquals(4.0, RarityRules.luckFactor(3, 2), 1e-12);        // 1 + 0.5 * 2 * 3
        assertEquals(2.0, RarityRules.luckFactor(1, 2), 1e-12);
        assertEquals(RarityRules.MAX_LUCK_FACTOR, RarityRules.luckFactor(4, 1000), 0.0);
        assertEquals(1.0, RarityRules.luckFactor(4, -3), 0.0, "bad luck never makes a drop rarer than the base");
        assertEquals(1.0, RarityRules.luckFactor(4, Double.NaN), 0.0);
        assertEquals(RarityRules.MAX_LUCK_FACTOR, RarityRules.luckFactor(4, Double.POSITIVE_INFINITY), 0.0);
        assertEquals(RarityRules.luckFactor(4, 3), RarityRules.luckFactor(99, 3), 0.0, "a rank above mythic counts as mythic");
        assertEquals(1.0, RarityRules.luckFactor(-4, 3), 0.0, "a rank below common counts as common");
    }

    @Test
    void theChanceFactorUsesLootingAndLuckAndIsCapped() {
        assertEquals(1.0, RarityRules.abyssalChanceFactor(0, 0), 0.0);
        assertEquals(1.75, RarityRules.abyssalChanceFactor(3, 0), 1e-12);
        assertEquals(2.0, RarityRules.abyssalChanceFactor(0, 20), 1e-12);
        assertEquals(2.75, RarityRules.abyssalChanceFactor(3, 20), 1e-12);
        assertEquals(RarityRules.MAX_CHANCE_FACTOR, RarityRules.abyssalChanceFactor(1000, 1000), 0.0);
        assertEquals(1.0, RarityRules.abyssalChanceFactor(-7, -7), 0.0);
        assertEquals(1.0, RarityRules.abyssalChanceFactor(0, Double.NaN), 0.0);
        assertEquals(0.001, RarityRules.abyssalBookChance(0.001, 0, 0), 1e-15);
        assertEquals(0.00175, RarityRules.abyssalBookChance(0.001, 3, 0), 1e-12);
        assertEquals(0.0, RarityRules.abyssalBookChance(Double.NaN, 3, 3), 0.0);
        assertEquals(0.0, RarityRules.abyssalBookChance(-1, 3, 3), 0.0);
        assertEquals(1.0, RarityRules.abyssalBookChance(5, 3, 3), 0.0);
    }

    @Test
    void theRiftChanceHasItsOwnSmallerMultiplier() {
        assertEquals(1.0, RarityRules.riftFactor(0), 0.0);
        assertEquals(2.0, RarityRules.riftFactor(10), 1e-12);
        assertEquals(3.0, RarityRules.riftFactor(1000), 0.0);
        assertEquals(1.0, RarityRules.riftFactor(-5), 0.0);
        assertEquals(1.0, RarityRules.riftFactor(Double.NaN), 0.0);
        assertEquals(0.2, RarityRules.riftChance(0.1, 10), 1e-12);
        assertEquals(1.0, RarityRules.riftChance(0.9, 1000), 0.0);
        assertEquals(0.0, RarityRules.riftChance(Double.NaN, 10), 0.0);
        assertEquals(0.0, RarityRules.riftChance(-1, 10), 0.0);
    }

    @Test
    void theMonsterWeightsAreTheDesignsAndMythicIsZero() {
        assertEquals(600, RarityRules.monsterWeight(BookRarity.COMMON));
        assertEquals(300, RarityRules.monsterWeight(BookRarity.UNCOMMON));
        assertEquals(80, RarityRules.monsterWeight(BookRarity.RARE));
        assertEquals(10, RarityRules.monsterWeight(BookRarity.EPIC));
        assertEquals(0, RarityRules.monsterWeight(BookRarity.MYTHIC));
        assertEquals(990.0, RarityRules.monsterTotal(0), 1e-9);
        assertEquals(10.0 / 990.0, RarityRules.monsterShare(BookRarity.EPIC, 0), 1e-12, "an epic is 1% of the books, so about 0.001% of the kills at base 0.1%");
        assertEquals(0.0, RarityRules.monsterShare(BookRarity.MYTHIC, 100), 0.0);
        for (double luck : new double[] {0, 1, 5, 20, 1000}) {
            double sum = 0;
            for (BookRarity r : BookRarity.values()) sum += RarityRules.monsterShare(r, luck);
            assertEquals(1.0, sum, 1e-9, "shares add up at luck " + luck);
        }
        assertTrue(RarityRules.monsterShare(BookRarity.EPIC, 10) > RarityRules.monsterShare(BookRarity.EPIC, 0), "luck helps the epic share");
        assertTrue(RarityRules.monsterShare(BookRarity.COMMON, 10) < RarityRules.monsterShare(BookRarity.COMMON, 0), "and takes it from the common one");
    }

    @Test
    void twoHundredThousandEvenDrawsLandOnTheShares() {
        for (double luck : new double[] {0, 3, 10}) {
            int[] hits = new int[BookRarity.count()];
            for (int i = 0; i < N; i++) hits[RarityRules.drawMonsterRarity(luck, even(i)).rank()]++;
            for (BookRarity r : BookRarity.values()) {
                double expected = N * RarityRules.monsterShare(r, luck);
                assertEquals(expected, hits[r.rank()], 2.0, r.id() + " at luck " + luck);
            }
            assertEquals(0, hits[BookRarity.MYTHIC.rank()], "a monster never drops a mythic book");
        }
    }

    @Test
    void aSeededRandomRunLandsOnTheSharesWithinFiveSigma() {
        Random random = new Random(326);
        Dice dice = random::nextDouble;
        double luck = 4.0;
        int[] hits = new int[BookRarity.count()];
        for (int i = 0; i < N; i++) hits[RarityRules.drawMonsterRarity(luck, dice).rank()]++;
        for (BookRarity r : BookRarity.values()) {
            double p = RarityRules.monsterShare(r, luck), sigma = Math.sqrt(N * p * (1 - p));
            assertTrue(Math.abs(hits[r.rank()] - N * p) <= 5 * sigma + 1e-9, r.id() + ": " + hits[r.rank()] + " of " + N + ", expected " + N * p);
        }
    }

    @Test
    void theRarityDrawSurvivesBadInput() {
        assertEquals(BookRarity.COMMON, RarityRules.drawMonsterRarity(0, null));
        assertEquals(BookRarity.COMMON, RarityRules.drawMonsterRarity(Double.NaN, () -> Double.NaN));
        assertEquals(BookRarity.COMMON, RarityRules.drawMonsterRarity(-9, () -> 0.0));
        assertEquals(BookRarity.EPIC, RarityRules.drawMonsterRarity(0, () -> 1.0), "the top of the range is the last rarity that can drop");
        assertEquals(BookRarity.EPIC, RarityRules.drawMonsterRarity(0, () -> 7.0));
        assertNotEquals(BookRarity.MYTHIC, RarityRules.drawMonsterRarity(Double.POSITIVE_INFINITY, () -> 0.999999));
    }

    @Test
    void theGuardianRarityFollowsTheDepth() {
        for (int f : new int[] {10, 20, -10, -20, 0, 5}) assertEquals(BookRarity.RARE, RarityRules.guardianRarity(f, () -> 0.0), "floor " + f + " is rare whatever the dice say");
        for (int f : new int[] {50, 60, 90, -50, -100}) assertEquals(BookRarity.EPIC, RarityRules.guardianRarity(f, () -> 0.99), "floor " + f + " is epic");
        assertEquals(BookRarity.EPIC, RarityRules.guardianRarity(Integer.MIN_VALUE, () -> 0.99));
        for (int f : new int[] {30, 40, -30, -40}) {
            assertEquals(BookRarity.EPIC, RarityRules.guardianRarity(f, () -> 0.05));
            assertEquals(BookRarity.RARE, RarityRules.guardianRarity(f, () -> 0.50));
            assertEquals(BookRarity.RARE, RarityRules.guardianRarity(f, null));
        }
        int epic = 0;
        for (int i = 0; i < N; i++) if (RarityRules.guardianRarity(30, even(i)) == BookRarity.EPIC) epic++;
        assertEquals(N * RarityRules.MIXED_EPIC_CHANCE, epic, 2.0, "one Guardian in five of floors 30 and 40 gives an epic book");
    }

    @Test
    void theRiftAndTheMonsterSwitches() {
        assertEquals(BookRarity.MYTHIC, RarityRules.riftRarity(true));
        assertEquals(BookRarity.EPIC, RarityRules.riftRarity(false));
        assertFalse(RarityRules.dropsFromMonsters(BookRarity.MYTHIC));
        for (BookRarity r : new BookRarity[] {BookRarity.COMMON, BookRarity.UNCOMMON, BookRarity.RARE, BookRarity.EPIC}) assertTrue(RarityRules.dropsFromMonsters(r));
    }

    @Test
    void saneAndUnitCleanTheNumbers() {
        assertEquals(0.0, RarityRules.sane(Double.NaN), 0.0);
        assertEquals(1.0e9, RarityRules.sane(Double.POSITIVE_INFINITY), 0.0);
        assertEquals(-1.0e9, RarityRules.sane(Double.NEGATIVE_INFINITY), 0.0);
        assertEquals(0.0, RarityRules.unit(-0.5), 0.0);
        assertEquals(1.0, RarityRules.unit(3), 0.0);
        assertEquals(0.25, RarityRules.unit(0.25), 0.0);
    }
}
