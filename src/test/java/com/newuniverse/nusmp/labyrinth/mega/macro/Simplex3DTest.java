package com.newuniverse.nusmp.labyrinth.mega.macro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: every landmark of the Hub is shaped by this noise, so a regression in its range (a tower that grows a spike), its continuity (a wall
 * with cliffs in it), its determinism (two servers with one seed building different worlds) or its seed sensitivity (every world getting the same
 * Hub) would be invisible in the code and very visible in the game. The numbers are pinned here instead.
 */
public class Simplex3DTest {

    @Test
    public void sameSeedGivesTheSameField() {
        Simplex3D a = new Simplex3D(12345L), b = new Simplex3D(12345L);
        for (int i = 0; i < 2000; i++) {
            double x = i * 0.37 - 300, y = i * 0.11 + 5, z = 90 - i * 0.53;
            assertEquals(a.noise(x, y, z), b.noise(x, y, z), "3D sample " + i);
            assertEquals(a.noise(x, z), b.noise(x, z), "2D sample " + i);
            assertEquals(a.fbm(x, y, z, 3, 2.0, 0.5), b.fbm(x, y, z, 3, 2.0, 0.5), "fbm sample " + i);
        }
    }

    @Test
    public void everySampleStaysInsideMinusOneAndOne() {
        for (long seed : new long[] {0L, 1L, -7L, 987654321L}) {
            Simplex3D n = new Simplex3D(seed);
            double lo = 9, hi = -9;
            for (int i = 0; i < 30000; i++) {
                double x = (i * 7919L % 4001) * 0.173, y = (i * 104729L % 3001) * 0.131, z = (i * 1299709L % 5003) * 0.157;
                double v = n.noise(x, y, z), w = n.noise(x, z), f = n.fbm(x, y, z, 4, 2.0, 0.5), g = n.fbm(x, z, 3, 2.0, 0.5);
                lo = Math.min(lo, Math.min(Math.min(v, w), Math.min(f, g)));
                hi = Math.max(hi, Math.max(Math.max(v, w), Math.max(f, g)));
            }
            assertTrue(lo >= -1.0 && hi <= 1.0, "range for seed " + seed + ": " + lo + " .. " + hi);
            assertTrue(hi > 0.5 && lo < -0.5, "the field actually uses most of its range: " + lo + " .. " + hi);
        }
    }

    @Test
    public void aSmallStepMakesASmallChange() {
        Simplex3D n = new Simplex3D(42L);
        double worst3 = 0, worst2 = 0;
        for (int i = 0; i < 20000; i++) {
            double x = (i % 211) * 0.9 - 100, y = (i % 97) * 0.8, z = (i % 173) * 0.7 - 60;
            double eps = 0.01;
            worst3 = Math.max(worst3, Math.abs(n.noise(x + eps, y, z) - n.noise(x, y, z)));
            worst3 = Math.max(worst3, Math.abs(n.noise(x, y + eps, z) - n.noise(x, y, z)));
            worst3 = Math.max(worst3, Math.abs(n.noise(x, y, z + eps) - n.noise(x, y, z)));
            worst2 = Math.max(worst2, Math.abs(n.noise(x + eps, z) - n.noise(x, z)));
        }
        // simplex gradients are bounded; measured over these samples the steepest 3D slope is about 6 per unit (a 0.01 step moves the value by
        // 0.062 at most), so 0.1 leaves a margin and still fails for a field with cliffs in it (a hash-based value noise jumps by up to 2)
        assertTrue(worst3 < 0.1, "3D continuity, worst jump " + worst3);
        assertTrue(worst2 < 0.1, "2D continuity, worst jump " + worst2);
    }

    @Test
    public void differentSeedsGiveDifferentFields() {
        Simplex3D a = new Simplex3D(1L), b = new Simplex3D(2L);
        int differing = 0;
        for (int i = 0; i < 1000; i++) {
            double x = i * 0.61, y = i * 0.29, z = i * 0.43;
            if (Math.abs(a.noise(x, y, z) - b.noise(x, y, z)) > 0.05) differing++;
        }
        assertTrue(differing > 800, "two seeds must disagree almost everywhere, differing=" + differing);
        // the origin is not special: plain simplex is 0 there for every seed, ours is offset per seed
        assertTrue(Math.abs(a.noise(0, 0, 0) - b.noise(0, 0, 0)) > 1e-9 || Math.abs(a.noise(0.5, 0.5, 0.5) - b.noise(0.5, 0.5, 0.5)) > 1e-9, "origin differs");
    }

    @Test
    public void theMeanIsNearZeroAndTheSpreadIsReal() {
        Simplex3D n = new Simplex3D(777L);
        double sum = 0, sq = 0;
        int count = 0;
        for (int x = -40; x < 40; x++) for (int y = 0; y < 40; y++) for (int z = -40; z < 40; z++) {
            double v = n.noise(x * 0.31, y * 0.31, z * 0.31);
            sum += v;
            sq += v * v;
            count++;
        }
        double mean = sum / count, rms = Math.sqrt(sq / count);
        assertTrue(Math.abs(mean) < 0.05, "mean " + mean);
        assertTrue(rms > 0.2 && rms < 0.8, "rms " + rms);
    }

    @Test
    public void fbmIsNormalisedAndSmootherThanItsOctaves() {
        Simplex3D n = new Simplex3D(5L);
        for (int i = 0; i < 3000; i++) {
            double x = i * 0.7, y = i * 0.3, z = i * 0.5;
            double f = n.fbm(x, y, z, 5, 2.0, 0.5);
            assertTrue(f >= -1.0 && f <= 1.0, "fbm in range at " + i);
        }
        // one octave of fbm is exactly the plain noise
        assertEquals(n.noise(3.3, 4.4, 5.5), n.fbm(3.3, 4.4, 5.5, 1, 2.0, 0.5));
        assertEquals(n.noise(3.3, 5.5), n.fbm(3.3, 5.5, 1, 2.0, 0.5));
        // zero octaves is flat
        assertEquals(0.0, n.fbm(1, 2, 3, 0, 2.0, 0.5));
    }
}
