package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Kind;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** How the swarm is brought in: the cap, the waves, the kinds, the places. */
class HiveWavesTest {
    @Test
    void theKindsFollowTheirWeightsAndEveryKindComes() {
        Map<Kind, Integer> n = new EnumMap<>(Kind.class);
        int total = 10_000;
        for (int i = 0; i < total; i++) n.merge(HiveWaves.kindFor((i + 0.5) / total), 1, Integer::sum);
        assertEquals(5, n.size());
        assertEquals(3400, n.get(Kind.ORC), 5);
        assertEquals(2400, n.get(Kind.LIZARDMAN), 5);
        assertEquals(2400, n.get(Kind.SLIME), 5);
        assertEquals(1000, n.get(Kind.GOBLIN), 5);
        assertEquals(800, n.get(Kind.HOUND), 5);
        assertSame(Kind.ORC, HiveWaves.kindFor(0.0));
        assertSame(Kind.HOUND, HiveWaves.kindFor(0.99999));
        assertSame(Kind.HOUND, HiveWaves.kindFor(5.0), "out of range is clamped");
        assertSame(Kind.ORC, HiveWaves.kindFor(-1.0));
    }

    @Test
    void wavesComeOnlyWhilePlayersAreInTheVillageAndOnlyBelowTheCap() {
        assertEquals(0, HiveWaves.due(10, 5000, Long.MIN_VALUE, 0), "nobody in the village");
        assertEquals(0, HiveWaves.due(HiveWaves.CAP, 5000, Long.MIN_VALUE, 2), "at the cap");
        assertEquals(0, HiveWaves.due(HiveWaves.CAP + 5, 5000, Long.MIN_VALUE, 2));
        assertEquals(HiveWaves.WAVE_SIZE, HiveWaves.due(10, 5000, Long.MIN_VALUE, 1), "the first wave is at once");
        assertEquals(HiveWaves.WAVE_SIZE, HiveWaves.due(HiveWaves.CAP - HiveWaves.WAVE_SIZE, 5000, Long.MIN_VALUE, 1));
        assertEquals(3, HiveWaves.due(HiveWaves.CAP - 3, 5000, Long.MIN_VALUE, 1), "a short wave near the cap");
    }

    @Test
    void wavesAreSpacedOut() {
        assertEquals(0, HiveWaves.due(10, 1000 + HiveWaves.WAVE_PERIOD - 1, 1000, 1));
        assertEquals(HiveWaves.WAVE_SIZE, HiveWaves.due(10, 1000 + HiveWaves.WAVE_PERIOD, 1000, 1));
        assertEquals(HiveWaves.WAVE_SIZE, HiveWaves.due(10, 50, 1000, 1), "a clock that went back does not hold the wave off");
    }

    @Test
    void theCapHoldsOverAWholeSessionOfWaves() {
        int alive = 0;
        long last = Long.MIN_VALUE;
        int peak = 0;
        for (long t = 0; t < 200_000; t += 20) {
            int make = HiveWaves.due(alive, t, last, 1);
            if (make > 0) {
                last = t;
                alive += make;
            }
            if (t % 300 == 0 && alive > 0) alive -= Math.min(alive, 5);              // the players kill five now and then
            peak = Math.max(peak, alive);
        }
        assertTrue(peak <= HiveWaves.CAP, "never more than the cap alive: " + peak);
        assertTrue(HiveWaves.FIRST <= HiveWaves.CAP);
    }

    @Test
    void aTrapBringsAFewMoreAndNeverFarPastTheCap() {
        assertEquals(HiveWaves.AMBUSH_MIN, HiveWaves.ambushSize(10, 0));
        assertEquals(HiveWaves.AMBUSH_MIN, HiveWaves.ambushSize(10, 1));
        assertEquals(4, HiveWaves.ambushSize(10, 2));
        assertEquals(HiveWaves.AMBUSH_MAX, HiveWaves.ambushSize(10, 9));
        assertEquals(HiveWaves.AMBUSH_MAX, HiveWaves.ambushSize(HiveWaves.CAP - 10, 9));
        assertEquals(HiveWaves.AMBUSH_MAX, HiveWaves.ambushSize(HiveWaves.CAP, 9), "at the cap, a trap may still bring its few");
        assertEquals(3, HiveWaves.ambushSize(HiveWaves.CAP + 3, 9), "but only up to the overflow past the cap");
        assertEquals(0, HiveWaves.ambushSize(HiveWaves.CAP + HiveWaves.OVERFLOW, 9));
        assertEquals(0, HiveWaves.ambushSize(HiveWaves.CAP + 50, 9));
    }

    @Test
    void creaturesAppearOnlyWellAwayFromThePlayers() {
        double[][] spots = new double[30][];
        for (int i = 0; i < spots.length; i++) spots[i] = new double[]{i * 3.0, 0};
        double[][] players = {{0, 0}, {60, 0}};
        List<Integer> picked = HiveWaves.pickSpots(spots, players, 8, 99L);
        assertEquals(8, picked.size());
        Set<Integer> distinct = new HashSet<>(picked);
        assertEquals(8, distinct.size(), "each place once");
        for (int i : picked) {
            for (double[] p : players) assertTrue(Math.hypot(p[0] - spots[i][0], p[1] - spots[i][1]) >= HiveWaves.SAFE_DIST, "spot " + i);
        }
        assertEquals(picked, HiveWaves.pickSpots(spots, players, 8, 99L), "the same seed gives the same choice");
        assertNotEquals(picked, HiveWaves.pickSpots(spots, players, 8, 100L), "another seed, another order");
    }

    @Test
    void fewerComeIfFewerQualify() {
        double[][] spots = {{0, 0}, {5, 5}, {100, 100}};
        assertEquals(List.of(2), HiveWaves.pickSpots(spots, new double[][]{{2, 2}}, 8, 1L));
        assertTrue(HiveWaves.pickSpots(spots, new double[][]{{50, 50}}, 8, 1L).size() == 3 || HiveWaves.pickSpots(spots, new double[][]{{50, 50}}, 8, 1L).size() == 2);
        assertTrue(HiveWaves.pickSpots(spots, new double[][]{{0, 0}, {100, 100}, {5, 5}}, 8, 1L).isEmpty());
        assertTrue(HiveWaves.pickSpots(new double[0][], new double[0][], 5, 1L).isEmpty());
        assertTrue(HiveWaves.pickSpots(spots, new double[0][], 0, 1L).isEmpty());
        assertEquals(3, HiveWaves.pickSpots(spots, new double[0][], 99, 1L).size(), "with nobody about, every place is fair");
        assertTrue(HiveWaves.pickSpots(spots, new double[0][], -2, 1L).isEmpty());
    }
}
