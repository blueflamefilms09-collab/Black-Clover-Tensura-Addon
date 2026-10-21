package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Luck curve of {@link LuckGauss}: only adds, draws nothing without Luck or depth, grows with both, saturates, stays clamped. */
class LuckGaussTest {
    private static final LuckGauss.Params P = LuckGauss.ITEMS;

    /** A dice that fails the test if it is asked for a number. */
    private static final Dice NEVER = () -> { throw new AssertionError("a random number was drawn when none should be"); };

    @Test
    void withNoLuckAndNoDepthNothingChangesAndNoNumberIsDrawn() {
        assertEquals(0.0, LuckGauss.mean(0.0, 0, P));
        assertEquals(0.0, LuckGauss.extra(0.0, 0, NEVER, P));
        for (int base : new int[]{1, 4, 17, 64}) assertEquals(base, LuckGauss.count(base, 0.0, 0, NEVER, P, 64));
        assertEquals(1.0, LuckGauss.factor(0.0, NEVER, LuckGauss.COINS));
        assertEquals(500L, LuckGauss.scale(500L, 0.0, NEVER, LuckGauss.COINS));
    }

    @Test
    void negativeLuckCountsAsNoLuck() {
        assertEquals(0.0, LuckGauss.luck(-5.0));
        assertEquals(0.0, LuckGauss.luck(Double.NaN));
        assertEquals(LuckGauss.MAX_LUCK, LuckGauss.luck(1000.0));
        assertEquals(4, LuckGauss.count(4, -9.0, 0, NEVER, P, 64));
    }

    @Test
    void theMiddleRisesWithLuckAndWithDepthAndDepthSaturates() {
        double last = -1;
        for (int luck = 0; luck <= 20; luck++) {
            double m = LuckGauss.mean(luck, 10, P);
            assertTrue(m > last, "mean must rise with Luck: " + luck);
            last = m;
        }
        last = -1;
        for (int floor : new int[]{0, 1, 10, 50, 200, 800, 2000}) {
            double m = LuckGauss.mean(0.0, floor, P);
            assertTrue(m >= last, "mean must not fall with depth");
            last = m;
        }
        assertEquals(0.5 * P.depthGain(), LuckGauss.mean(0.0, 200, P), 1e-9, "floor 200 is half of the depth term");
        assertTrue(LuckGauss.mean(0.0, 100000, P) < P.depthGain(), "the depth term never reaches its gain");
        assertEquals(LuckGauss.mean(0.0, 77, P), LuckGauss.mean(0.0, -77, P), "the abyssal floors are as deep as the holy ones");
    }

    @Test
    void theCountOnlyEverRisesAndNeverPassesTheCapOrTheFactor() {
        Random rnd = new Random(42);
        Dice d = rnd::nextDouble;
        for (int i = 0; i < 20000; i++) {
            int base = 1 + rnd.nextInt(16);
            double luck = rnd.nextDouble() * 25 - 3;
            int floor = rnd.nextInt(1200) - 100;
            int now = LuckGauss.count(base, luck, floor, d, P, 64);
            assertTrue(now >= base, "never fewer: " + base + " -> " + now);
            assertTrue(now <= Math.max(base, 64), "never past the cap");
            assertTrue(now <= Math.ceil(base * P.maxFactor()) + 1, "never past the factor: " + base + " -> " + now);
        }
    }

    @Test
    void theCapWins() {
        Random rnd = new Random(3);
        for (int i = 0; i < 2000; i++) assertTrue(LuckGauss.count(60, 20.0, 500, rnd::nextDouble, P, 64) <= 64);
        assertEquals(60, LuckGauss.count(60, 20.0, 500, () -> 0.5, P, 10), "a base above the cap is kept, not cut");
    }

    @Test
    void theAverageFollowsTheMiddleAndALuckierPlayerGetsMore() {
        Random a = new Random(5), b = new Random(5);
        double sum0 = 0, sum10 = 0, sum20 = 0;
        int n = 40000;
        for (int i = 0; i < n; i++) {
            sum0 += LuckGauss.count(10, 0.0, 50, a::nextDouble, P, 64);
            sum10 += LuckGauss.count(10, 10.0, 50, b::nextDouble, P, 64);
        }
        Random c = new Random(5);
        for (int i = 0; i < n; i++) sum20 += LuckGauss.count(10, 20.0, 50, c::nextDouble, P, 64);
        assertTrue(sum10 > sum0 * 1.3, "Luck 10 should be well above none: " + sum0 / n + " vs " + sum10 / n);
        assertTrue(sum20 > sum10, "Luck 20 above Luck 10");
        double expected10 = 10 * (1 + LuckGauss.mean(10.0, 50, P));
        assertEquals(expected10, sum10 / n, 0.6, "the average is near base times one plus the middle (the clamp at zero lifts it a little)");
    }

    @Test
    void theGaussianHasTheRightShape() {
        Random rnd = new Random(9);
        double sum = 0, sq = 0;
        int n = 200000;
        for (int i = 0; i < n; i++) {
            double g = LuckGauss.gauss(rnd::nextDouble);
            sum += g;
            sq += g * g;
        }
        assertEquals(0.0, sum / n, 0.02, "mean 0");
        assertEquals(1.0, sq / n, 0.03, "variance 1");
        assertTrue(Double.isFinite(LuckGauss.gauss(() -> 0.0)), "a zero draw stays finite");
        assertTrue(Double.isFinite(LuckGauss.gauss(() -> 0.999999999999)));
    }

    @Test
    void coinsScaleWithLuckButOnlyUpAndWithinTheirCap() {
        Random rnd = new Random(11);
        double sum = 0;
        for (int i = 0; i < 20000; i++) {
            long v = LuckGauss.scale(1000L, 15.0, rnd::nextDouble, LuckGauss.COINS);
            assertTrue(v >= 1000L, "never less coin");
            assertTrue(v <= Math.round(1000L * LuckGauss.COINS.maxFactor()) + 1, "never past the coin cap");
            sum += v;
        }
        assertTrue(sum / 20000 > 1300, "Luck 15 should raise the coins clearly: " + sum / 20000);
        assertEquals(0.0, LuckGauss.coinChanceBonus(0.0));
        assertEquals(0.05, LuckGauss.coinChanceBonus(5.0), 1e-9);
        assertEquals(0.15, LuckGauss.coinChanceBonus(500.0), 1e-9, "the chance bonus is capped at fifteen points");
    }

    @Test
    void paramsAreClamped() {
        LuckGauss.Params wild = new LuckGauss.Params(-3, 99, Double.NaN, 0);
        assertEquals(0.0, wild.luckGain());
        assertEquals(5.0, wild.depthGain());
        assertEquals(0.0, wild.sigmaRatio());
        assertEquals(1.0, wild.maxFactor());
        assertEquals(7, LuckGauss.count(7, 20.0, 900, () -> 0.3, wild, 64), "a max factor of 1 means Luck does nothing");
    }

    @Test
    void armourChancesRiseWithLuckAndDepthAndStayCapped() {
        for (LuckGauss.Kind k : LuckGauss.Kind.values()) {
            double a0 = LuckGauss.armourChance(0, 0, k), a10 = LuckGauss.armourChance(10, 0, k), aDeep = LuckGauss.armourChance(10, 600, k);
            assertTrue(a0 > 0 && a10 > a0 && aDeep >= a10, "armour chance rises: " + k);
            assertTrue(LuckGauss.armourChance(1000, 100000, k) <= LuckGauss.MAX_ARMOUR);
        }
    }
}
