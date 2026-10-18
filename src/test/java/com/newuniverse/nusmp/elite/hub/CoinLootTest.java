package com.newuniverse.nusmp.elite.hub;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryScaling;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: "deeper pays more, the Hub pays nothing, a boss always pays" is the economy's contract, and the loot roll is pure, so every part of it is pinned here: the tier boundaries agree with the
 * Mob Foundry's, the expected payout rises with the floor and the last floor of a tier never beats the first of the next, a rift chest beats a plain chest, Looting helps mobs and chests, and a
 * statistical run shows a mob pays about its chance.
 */
class CoinLootTest {
    private static final CoinLoot.Params P = CoinLoot.DEFAULTS;

    private static Dice seeded(long seed) {
        Random r = new Random(seed);
        return r::nextDouble;
    }

    @Test
    void theTierBoundariesAreTheFoundrysOnBothRoads() {
        assertEquals(0, CoinLoot.tierOf(0));
        int[] floors = {1, 2, 199, 200, 201, 400, 401, 600, 601, 800, 801, 1000, 1001, 5000};
        for (int f : floors) {
            assertEquals(FoundryScaling.tierOf(f), CoinLoot.tierOf(f), "holy floor " + f);
            assertEquals(FoundryScaling.tierOf(-f), CoinLoot.tierOf(-f), "abyssal floor " + f);
        }
        assertEquals(0.0, CoinLoot.depth(1));
        assertEquals(1.0, CoinLoot.depth(200));
        assertEquals(0.0, CoinLoot.depth(201));
        assertEquals(1.0, CoinLoot.depth(-5000));
    }

    @Test
    void theHubPaysNothingWhateverTheSource() {
        for (CoinLoot.Source s : CoinLoot.Source.values()) {
            assertEquals(0L, CoinLoot.roll(s, 0, 3, P, () -> 0.0), s + " in the Hub");
            assertEquals(0.0, CoinLoot.expected(s, 0, P));
        }
        assertEquals(0.0, CoinLoot.mobChance(0, 5, P));
    }

    @Test
    void deeperAlwaysPaysMoreAndATierBoundaryNeverPaysLess() {
        for (CoinLoot.Source s : CoinLoot.Source.values()) {
            double prev = 0;
            for (int f = 1; f <= 1100; f++) {
                double e = CoinLoot.expected(s, f, P);
                assertTrue(e >= prev, s + " floor " + f + " pays less than floor " + (f - 1));
                assertEquals(e, CoinLoot.expected(s, -f, P), 1e-9, "both roads pay alike at the same depth");
                prev = e;
            }
        }
        for (int t = 1; t < CoinLoot.TIERS; t++) {
            int last = t * CoinLoot.FLOORS_PER_TIER;
            assertTrue(CoinLoot.expected(CoinLoot.Source.MOB, last, P) < CoinLoot.expected(CoinLoot.Source.MOB, last + 1, P), "tier " + t + " to " + (t + 1));
        }
    }

    @Test
    void aBossAndAChestAlwaysPayAndARiftChestPaysMore() {
        for (int i = 0; i < 200; i++) {
            Dice d = seeded(i);
            assertTrue(CoinLoot.roll(CoinLoot.Source.BOSS, 10, 0, P, d) > 0);
            assertTrue(CoinLoot.roll(CoinLoot.Source.CHEST, 3, 0, P, d) > 0);
        }
        assertTrue(CoinLoot.expected(CoinLoot.Source.RIFT_CHEST, 5, P) > CoinLoot.expected(CoinLoot.Source.CHEST, 5, P));
        assertEquals(3.0, CoinLoot.expected(CoinLoot.Source.RIFT_CHEST, 5, P) / CoinLoot.expected(CoinLoot.Source.CHEST, 5, P), 1e-9);
        assertTrue(CoinLoot.expected(CoinLoot.Source.BOSS, 10, P) > CoinLoot.expected(CoinLoot.Source.CHEST, 10, P) * 5, "a decennial boss is the big payday");
    }

    @Test
    void aMobPaysAboutItsChanceAndLootingHelps() {
        int paid = 0, paidLoot = 0, n = 20_000;
        Dice d = seeded(5);
        Dice d2 = seeded(5);
        for (int i = 0; i < n; i++) {
            if (CoinLoot.roll(CoinLoot.Source.MOB, 7, 0, P, d) > 0) paid++;
            if (CoinLoot.roll(CoinLoot.Source.MOB, 7, 3, P, d2) > 0) paidLoot++;
        }
        assertEquals(P.mobChance(), paid / (double) n, 0.03);
        assertEquals(P.mobChance() + 0.15, paidLoot / (double) n, 0.03);
        assertTrue(paidLoot > paid);
        assertEquals(1.0, CoinLoot.mobChance(1, 100, P), "never above certain");
    }

    @Test
    void theJitterStaysInsideItsBandAndLootingRaisesAmountsNotBosses() {
        double mid = CoinLoot.expected(CoinLoot.Source.CHEST, 50, P);
        for (int i = 0; i < 2_000; i++) {
            long v = CoinLoot.roll(CoinLoot.Source.CHEST, 50, 0, P, seeded(i));
            assertTrue(v >= Math.floor(mid * 0.6) && v <= Math.ceil(mid * 1.4), "chest " + v + " vs " + mid);
        }
        assertTrue(CoinLoot.roll(CoinLoot.Source.CHEST, 50, 5, P, () -> 0.5) > CoinLoot.roll(CoinLoot.Source.CHEST, 50, 0, P, () -> 0.5));
        assertEquals(CoinLoot.roll(CoinLoot.Source.BOSS, 50, 5, P, () -> 0.5), CoinLoot.roll(CoinLoot.Source.BOSS, 50, 0, P, () -> 0.5), "Looting does not touch a boss");
    }

    @Test
    void scaleZeroSwitchesEverythingOffAndBadNumbersAreClamped() {
        CoinLoot.Params off = new CoinLoot.Params(0.5, 6, 80, 1500, 3, 3, 0.5, 0.1, 0.0);
        for (CoinLoot.Source s : CoinLoot.Source.values()) assertEquals(0L, CoinLoot.roll(s, 10, 0, off, () -> 0.0));
        CoinLoot.Params bad = new CoinLoot.Params(Double.NaN, -5, -5, -5, -1, -1, 99, 99, -3);
        assertEquals(0.0, bad.mobChance());
        assertEquals(1.0, bad.tierGrowth());
        assertEquals(0.0, bad.floorGrowth());
        assertFalse(CoinLoot.roll(CoinLoot.Source.BOSS, 10, 0, bad, () -> 0.5) > 0);
        long huge = CoinLoot.roll(CoinLoot.Source.BOSS, 900, 0, new CoinLoot.Params(1, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, 100, 10, 1, 1, 100), () -> 0.99);
        assertTrue(huge <= CoinMath.CAP, "never above the cap");
    }

    @Test
    void currencyModeParsesWithTheCoinsAsTheDefault() {
        assertEquals(CurrencyMode.TENSURA_COINS, CurrencyMode.parse("tensura_coins"));
        assertEquals(CurrencyMode.MAGIC_STONES, CurrencyMode.parse("magic_stones"));
        assertEquals(CurrencyMode.MAGIC_STONES, CurrencyMode.parse("  Magic-Stones "));
        assertEquals(CurrencyMode.TENSURA_COINS, CurrencyMode.parse("gold"));
        assertEquals(CurrencyMode.TENSURA_COINS, CurrencyMode.parse(null));
    }
}
