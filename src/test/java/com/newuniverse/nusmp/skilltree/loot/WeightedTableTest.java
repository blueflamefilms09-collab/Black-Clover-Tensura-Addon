package com.newuniverse.nusmp.skilltree.loot;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: {@link WeightedTable} is the pick behind every route book of the dungeon with 283 lines, replacing the per-drop walk of {@link RoutePick}; the owner's rule is "uniformly weighted by
 * loot_weight". So it is tested against the thing it replaces: for tens of thousands of random weight sets and dice values it must answer exactly what {@code RoutePick} answers (so the proportions
 * already approved do not move), at the boundaries between weights, with zero and negative weights (never chosen), nothing positive (-1, never a guessed index), and a dice of exactly 1.0 (must
 * not leave the array). A seeded run over a 283-line table shaped like the real one (the 8 hand-built weights, 275 lines of weight 1) checks the proportions, and the avoiding pick is tested for
 * the promise the pity rule leans on: never an avoided entry when any other exists, and still an answer when all are avoided.
 */
class WeightedTableTest {
    private static Dice fixed(double v) { return () -> v; }

    @Test
    void emptyAndZeroTablesAnswerMinusOne() {
        assertEquals(-1, WeightedTable.of(new int[0]).pick(fixed(0.5)));
        assertEquals(-1, WeightedTable.of(new int[] {0, -3, 0}).pick(fixed(0.5)));
        assertEquals(-1, WeightedTable.of(null).pick(fixed(0.5)));
        assertEquals(-1, WeightedTable.EMPTY.pick(fixed(0.5)));
        assertEquals(-1, WeightedTable.of(new int[] {1}).pick(null));
        assertTrue(WeightedTable.of(new int[] {0, 0}).isEmpty());
        assertEquals(0, WeightedTable.of(new int[] {0, 0}).total());
    }

    @Test
    void boundariesBetweenWeights() {
        WeightedTable t = WeightedTable.of(new int[] {1, 0, 3});
        assertEquals(2, t.size());
        assertEquals(4, t.total());
        assertEquals(0, t.pick(fixed(0.0)));
        assertEquals(0, t.pick(fixed(0.24)));
        assertEquals(2, t.pick(fixed(0.25)), "the zero-weight entry in the middle is skipped");
        assertEquals(2, t.pick(fixed(0.99)));
        assertEquals(2, t.pick(fixed(1.0)), "a dice that returns exactly 1.0 must stay in range");
        assertEquals(1, t.weightOf(0));
        assertEquals(0, t.weightOf(1));
        assertEquals(3, t.weightOf(2));
        assertEquals(0, t.weightOf(99));
    }

    @Test
    void itAnswersExactlyWhatRoutePickAnswers() {
        Random r = new Random(20260418L);
        for (int round = 0; round < 3000; round++) {
            int n = 1 + r.nextInt(40);
            int[] w = new int[n];
            for (int i = 0; i < n; i++) w[i] = r.nextInt(8) - 2;                 // -2 .. 5: negatives, zeros and positives
            WeightedTable t = WeightedTable.of(w);
            for (int k = 0; k < 12; k++) {
                double d = k == 0 ? 0.0 : k == 1 ? 1.0 : k == 2 ? 0.9999999999 : r.nextDouble();
                assertEquals(RoutePick.pick(w, fixed(d)), t.pick(fixed(d)), "round " + round + " dice " + d);
            }
        }
    }

    @Test
    void theRealShapeOf283LinesIsPickedByWeight() {
        int[] w = new int[283];
        int[] hand = {3, 2, 2, 2, 2, 2, 1, 2};                                  // the shipped routes' loot_weight values
        for (int i = 0; i < 283; i++) w[i] = i < hand.length ? hand[i] : 1;
        WeightedTable t = WeightedTable.of(w);
        assertEquals(283, t.size());
        long total = 0;
        for (int x : w) total += x;
        assertEquals(total, t.total());
        Random r = new Random(77);
        Dice dice = r::nextDouble;
        int n = 600_000;
        int[] hits = new int[283];
        for (int i = 0; i < n; i++) hits[t.pick(dice)]++;
        for (int i : new int[] {0, 1, 6, 8, 100, 282}) {
            double p = w[i] / (double) total, sigma = Math.sqrt(n * p * (1 - p));
            assertTrue(Math.abs(hits[i] - n * p) < 5 * sigma, "line " + i + " (weight " + w[i] + "): " + hits[i] + " hits, expected " + (n * p) + " +- " + 5 * sigma);
        }
        for (int i = 8; i < 283; i++) assertTrue(hits[i] > 0, "a weight-1 line was never picked in " + n + " draws: " + i);
    }

    @Test
    void zeroWeightLinesNeverDropAndOthersAreUniform() {
        int[] w = {0, 5, 0, 5, 0, 5, 0};
        WeightedTable t = WeightedTable.of(w);
        Random r = new Random(3);
        int[] hits = new int[w.length];
        for (int i = 0; i < 30_000; i++) hits[t.pick(r::nextDouble)]++;
        assertEquals(0, hits[0] + hits[2] + hits[4] + hits[6]);
        for (int i : new int[] {1, 3, 5}) assertTrue(Math.abs(hits[i] - 10_000) < 5 * Math.sqrt(30_000 / 3.0 * 2 / 3), "uniform: " + hits[i]);
    }

    @Test
    void avoidingNeverReturnsAnAvoidedEntryWhileAnotherExists() {
        int[] w = new int[283];
        for (int i = 0; i < w.length; i++) w[i] = 1 + i % 3;
        WeightedTable t = WeightedTable.of(w);
        Random r = new Random(5);
        // the player owns 282 of 283 lines: only line 200 is acceptable; the plain draw hits it about once in 560, so the redraws almost never find it and the scan must
        for (int i = 0; i < 2000; i++) assertEquals(200, t.pickAvoiding(r::nextDouble, k -> k != 200, 12));
        // owning nothing: the avoiding pick is the plain pick
        for (int i = 0; i < 200; i++) {
            double d = r.nextDouble();
            assertEquals(t.pick(fixed(d)), t.pickAvoiding(fixed(d), k -> false, 12));
        }
    }

    @Test
    void whenEveryEntryIsAvoidedTheFirstPickStands() {
        WeightedTable t = WeightedTable.of(new int[] {2, 2, 2});
        assertEquals(1, t.pickAvoiding(fixed(0.5), k -> true, 4), "nothing better exists: the plain pick is kept (a duplicate book is still worth Skill Points)");
        assertEquals(-1, WeightedTable.EMPTY.pickAvoiding(fixed(0.5), k -> true, 4));
    }

    @Test
    void theRedrawsKeepTheWeights() {
        // two acceptable lines of weight 1 and 3 among avoided ones: the accepted ones keep their 1:3 ratio through the redraws
        int[] w = {10, 1, 10, 3, 10};
        WeightedTable t = WeightedTable.of(w);
        Random r = new Random(9);
        int a = 0, b = 0, n = 40_000;
        for (int i = 0; i < n; i++) {
            int p = t.pickAvoiding(r::nextDouble, k -> k != 1 && k != 3, 60);
            if (p == 1) a++;
            else if (p == 3) b++;
            else assertTrue(false, "an avoided line was returned: " + p);
        }
        assertTrue(Math.abs(b / (double) (a + b) - 0.75) < 0.02, "expected 3:1, got " + a + ":" + b);
    }
}
