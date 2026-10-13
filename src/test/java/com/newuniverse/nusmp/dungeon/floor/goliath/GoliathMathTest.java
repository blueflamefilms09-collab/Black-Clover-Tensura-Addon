package com.newuniverse.nusmp.dungeon.floor.goliath;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The maths of the Goliath's moves: ring waves, the line, the fist's mark, the rubble's circles and the damage budget. */
class GoliathMathTest {
    // ---------------------------------------------------------------- ring waves
    @Test
    void aRingWaveGrowsAtItsSpeedAndPassesEveryoneInReachOnce() {
        assertEquals(GoliathTable.WAVE_START, GoliathMath.waveRadius(0, 1.1), 1e-9);
        assertEquals(GoliathTable.WAVE_START + 10 * 1.1, GoliathMath.waveRadius(10, 1.1), 1e-9);
        assertEquals(GoliathTable.WAVE_START, GoliathMath.waveRadius(-5, 1.1), 1e-9, "no negative age");
        for (double speed : new double[]{GoliathTable.WAVE_SPEED, GoliathTable.QUAKE_SPEED}) {
            for (double reach : new double[]{7.0, 8.75, 30.0, 34.0}) {
                int ticks = GoliathMath.waveTicks(reach, speed);
                for (double dist = 0; dist <= reach; dist += 0.25) {
                    int hits = 0;
                    for (int age = 0; age < ticks; age++) if (GoliathMath.waveHits(dist, GoliathMath.waveRadius(age, speed))) hits++;
                    assertTrue(hits >= 1, "something at " + dist + " of " + reach + " is passed by the wave at speed " + speed);
                    assertTrue(hits <= 3, "and not for long: " + hits + " ticks");
                }
            }
        }
    }

    @Test
    void aWaveNeverOutrunsItsTicksAndIsJumpedByFeetOffTheFloor() {
        assertTrue(GoliathMath.waveRadius(GoliathMath.waveTicks(34, 1.1) - 1, 1.1) >= 34, "the last tick reaches the reach");
        assertTrue(GoliathMath.waveRadius(GoliathMath.waveTicks(34, 1.1) - 2, 1.1) < 34 + GoliathTable.WAVE_THICK + 1.1);
        assertFalse(GoliathMath.jumpsWave(0.0));
        assertFalse(GoliathMath.jumpsWave(0.75), "exactly at the limit is not a jump");
        assertTrue(GoliathMath.jumpsWave(0.76));
        assertTrue(GoliathMath.waveHits(10.0, 10.0));
        assertTrue(GoliathMath.waveHits(10.0, 11.29));
        assertFalse(GoliathMath.waveHits(10.0, 11.31));
        assertTrue(GoliathMath.inBand(0) && GoliathMath.inBand(2.6) && GoliathMath.inBand(-1.5));
        assertFalse(GoliathMath.inBand(2.7), "someone up on a gallery is out of a ring on the floor");
        assertFalse(GoliathMath.inBand(-1.6));
        assertFalse(GoliathMath.inBand(6.0));
    }

    @Test
    void theTellShowsTheRingGrowingToItsReach() {
        assertEquals(1.5, GoliathMath.tellRadius(8, 36, 36), 1e-9, "at the start only the middle");
        assertEquals(8.0, GoliathMath.tellRadius(8, 0, 36), 1e-9, "at the end the whole reach");
        double last = 0;
        for (int left = 36; left >= 0; left--) {
            double r = GoliathMath.tellRadius(8, left, 36);
            assertTrue(r >= last, "it only grows");
            last = r;
        }
        assertEquals(8.0, GoliathMath.tellRadius(8, -4, 36), 1e-9, "no overshoot");
    }

    // ---------------------------------------------------------------- the line
    @Test
    void theLineRunsPastTheFoeAndHitsOnlyWhoStandsOnIt() {
        assertEquals(6.0, GoliathMath.lineLength(0.0), 1e-9);
        assertEquals(14.0, GoliathMath.lineLength(10.0), 1e-9);
        assertEquals(30.0, GoliathMath.lineLength(60.0), 1e-9);
        assertEquals(15, GoliathMath.stretches(30.0));
        assertEquals(3, GoliathMath.stretches(6.0));
        assertEquals(2.0, GoliathMath.stretchAlong(1), 1e-9);
        assertEquals(30.0, GoliathMath.stretchAlong(15), 1e-9);
        assertEquals(2, GoliathMath.stretchDelay(1));
        assertEquals(30, GoliathMath.stretchDelay(15));
        double[] f = GoliathMath.lineFrame(10, 20, 0, 1, 12, 27);              // a line running along +z from (10, 20): a point at (12, 27)
        assertEquals(7.0, f[0], 1e-9, "seven blocks along");
        assertEquals(Math.abs(f[1]), 2.0, 1e-9, "two blocks to the side");
        double[] g = GoliathMath.lineFrame(0, 0, 0.6, 0.8, 3, 4);
        assertEquals(5.0, g[0], 1e-9);
        assertEquals(0.0, g[1], 1e-9, "dead on the line");
        assertTrue(GoliathMath.stretchHits(8.0, 1.0, 8.0, GoliathTable.LINE_HALF_WIDTH));
        assertTrue(GoliathMath.stretchHits(9.2, -1.7, 8.0, GoliathTable.LINE_HALF_WIDTH));
        assertFalse(GoliathMath.stretchHits(9.3, 0.0, 8.0, GoliathTable.LINE_HALF_WIDTH), "a stretch is two blocks long");
        assertFalse(GoliathMath.stretchHits(8.0, 1.8, 8.0, GoliathTable.LINE_HALF_WIDTH), "and three and a half wide");
        // a foe standing still on the line is hit by exactly one stretch (they are two blocks apart and each covers 2.4)
        for (double along = 1.0; along <= 29.0; along += 0.1) {
            int n = 0;
            for (int k = 1; k <= 15; k++) if (GoliathMath.stretchHits(along, 0, GoliathMath.stretchAlong(k), 1.7)) n++;
            assertTrue(n >= 1 && n <= 2, "at " + along + ": " + n);
        }
    }

    @Test
    void theFistLandsOnTheFoeWithinItsReach() {
        double[] on = GoliathMath.fistMark(0, 0, 4, 3, 11, 0, 1);
        assertArrayEquals(new double[]{4, 3}, on, 1e-9, "on the foe");
        double[] far = GoliathMath.fistMark(0, 0, 30, 40, 11, 0, 1);
        assertEquals(11.0, Math.hypot(far[0], far[1]), 1e-9, "at the edge of the reach");
        assertEquals(30.0 / 50.0, far[0] / 11.0, 1e-9, "on the line to the foe");
        double[] under = GoliathMath.fistMark(5, 5, 5, 5, 11, 1, 0);
        assertArrayEquals(new double[]{6, 5}, under, 1e-9, "a foe on top of the giant is struck in front of it");
        double[] blind = GoliathMath.fistMark(5, 5, 5, 5, 11, 0, 0);
        assertArrayEquals(new double[]{5, 6}, blind, 1e-9);
    }

    // ---------------------------------------------------------------- the rubble
    @Test
    void theRubbleCountGrowsWithFoesAndPowerAndStaysBounded() {
        assertEquals(3, GoliathMath.rubbleCount(0, 1));
        assertEquals(3, GoliathMath.rubbleCount(1, 1));
        assertEquals(4, GoliathMath.rubbleCount(2, 1));
        assertEquals(5, GoliathMath.rubbleCount(1, 3));
        assertEquals(8, GoliathMath.rubbleCount(20, 4));
        assertEquals(3, GoliathMath.rubbleCount(-5, 0));
        for (int foes = 0; foes < 12; foes++) for (int slots = 0; slots < 6; slots++) {
            int c = GoliathMath.rubbleCount(foes, slots);
            assertTrue(c >= 3 && c <= 8);
        }
    }

    @Test
    void theRubbleFallsOnEveryFoeAndOnTheRestOfThePitApart() {
        Random rnd = new Random(5);
        for (int trial = 0; trial < 400; trial++) {
            List<double[]> foes = new ArrayList<>();
            int nf = rnd.nextInt(6);
            for (int i = 0; i < nf; i++) {
                double a = rnd.nextDouble() * Math.PI * 2, d = rnd.nextDouble() * 26;
                foes.add(new double[]{Math.cos(a) * d, Math.sin(a) * d});
            }
            int count = GoliathMath.rubbleCount(nf, 1 + rnd.nextInt(4));
            double bx = rnd.nextDouble() * 10 - 5, bz = rnd.nextDouble() * 10 - 5;
            List<double[]> spots = GoliathMath.rubbleSpots(foes, count, 0, 0, 28, 4.0, bx, bz, rnd.nextDouble() * 6);
            assertTrue(spots.size() <= count, "never more than asked");
            assertTrue(spots.size() >= Math.min(count, Math.min(nf, 1)), "at least a circle for the first foe");
            for (int i = 0; i < spots.size(); i++) {
                for (int j = i + 1; j < spots.size(); j++) assertTrue(Math.hypot(spots.get(i)[0] - spots.get(j)[0], spots.get(i)[1] - spots.get(j)[1]) >= 4.0 - 1e-9, "circles keep apart");
            }
            // every foe is under a circle (its own, or one within the gap) while there is room for as many circles as foes
            if (nf <= count) {
                for (double[] f : foes) {
                    boolean covered = false;
                    for (double[] s : spots) if (Math.hypot(s[0] - f[0], s[1] - f[1]) < 4.0 + 1e-9) covered = true;
                    assertTrue(covered, "a foe is without a circle");
                }
            }
            // the circles that were not put on a foe lie inside the pit and away from the giant
            for (int i = Math.min(nf, spots.size()); i < spots.size(); i++) {
                double[] s = spots.get(i);
                if (foes.stream().anyMatch(f -> f[0] == s[0] && f[1] == s[1])) continue;
                assertTrue(Math.hypot(s[0], s[1]) <= 25.0 + 1e-9, "inside the pit");
                assertTrue(Math.hypot(s[0] - bx, s[1] - bz) >= 4.0 - 1e-9, "away from the giant");
            }
        }
        assertTrue(GoliathMath.rubbleSpots(null, 0, 0, 0, 28, 4, 0, 0, 0).isEmpty());
        assertEquals(3, GoliathMath.rubbleSpots(List.of(), 3, 0, 0, 28, 4, 0, 0, 0).size(), "with no foes the circles are still laid out");
    }

    // ---------------------------------------------------------------- the budget
    @Test
    void aBudgetStopsAMoveAtItsShareOfAVictimsHealth() {
        GoliathMath.DamageBudget b = new GoliathMath.DamageBudget(0.55);
        double maxHealth = 20, hit = 8;
        double total = 0;
        for (int i = 0; i < 5; i++) {
            double scale = b.allow(7, maxHealth, hit);
            assertTrue(scale >= 0 && scale <= 1);
            b.spend(7, hit * scale);
            total += hit * scale;
        }
        assertEquals(0.55 * maxHealth, total, 1e-9, "never more than the share, and exactly the share for hits that would exceed it");
        assertEquals(0.0, b.allow(7, maxHealth, 1.0), "nothing is left");
        assertEquals(1.0, b.allow(8, maxHealth, 1.0), "another victim has a budget of their own");
        assertEquals(0.55, b.share(), 1e-9);
        assertEquals(11.0, b.spent(7), 1e-9);
        assertEquals(0.0, b.spent(99), 1e-9);
    }

    @Test
    void aBudgetNeverGoesNegativeOrNaN() {
        GoliathMath.DamageBudget b = new GoliathMath.DamageBudget(0.7);
        assertEquals(0.0, b.allow(1, 0, 5));
        assertEquals(0.0, b.allow(1, 20, 0));
        assertEquals(0.0, b.allow(1, 20, -3));
        assertEquals(0.0, b.allow(1, Double.NaN, 5));
        assertEquals(0.0, b.allow(1, 20, Double.NaN));
        b.spend(1, -5);
        b.spend(1, Double.NaN);
        assertEquals(0.0, b.spent(1));
        assertEquals(1.0, new GoliathMath.DamageBudget(5).share(), "a share is clamped to 0..1");
        assertEquals(0.0, new GoliathMath.DamageBudget(-2).share());
        // a lot of small hits add up to the share and no more
        GoliathMath.DamageBudget c = new GoliathMath.DamageBudget(0.7);
        double dealt = 0;
        for (int i = 0; i < 100; i++) {
            double s = c.allow(3, 40, 2.5);
            c.spend(3, 2.5 * s);
            dealt += 2.5 * s;
        }
        assertEquals(28.0, dealt, 1e-9);
    }
}
