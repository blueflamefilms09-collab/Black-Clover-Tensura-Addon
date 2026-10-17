package com.newuniverse.nusmp.avatar.aptitude;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The free aptitude roll: base odds are the rarity weights, luck shifts them towards the rare classes, and the result is always a member of the pool. */
class AptitudeRollTest {
    private static Aptitude apt(String id, Aptitude.Rarity r) { return new Aptitude(id, id, r, 0, "", List.of()); }

    /** Two aptitudes of every rarity, so every class is present. */
    private static List<Aptitude> full() {
        List<Aptitude> l = new ArrayList<>();
        for (Aptitude.Rarity r : Aptitude.Rarity.values()) { l.add(apt(r.name().toLowerCase() + "_a", r)); l.add(apt(r.name().toLowerCase() + "_b", r)); }
        return l;
    }

    private static double sum(double[] d) { double s = 0; for (double v : d) s += v; return s; }

    @Test
    void withNoLuckTheOddsAreTheRarityWeights() {
        double[] o = AptitudeRoll.odds(full(), 0.0, 0.06);
        assertEquals(0.60, o[Aptitude.Rarity.COMMON.ordinal()], 1e-9);
        assertEquals(0.25, o[Aptitude.Rarity.UNCOMMON.ordinal()], 1e-9);
        assertEquals(0.10, o[Aptitude.Rarity.RARE.ordinal()], 1e-9);
        assertEquals(0.04, o[Aptitude.Rarity.EPIC.ordinal()], 1e-9);
        assertEquals(0.01, o[Aptitude.Rarity.LEGENDARY.ordinal()], 1e-9);
        assertEquals(1.0, sum(o), 1e-9);
    }

    @Test
    void luckLiftsTheRareClassesAndLowersTheCommonOnes() {
        double[] zero = AptitudeRoll.odds(full(), 0.0, 0.06), lucky = AptitudeRoll.odds(full(), 10.0, 0.06), luckier = AptitudeRoll.odds(full(), 25.0, 0.06);
        int c = Aptitude.Rarity.COMMON.ordinal(), l = Aptitude.Rarity.LEGENDARY.ordinal(), e = Aptitude.Rarity.EPIC.ordinal();
        assertTrue(lucky[l] > zero[l] * 4, "legendary odds grow a lot: " + lucky[l]);
        assertTrue(luckier[l] > lucky[l]);
        assertTrue(lucky[e] > zero[e]);
        assertTrue(lucky[c] < zero[c] && luckier[c] < lucky[c]);
        assertEquals(1.0, sum(lucky), 1e-9);
        assertEquals(1.0, sum(luckier), 1e-9);
        double[] unlucky = AptitudeRoll.odds(full(), -10.0, 0.06);
        assertTrue(unlucky[l] < zero[l] && unlucky[c] > zero[c], "bad luck pushes towards the common");
    }

    @Test
    void strengthZeroMakesLuckDoNothing() {
        assertEquals(AptitudeRoll.odds(full(), 0.0, 0.0)[4], AptitudeRoll.odds(full(), 30.0, 0.0)[4], 1e-12);
    }

    @Test
    void aMissingClassGivesItsShareToTheOthers() {
        List<Aptitude> pool = List.of(apt("c", Aptitude.Rarity.COMMON), apt("l", Aptitude.Rarity.LEGENDARY));
        double[] o = AptitudeRoll.odds(pool, 0.0, 0.06);
        assertEquals(60.0 / 61.0, o[0], 1e-9);
        assertEquals(0.0, o[1], 1e-12);
        assertEquals(1.0 / 61.0, o[4], 1e-9);
        assertEquals(1.0, sum(o), 1e-9);
    }

    @Test
    void anEmptyPoolHasNoOddsAndNoResult() {
        assertEquals(0.0, sum(AptitudeRoll.odds(List.of(), 5.0, 0.06)), 1e-12);
        assertNull(AptitudeRoll.pick(List.of(), 5.0, 0.06, new Random(1)));
        assertEquals(0.0, sum(AptitudeRoll.odds(null, 5.0, 0.06)), 1e-12);
    }

    @Test
    void theRollFollowsTheOddsOverManyDraws() {
        List<Aptitude> pool = full();
        for (double luck : new double[]{0.0, 12.0}) {
            Random rng = new Random(7);
            int[] hits = new int[5];
            int n = 200_000;
            for (int i = 0; i < n; i++) hits[AptitudeRoll.pick(pool, luck, 0.06, rng).rarity().ordinal()]++;
            double[] want = AptitudeRoll.odds(pool, luck, 0.06);
            for (int r = 0; r < 5; r++) assertEquals(want[r], hits[r] / (double) n, 0.006, "luck " + luck + " class " + r);
        }
    }

    @Test
    void insideAClassEveryAptitudeIsEquallyLikely() {
        List<Aptitude> pool = new ArrayList<>();
        pool.add(apt("a", Aptitude.Rarity.RARE));
        pool.add(apt("b", Aptitude.Rarity.RARE));
        pool.add(apt("c", Aptitude.Rarity.RARE));
        pool.add(apt("only_common", Aptitude.Rarity.COMMON));
        Random rng = new Random(3);
        int a = 0, b = 0, c = 0, n = 120_000;
        for (int i = 0; i < n; i++) {
            String id = AptitudeRoll.pick(pool, 0.0, 0.0, rng).id();
            if (id.equals("a")) a++; else if (id.equals("b")) b++; else if (id.equals("c")) c++;
        }
        assertEquals(a, b, n * 0.02);
        assertEquals(b, c, n * 0.02);
    }

    @Test
    void anAptitudeRemovedFromThePoolNeverComesUp() {
        List<Aptitude> pool = full();
        pool.removeIf(a -> a.id().equals("legendary_a"));
        Random rng = new Random(9);
        for (int i = 0; i < 20_000; i++) assertNotEquals("legendary_a", AptitudeRoll.pick(pool, 40.0, 0.2, rng).id());
    }

    @Test
    void absurdInputsAreClamped() {
        assertEquals(AptitudeRoll.odds(full(), 40.0, 0.06)[4], AptitudeRoll.odds(full(), 1e9, 0.06)[4], 1e-12);
        assertEquals(AptitudeRoll.odds(full(), 0.0, 0.06)[4], AptitudeRoll.odds(full(), Double.NaN, 0.06)[4], 1e-12);
        assertEquals(AptitudeRoll.odds(full(), -20.0, 0.06)[4], AptitudeRoll.odds(full(), -1e9, 0.06)[4], 1e-12);
        assertEquals(1.0, sum(AptitudeRoll.odds(full(), 30.0, 50.0)), 1e-9);
        assertNotNull(AptitudeRoll.pick(full(), Double.NaN, Double.NaN, new Random(1)));
    }
}
