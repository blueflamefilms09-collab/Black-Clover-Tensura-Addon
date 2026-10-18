package com.newuniverse.nusmp.skilltree.item;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a gamble the players pay real money-in-the-game for must have the odds the owner described: a floor of 1, a 5% jackpot of 20, ordinary payouts between 1
 * and 9, and a mean of about 5. Each of those is pinned here, three ways: with scripted dice at the exact boundaries of the table (so an off-by-one in the cumulative
 * walk is caught at the number where it happens), with the floor argument (a floor raises the minimum but a silly floor is clamped and never touches the jackpot),
 * and with a long seeded run whose proportions and mean must land within five standard deviations of the exact values (a fixed seed, so the test either always
 * passes or always fails).
 */
class JackInTheBoxTest {
    /** Dice that return the given values in order, then the last one forever. */
    private static Dice seq(double... v) {
        int[] i = {0};
        return () -> v[Math.min(i[0]++, v.length - 1)];
    }

    @Test
    void theJackpotIsTheFirstDrawBelowFivePercent() {
        assertEquals(20, JackInTheBox.roll(1, seq(0.0)));
        assertEquals(20, JackInTheBox.roll(1, seq(0.0499)));
        assertEquals(1, JackInTheBox.roll(1, seq(0.05, 0.0)), "5% exactly is not a jackpot");
        assertTrue(JackInTheBox.isJackpot(20));
        assertFalse(JackInTheBox.isJackpot(9));
    }

    @Test
    void theOrdinaryPayoutsFollowTheCumulativeWeights() {
        // weights 14 14 14 14 12 10 8 8 6 -> boundaries 14 28 42 56 68 78 86 94 100
        int[] first = {0, 14, 28, 42, 56, 68, 78, 86, 94};
        for (int i = 0; i < 9; i++) {
            // the draw is turned into an integer 0..99 by floor(x * 100), so aim at the middle of the integer to stay clear of rounding
            assertEquals(i + 1, JackInTheBox.roll(1, seq(0.5, (first[i] + 0.5) / 100.0)), "first value of payout " + (i + 1));
            int last = i == 8 ? 99 : first[i + 1] - 1;
            assertEquals(i + 1, JackInTheBox.roll(1, seq(0.5, (last + 0.5) / 100.0)), "last value of payout " + (i + 1));
        }
    }

    @Test
    void theFloorRaisesTheMinimumAndIsClamped() {
        assertEquals(1, JackInTheBox.roll(1, seq(0.5, 0.0)));
        assertEquals(4, JackInTheBox.roll(4, seq(0.5, 0.0)), "a floor of 4 turns a 1 into a 4");
        assertEquals(6, JackInTheBox.roll(4, seq(0.5, 0.75)), "a floor does not lower a better roll");
        assertEquals(1, JackInTheBox.roll(-3, seq(0.5, 0.0)), "a negative floor reads as 1");
        assertEquals(9, JackInTheBox.roll(500, seq(0.5, 0.0)), "a silly floor is clamped to the top of the ordinary range");
        assertEquals(20, JackInTheBox.roll(500, seq(0.01)), "the jackpot is the jackpot whatever the floor");
    }

    @Test
    void aLongSeededRunHasTheDesignedShape() {
        Random random = new Random(1_62_0L);
        Dice d = random::nextDouble;
        int n = 400_000, jackpots = 0;
        long sum = 0;
        int min = 99, max = 0;
        for (int i = 0; i < n; i++) {
            int v = JackInTheBox.roll(1, d);
            if (v == 20) jackpots++;
            else assertTrue(v >= 1 && v <= 9, "an ordinary payout is 1 to 9: " + v);
            sum += v;
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        assertEquals(1, min, "the floor is 1 and it does occur");
        assertEquals(20, max);
        double sigma = Math.sqrt(n * 0.05 * 0.95);
        assertTrue(Math.abs(jackpots - n * 0.05) < 5 * sigma, "jackpots " + jackpots + " of " + n);
        double mean = (double) sum / n;
        assertTrue(Math.abs(mean - JackInTheBox.mean()) < 0.05, "mean " + mean + " vs " + JackInTheBox.mean());
    }

    @Test
    void theExactMeanIsAboutFive() {
        double m = JackInTheBox.mean();
        assertTrue(m > 4.9 && m < 5.3, "mean " + m);
    }

    @Test
    void everyOrdinaryPayoutCanHappenAndTheLowOnesAreLikelier() {
        Random random = new Random(7L);
        Dice d = random::nextDouble;
        int[] count = new int[21];
        for (int i = 0; i < 100_000; i++) count[JackInTheBox.roll(1, d)]++;
        for (int v = 1; v <= 9; v++) assertTrue(count[v] > 0, "payout " + v + " never came up");
        assertTrue(count[1] > count[9] * 2, "1 is likelier than 9: " + count[1] + " vs " + count[9]);
        for (int v = 10; v <= 19; v++) assertEquals(0, count[v], "nothing between 10 and 19: " + v);
    }
}
