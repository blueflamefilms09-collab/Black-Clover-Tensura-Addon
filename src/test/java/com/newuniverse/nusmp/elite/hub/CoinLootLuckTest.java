package com.newuniverse.nusmp.elite.hub;

import com.newuniverse.nusmp.labyrinth.loot.LuckGauss;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tensura coins also scale with Luck (1.107.0): the old roll is unchanged with no Luck, and Luck raises the amount and a mob's chance to pay, within the cap. */
class CoinLootLuckTest {
    private static final CoinLoot.Params P = CoinLoot.DEFAULTS;

    @Test
    void withNoLuckTheNewRollIsTheOldRollDrawForDraw() {
        for (CoinLoot.Source src : CoinLoot.Source.values()) {
            for (int floor : new int[]{0, 1, 7, 250, 999}) {
                for (int looting : new int[]{0, 3}) {
                    Random a = new Random(floor * 31L + looting), b = new Random(floor * 31L + looting);
                    for (int i = 0; i < 200; i++) {
                        assertEquals(CoinLoot.roll(src, floor, looting, P, a::nextDouble), CoinLoot.roll(src, floor, looting, 0.0, LuckGauss.COINS, P, b::nextDouble), src + " floor " + floor);
                    }
                }
            }
        }
    }

    @Test
    void theHubStillPaysNothingWhateverTheLuck() {
        for (CoinLoot.Source src : CoinLoot.Source.values()) assertEquals(0L, CoinLoot.roll(src, 0, 5, 20.0, LuckGauss.COINS, P, () -> 0.0));
    }

    @Test
    void luckRaisesTheAmountButNeverPastTheCap() {
        double sum0 = 0, sum15 = 0;
        int n = 20000;
        Random a = new Random(5), b = new Random(5);
        long max = 0;
        for (int i = 0; i < n; i++) {
            sum0 += CoinLoot.roll(CoinLoot.Source.CHEST, 40, 0, 0.0, LuckGauss.COINS, P, a::nextDouble);
            long v = CoinLoot.roll(CoinLoot.Source.CHEST, 40, 0, 15.0, LuckGauss.COINS, P, b::nextDouble);
            sum15 += v;
            max = Math.max(max, v);
        }
        assertTrue(sum15 > sum0 * 1.3, "Luck 15 raises a chest's coins: " + sum0 / n + " vs " + sum15 / n);
        double top = CoinLoot.expected(CoinLoot.Source.CHEST, 40, P) * 1.4 * LuckGauss.COINS.maxFactor();
        assertTrue(max <= Math.round(top) + 1, "never past the jitter times the Luck cap: " + max + " vs " + top);
    }

    @Test
    void luckRaisesAMobsChanceToPayAndTheBossAmountToo() {
        int paid0 = 0, paid15 = 0;
        Random a = new Random(9), b = new Random(9);
        for (int i = 0; i < 20000; i++) {
            if (CoinLoot.roll(CoinLoot.Source.MOB, 12, 0, 0.0, LuckGauss.COINS, P, a::nextDouble) > 0) paid0++;
            if (CoinLoot.roll(CoinLoot.Source.MOB, 12, 0, 15.0, LuckGauss.COINS, P, b::nextDouble) > 0) paid15++;
        }
        assertTrue(paid15 > paid0 + 1500, "15 points of Luck add fifteen points of chance: " + paid0 + " vs " + paid15);
        double boss0 = 0, boss15 = 0;
        Random c = new Random(2), d = new Random(2);
        for (int i = 0; i < 5000; i++) {
            boss0 += CoinLoot.roll(CoinLoot.Source.BOSS, 100, 0, 0.0, LuckGauss.COINS, P, c::nextDouble);
            boss15 += CoinLoot.roll(CoinLoot.Source.BOSS, 100, 0, 15.0, LuckGauss.COINS, P, d::nextDouble);
        }
        assertTrue(boss15 > boss0 * 1.3, "a boss pays more to a lucky killer");
    }

    @Test
    void aLuckCurveThatDoesNothingLeavesTheRollAlone() {
        LuckGauss.Params off = new LuckGauss.Params(0.0, 0.0, 0.5, 1.0);
        Random a = new Random(4), b = new Random(4);
        for (int i = 0; i < 500; i++) {
            long plain = CoinLoot.roll(CoinLoot.Source.CHEST, 9, 1, 0.0, LuckGauss.COINS, P, a::nextDouble);
            long with = CoinLoot.roll(CoinLoot.Source.CHEST, 9, 1, 15.0, off, P, b::nextDouble);
            assertEquals(plain, with);
        }
    }
}
