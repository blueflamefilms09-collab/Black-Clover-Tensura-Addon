package com.newuniverse.nusmp.labyrinth.gen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The breadcrumb maths of the Magicule Veins (pure, no Minecraft). */
class VeinMathTest {
    private static final double BASE = 0.01, PEAK = 0.12, PULL = 0.6;
    private static final int RISE = 112;

    @Test
    void theChanceRisesWithHeightAndIsBounded() {
        double last = -1;
        for (int dy = -20; dy <= 200; dy++) {
            double c = VeinMath.chance(dy, 0, BASE, PEAK, RISE, PULL);
            assertTrue(c >= 0.0 && c <= 1.0, "bounded at dy " + dy);
            assertTrue(c >= last, "monotone at dy " + dy);
            last = c;
        }
        assertEquals(BASE + VeinMath.FLOOR, VeinMath.chance(0, 0, BASE, PEAK, RISE, PULL), 1e-12);
        assertEquals(BASE + VeinMath.FLOOR, VeinMath.chance(-30, 0, BASE, PEAK, RISE, PULL), 1e-12, "below the origin: the base chance");
        assertEquals((BASE + PEAK) / 2 + VeinMath.FLOOR, VeinMath.chance(56, 0, BASE, PEAK, RISE, PULL), 1e-12);
        assertEquals(PEAK + VeinMath.FLOOR, VeinMath.chance(112, 0, BASE, PEAK, RISE, PULL), 1e-12);
        assertEquals(PEAK + VeinMath.FLOOR, VeinMath.chance(300, 0, BASE, PEAK, RISE, PULL), 1e-12, "capped above the rise");
    }

    @Test
    void theChanceFallsAwayFromTheAxis() {
        double last = 2;
        for (int dxz = 0; dxz <= 100; dxz++) {
            double c = VeinMath.chance(112, dxz, BASE, PEAK, RISE, PULL);
            assertTrue(c <= last, "falls at dxz " + dxz);
            last = c;
        }
        assertEquals(PEAK * (1 - PULL) + VeinMath.FLOOR, VeinMath.chance(112, 64, BASE, PEAK, RISE, PULL), 1e-12);
        assertEquals(PEAK * (1 - PULL) + VeinMath.FLOOR, VeinMath.chance(112, 500, BASE, PEAK, RISE, PULL), 1e-12, "the pull is capped at REACH");
        assertEquals(PEAK * (1 - PULL / 2) + VeinMath.FLOOR, VeinMath.chance(112, 32, BASE, PEAK, RISE, PULL), 1e-12);
    }

    @Test
    void degenerateParametersAreClamped() {
        assertEquals(1.0, VeinMath.chance(10, 0, 5.0, 7.0, RISE, 0), 1e-12);
        assertEquals(VeinMath.FLOOR, VeinMath.chance(10, 0, -1.0, -1.0, RISE, 0), 1e-12);
        assertEquals(PEAK + VeinMath.FLOOR, VeinMath.chance(1, 0, BASE, PEAK, 0, PULL), 1e-12, "rise 0: everything is the peak");
        assertEquals(0.0, VeinMath.clamp01(Double.NaN), 0.0);
        assertEquals(BASE + VeinMath.FLOOR, VeinMath.chance(0, Double.NaN, BASE, PEAK, RISE, PULL), 1e-12);
    }

    @Test
    void theRollIsDeterministicAndSeedDependent() {
        for (int i = 0; i < 1000; i++) {
            int x = i * 7 - 300, y = i % 128, z = -i * 3;
            assertEquals(VeinMath.hash(42L, x, y, z), VeinMath.hash(42L, x, y, z));
            assertEquals(VeinMath.roll(42L, x, y, z, 0.3), VeinMath.roll(42L, x, y, z, 0.3));
        }
        int differ = 0;
        for (int i = 0; i < 1000; i++) if (VeinMath.hash(1L, i, 0, 0) != VeinMath.hash(2L, i, 0, 0)) differ++;
        assertTrue(differ > 990, "another seed is another pattern");
        assertNotEquals(VeinMath.hash(1L, 1, 2, 3), VeinMath.hash(1L, 3, 2, 1));
        assertNotEquals(VeinMath.hash(1L, 1, 2, 3), VeinMath.hash(1L, 1, 3, 2));
    }

    @Test
    void theRollHonoursTheChance() {
        assertFalse(VeinMath.roll(9L, 1, 2, 3, 0.0));
        assertFalse(VeinMath.roll(9L, 1, 2, 3, -1.0));
        assertFalse(VeinMath.roll(9L, 1, 2, 3, Double.NaN));
        assertTrue(VeinMath.roll(9L, 1, 2, 3, 1.0));
        for (double chance : new double[]{0.012, 0.1, 0.5}) {
            int hits = 0, n = 200000;
            for (int i = 0; i < n; i++) if (VeinMath.roll(2026L, i % 400, i / 400, i % 17, chance)) hits++;
            assertEquals(chance, hits / (double) n, 0.005, "observed rate at chance " + chance);
        }
    }

    @Test
    void theUnitIsInTheHalfOpenInterval() {
        for (int i = 0; i < 100000; i++) {
            double u = VeinMath.unit(77L, i, i >> 3, i >> 5);
            assertTrue(u >= 0.0 && u < 1.0);
        }
    }
}
