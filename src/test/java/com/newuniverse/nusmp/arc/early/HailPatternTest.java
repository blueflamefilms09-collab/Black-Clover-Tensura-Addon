package com.newuniverse.nusmp.arc.early;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Heath's cage: always the same for a seed, always with a way through. */
class HailPatternTest {
    private static final double R = HeathTable.CAGE_RADIUS, IMPACT = HeathTable.CAGE_IMPACT, HALF = HeathTable.CAGE_CORRIDOR;

    @Test
    void theSameSeedGivesTheSameCage() {
        List<HailPattern.Strike> a = HailPattern.plan(42L, 10, -4, R, 3, 8, IMPACT, 1.0, HALF);
        List<HailPattern.Strike> b = HailPattern.plan(42L, 10, -4, R, 3, 8, IMPACT, 1.0, HALF);
        List<HailPattern.Strike> c = HailPattern.plan(43L, 10, -4, R, 3, 8, IMPACT, 1.0, HALF);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    void everyWaveIsFullInsideTheDiscAndSpacedOut() {
        for (long seed = 0; seed < 300; seed++) {
            List<HailPattern.Strike> all = HailPattern.plan(seed, 0, 0, R, 3, 8, IMPACT, seed * 0.7, HALF);
            for (int w = 0; w < 3; w++) {
                List<HailPattern.Strike> wave = HailPattern.wave(all, w);
                assertTrue(wave.size() >= 7, "seed " + seed + " wave " + w + " has " + wave.size());
                for (int i = 0; i < wave.size(); i++) {
                    HailPattern.Strike s = wave.get(i);
                    assertTrue(Math.hypot(s.x(), s.z()) <= R + 1e-9, "inside the disc");
                    for (int j = i + 1; j < wave.size(); j++) {
                        HailPattern.Strike t = wave.get(j);
                        assertTrue(Math.hypot(s.x() - t.x(), s.z() - t.z()) >= IMPACT * 1.8 - 1e-9, "icicles of one wave do not overlap");
                    }
                }
            }
        }
    }

    @Test
    void thereIsAlwaysACorridorThatNoIcicleTouches() {
        for (long seed = 0; seed < 400; seed++) {
            double opening = (seed % 8) * Math.PI / 4 + 0.1;
            double ox = Math.cos(opening), oz = Math.sin(opening);
            List<HailPattern.Strike> all = HailPattern.plan(seed * 31 + 7, 5, 5, R, 3, 10, IMPACT, opening, HALF);
            assertTrue(all.size() >= 27, "seed " + seed + " placed " + all.size());
            // the middle of the corridor is clear of every landing, at every distance out to the rim, including the very middle of the cage
            for (double d = 0; d <= R; d += 0.5) {
                double px = 5 + ox * d, pz = 5 + oz * d;
                for (HailPattern.Strike s : all) assertTrue(Math.hypot(s.x() - px, s.z() - pz) >= HALF + IMPACT - 1e-9, "seed " + seed + ": the corridor axis at " + d + " is safe");
            }
            // and so is a foe at the edge of the corridor, half way out
            double sx = 5 + ox * R / 2 - oz * HALF, sz = 5 + oz * R / 2 + ox * HALF;
            for (HailPattern.Strike s : all) assertTrue(Math.hypot(s.x() - sx, s.z() - sz) >= IMPACT - 1e-9, "the edge of the corridor is clear of every landing");
        }
    }

    @Test
    void theCorridorTestIsACapsuleRoundTheAxis() {
        double axisX = R, axisZ = 0;
        assertTrue(HailPattern.clearOfCorridor(0, -3, axisX, axisZ, 1.5, 1.4), "behind the middle but far enough");
        assertFalse(HailPattern.clearOfCorridor(0, -2.8, axisX, axisZ, 1.5, 1.4), "behind the middle and too close: the middle of the cage is protected too");
        assertFalse(HailPattern.clearOfCorridor(4, 0, axisX, axisZ, 1.5, 1.4), "on the axis");
        assertFalse(HailPattern.clearOfCorridor(4, 2.8, axisX, axisZ, 1.5, 1.4), "just inside the margin");
        assertTrue(HailPattern.clearOfCorridor(4, 2.9, axisX, axisZ, 1.5, 1.4), "at the margin");
        assertEquals(0, HailPattern.plan(1L, 0, 0, R, 0, 8, IMPACT, 0, HALF).size());
        assertEquals(0, HailPattern.plan(1L, 0, 0, R, 3, 0, IMPACT, 0, HALF).size());
    }
}
