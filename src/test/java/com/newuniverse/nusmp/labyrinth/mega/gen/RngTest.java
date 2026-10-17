package com.newuniverse.nusmp.labyrinth.mega.gen;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the dungeon must be the same dungeon on every machine and after every restart (a half-placed sector is repaired by planning it again from its seed),
 * so the random generator is hand-written and its output is pinned to values computed independently in Python from the published SplitMix64 algorithm,
 * not to whatever the JDK would give. The weighted draw is tested for the property the pools rely on: the element with the larger weight comes first more
 * often, in proportion; every element comes out exactly once; and the pool's empty element ends the sequence when it comes up.
 */
class RngTest {

    @Test
    void outputIsPinnedToTheReferenceSplitMix64() {
        Rng r = new Rng(0);
        assertEquals(-2152535657050944081L, r.nextLong(), "the first SplitMix64 output for seed 0 is 0xe220a8397b1dcdaf");
        assertEquals(7960286522194355700L, r.nextLong());
        assertEquals(487617019471545679L, r.nextLong());
        Rng s = new Rng(1234567);
        assertEquals(6457827717110365317L, s.nextLong());
        assertEquals(3203168211198807973L, s.nextLong());
        assertEquals(-8629252141511181193L, s.nextLong());
    }

    @Test
    void mixingIsPinnedAndNotSymmetric() {
        assertEquals(-5278393320434088484L, Rng.mix(5));
        assertEquals(-643518723203083613L, Rng.mix(1, 2));
        assertEquals(-6227511733432959707L, Rng.mix(2, 1));
        assertNotEquals(Rng.mix(1, 2), Rng.mix(2, 1));
    }

    @Test
    void sameSeedSameSequenceDifferentSeedDifferent() {
        Rng a = new Rng(42), b = new Rng(42), c = new Rng(43);
        boolean differs = false;
        for (int i = 0; i < 100; i++) {
            long x = a.nextLong();
            assertEquals(x, b.nextLong());
            if (x != c.nextLong()) differs = true;
        }
        assertTrue(differs);
    }

    @Test
    void nextIntStaysInRangeAndShuffleKeepsTheElements() {
        Rng r = new Rng(5);
        int[] hits = new int[7];
        for (int i = 0; i < 7000; i++) {
            int v = r.nextInt(7);
            assertTrue(v >= 0 && v < 7);
            hits[v]++;
        }
        for (int h : hits) assertTrue(h > 800 && h < 1200, "roughly uniform: " + h);
        List<Integer> l = new ArrayList<>();
        List<Integer> sorted = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            l.add(i);
            sorted.add(i);
        }
        new Rng(9).shuffle(l);
        Set<Integer> seen = new HashSet<>(l);
        assertEquals(50, seen.size());
        assertNotEquals(sorted, l);
        double d = new Rng(3).nextDouble();
        assertTrue(d >= 0 && d < 1);
    }

    private static PoolDef pool(int emptyWeight, int... weights) {
        List<PoolEntry> e = new ArrayList<>();
        for (int i = 0; i < weights.length; i++) e.add(new PoolEntry("t" + i, weights[i], "minecraft:empty"));
        return new PoolDef("p", e, emptyWeight, "minecraft:empty");
    }

    @Test
    void theWeightedDrawReturnsEveryElementOnceAndFavoursHeavyOnes() {
        PoolDef p = pool(0, 1, 9, 5);
        int[] first = new int[3];
        Rng rng = new Rng(1);
        for (int n = 0; n < 6000; n++) {
            WeightedDraw d = new WeightedDraw(p, false, rng);
            Set<String> seen = new HashSet<>();
            PoolEntry e;
            int idx = 0;
            while ((e = d.next()) != null) {
                assertTrue(seen.add(e.shape()), "an element comes out once");
                if (idx++ == 0) first[Integer.parseInt(e.shape().substring(1))]++;
            }
            assertEquals(3, seen.size());
        }
        // expected first-place shares 1/15, 9/15, 5/15 of 6000 = 400, 3600, 2000
        assertTrue(Math.abs(first[0] - 400) < 120, "light element first: " + first[0]);
        assertTrue(Math.abs(first[1] - 3600) < 200, "heavy element first: " + first[1]);
        assertTrue(Math.abs(first[2] - 2000) < 160, "middle element first: " + first[2]);
    }

    @Test
    void theEmptyElementEndsTheSequenceUnlessIgnored() {
        PoolDef p = pool(30, 1, 1);                         // the empty element is 15 times as likely as each template
        Rng rng = new Rng(2);
        int total = 0, drawsWithout = 0;
        for (int n = 0; n < 2000; n++) {
            WeightedDraw d = new WeightedDraw(p, false, rng);
            while (d.next() != null) total++;
            WeightedDraw ignoring = new WeightedDraw(p, true, rng);
            while (ignoring.next() != null) drawsWithout++;
        }
        assertEquals(4000, drawsWithout, "ignoring the empty element every template comes out");
        assertTrue(total < 800, "with the empty element most sequences end at once: " + total);
        assertNull(new WeightedDraw(pool(0), false, rng).next(), "an empty pool yields nothing");
    }
}
