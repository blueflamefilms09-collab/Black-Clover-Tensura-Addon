package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Zenon's Bone Magic: where spears grow, how volleys fan, the blade's cone, the garden's plan and cells, the armour. */
class BoneMathTest {
    static final double E = 1e-9;

    @Test
    void spearCountsGrowWithTheCastSlotsAndAreCapped() {
        assertEquals(3, BoneMath.spearCount(1));
        assertEquals(5, BoneMath.spearCount(2));
        assertEquals(7, BoneMath.spearCount(3));
        assertEquals(9, BoneMath.spearCount(4));
        assertEquals(3, BoneMath.spearCount(0));
        assertEquals(3, BoneMath.spearCount(-4));
        assertEquals(BoneMath.MAX_SPEARS, BoneMath.spearCount(99));
        int last = 0;
        for (int p = 20; p <= 100; p++) {
            int n = BoneMath.spearCount(PowerCurve.tuning(p).castSlots());
            assertTrue(n >= last && n >= 3 && n <= BoneMath.MAX_SPEARS, "power " + p);
            last = n;
        }
        assertEquals(3, BoneMath.spearCount(PowerCurve.tuning(20).castSlots()));
        assertEquals(9, BoneMath.spearCount(PowerCurve.tuning(100).castSlots()));
    }

    @Test
    void spearsGrowInAnArchBehindTheShouldersMirroredAcrossTheFacing() {
        for (int n = 1; n <= BoneMath.MAX_SPEARS; n++) {
            for (double yaw : new double[]{0, 90, 180, -90, 37.5}) {
                double yr = Math.toRadians(yaw), fx = -Math.sin(yr), fz = Math.cos(yr), rx = Math.cos(yr), rz = Math.sin(yr);
                for (int i = 0; i < n; i++) {
                    double[] p = BoneMath.growthPoint(i, n, yaw), m = BoneMath.growthPoint(n - 1 - i, n, yaw);
                    double fwd = p[0] * fx + p[2] * fz, lat = p[0] * rx + p[2] * rz;
                    assertTrue(fwd < 0, "behind him: n=" + n + " i=" + i);
                    assertTrue(p[1] >= 1.5 && p[1] <= 2.1, "shoulder height");
                    assertTrue(Math.abs(lat) <= 1.15 + E);
                    double mFwd = m[0] * fx + m[2] * fz, mLat = m[0] * rx + m[2] * rz;
                    assertEquals(fwd, mFwd, 1e-9, "mirror image");
                    assertEquals(-lat, mLat, 1e-9, "mirror image");
                    assertEquals(p[1], m[1], 1e-9);
                }
            }
        }
    }

    @Test
    void noTwoSpearsGrowFromTheSameSpot() {
        for (int n = 2; n <= BoneMath.MAX_SPEARS; n++) {
            double best = Double.POSITIVE_INFINITY;
            for (int i = 0; i < n; i++)
                for (int j = i + 1; j < n; j++) {
                    double[] a = BoneMath.growthPoint(i, n, 20), b = BoneMath.growthPoint(j, n, 20);
                    best = Math.min(best, Math.sqrt(Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2) + Math.pow(a[2] - b[2], 2)));
                }
            assertTrue(best > 0.15, "n=" + n + " min gap " + best);
        }
        assertArrayEquals(new double[]{0, 2.0, -0.6}, BoneMath.growthPoint(0, 1, 0), 1e-9);
    }

    @Test
    void aVolleyFansOutNarrowlyAndSymmetrically() {
        assertEquals(0, BoneMath.spread(0, 1), E);
        for (int n = 2; n <= BoneMath.MAX_SPEARS; n++) {
            double lo = Double.POSITIVE_INFINITY, hi = Double.NEGATIVE_INFINITY, prev = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < n; i++) {
                double s = BoneMath.spread(i, n);
                assertTrue(s > prev, "ordered left to right");
                prev = s;
                lo = Math.min(lo, s);
                hi = Math.max(hi, s);
                assertEquals(-s, BoneMath.spread(n - 1 - i, n), 1e-9);
            }
            assertTrue(hi - lo <= BoneMath.MAX_FAN + E, "n=" + n);
        }
        assertEquals(BoneMath.MAX_FAN, BoneMath.spread(8, 9) - BoneMath.spread(0, 9), 1e-9);
        assertEquals(0.12, BoneMath.spread(2, 3) - BoneMath.spread(0, 3), 1e-9);
        // out of range indices are clamped, never a wild angle
        assertEquals(BoneMath.spread(8, 9), BoneMath.spread(40, 9), E);
        assertEquals(BoneMath.spread(0, 9), BoneMath.spread(-3, 9), E);
    }

    @Test
    void flightTimeIsDistanceOverSpeed() {
        assertEquals(10, BoneMath.flightTicks(15.9, 1.6));
        assertEquals(11, BoneMath.flightTicks(16.1, 1.6));
        assertEquals(1, BoneMath.flightTicks(0, 1.6));
        assertEquals(1, BoneMath.flightTicks(-4, 1.6));
        assertTrue(BoneMath.flightTicks(5, 0) < 100, "a zero speed is not a hang");
        assertTrue(BoneMath.flightTicks(36, BoneMath.SPEAR_SPEED) <= BoneMath.SPEAR_LIFE, "a spear reaches the end of Zenon's range before it fades");
    }

    @Test
    void aSpearTakesTimeToArriveSoAMovingPlayerCanStepAside() {
        // at 12 blocks a spear needs 8 ticks; a sprinting player (0.28 blocks a tick) moves 2.2 blocks, more than the spear and a body are wide
        double lateral = 0.28 * BoneMath.flightTicks(12, BoneMath.SPEAR_SPEED);
        assertTrue(lateral > BoneMath.SPEAR_REACH + 0.3 + 1.0, "sidestep covers " + lateral);
    }

    @Test
    void bladeComboLengthByCastSlots() {
        assertEquals(3, BoneMath.bladeSlashes(1));
        assertEquals(3, BoneMath.bladeSlashes(2));
        assertEquals(4, BoneMath.bladeSlashes(3));
        assertEquals(5, BoneMath.bladeSlashes(4));
        assertEquals(5, BoneMath.bladeSlashes(40));
        assertEquals(3, BoneMath.bladeSlashes(-1));
    }

    @Test
    void theBladeConeHitsInFrontAndNotBehindOrBeyond() {
        double r = BoneMath.BLADE_REACH, arc = BoneMath.BLADE_HALF_ARC;
        // slash from the origin along +z
        assertTrue(BoneMath.inCone(0, 0, 0, 1, 0, 3, r, arc));
        assertTrue(BoneMath.inCone(0, 0, 0, 1, 0, r, r, arc), "exactly at the reach");
        assertFalse(BoneMath.inCone(0, 0, 0, 1, 0, r + 0.01, r, arc));
        assertFalse(BoneMath.inCone(0, 0, 0, 1, 0, -3, r, arc), "behind");
        assertFalse(BoneMath.inCone(0, 0, 0, 1, 3, 0, r, arc), "right beside him is 90 degrees off");
        assertTrue(BoneMath.inCone(0, 0, 0, 1, 2, 3, r, arc), "about 34 degrees off");
        assertFalse(BoneMath.inCone(0, 0, 0, 1, 3.5, 1.5, r, arc), "about 67 degrees off");
        assertTrue(BoneMath.inCone(0, 0, 0, 1, 0.2, -0.3, r, arc), "on top of him is always a hit");
        assertFalse(BoneMath.inCone(0, 0, 0, 0, 0, 3, r, arc), "no direction, no cone");
        // symmetric under a turn
        for (double a = 0; a < Math.PI * 2; a += 0.3) {
            double dx = Math.cos(a), dz = Math.sin(a);
            assertTrue(BoneMath.inCone(5, -2, dx, dz, 5 + dx * 3, -2 + dz * 3, r, arc));
            assertFalse(BoneMath.inCone(5, -2, dx, dz, 5 - dx * 3, -2 - dz * 3, r, arc));
        }
    }

    @Test
    void theBladeComboCanBeBackedOutOfAfterTheFirstSlash() {
        // a hit knocks the player 0.5 blocks and he backs off at 0.2 blocks a tick: out of the 4.2 reach well before the third slash
        double dist = 2.4 + 0.5;
        for (int t = 0; t < 2 * BoneMath.BLADE_GAP; t++) dist += 0.2;
        assertTrue(dist > BoneMath.BLADE_REACH, "backed out to " + dist);
    }

    @Test
    void gardenSpotsGrowWithPowerAndAreCapped() {
        assertEquals(5, BoneMath.gardenCount(1));
        assertEquals(6, BoneMath.gardenCount(2));
        assertEquals(7, BoneMath.gardenCount(3));
        assertEquals(8, BoneMath.gardenCount(4));
        assertEquals(BoneMath.MAX_SPOTS, BoneMath.gardenCount(99));
        assertEquals(5, BoneMath.gardenCount(0));
    }

    @Test
    void theGardenPlanStartsOnTheTargetThenTheOthersThenRings() {
        double r = BoneMath.GARDEN_RADIUS;
        List<double[]> plan = BoneMath.gardenPlan(6, 10, 20, new double[][]{{10.5, 20.5}, {12.5, 20}, {30, 20}, {10, 45}}, r, 0.3);
        assertEquals(6, plan.size());
        assertArrayEquals(new double[]{10, 20}, plan.get(0), E, "the first is on the target");
        assertArrayEquals(new double[]{30, 20}, plan.get(1), E, "the next on another foe; the two standing near the target (0.7 and 2.5 blocks) are skipped");
        assertArrayEquals(new double[]{10, 45}, plan.get(2), E);
        assertTrue(PortalMath.minSeparation(plan) >= r * 1.6 - 1e-9, "no foe stands under two spots: " + PortalMath.minSeparation(plan));
    }

    @Test
    void theGardenPlanFillsWithRingsWhenAloneAndNeverOverlaps() {
        double r = BoneMath.GARDEN_RADIUS;
        for (int n = 1; n <= BoneMath.MAX_SPOTS; n++) {
            for (double phase = 0; phase < 6.3; phase += 0.7) {
                List<double[]> plan = BoneMath.gardenPlan(n, 0, 0, null, r, phase);
                assertEquals(n, plan.size(), "n=" + n + " phase=" + phase);
                assertArrayEquals(new double[]{0, 0}, plan.get(0), E);
                if (n > 1) assertTrue(PortalMath.minSeparation(plan) >= r * 1.6 - 1e-9, "n=" + n + " phase " + phase);
            }
        }
        assertEquals(BoneMath.MAX_SPOTS, BoneMath.gardenPlan(50, 0, 0, null, r, 0).size());
        assertTrue(BoneMath.gardenPlan(0, 0, 0, null, r, 0).isEmpty());
        assertTrue(BoneMath.gardenPlan(-3, 0, 0, null, r, 0).isEmpty());
    }

    @Test
    void theGardenIgnoresBrokenFoeEntries() {
        List<double[]> plan = BoneMath.gardenPlan(3, 0, 0, new double[][]{null, {1}, {12, 0}}, BoneMath.GARDEN_RADIUS, 0);
        assertEquals(3, plan.size());
        assertArrayEquals(new double[]{12, 0}, plan.get(1), E);
    }

    @Test
    void theGrowthsStandJustOutsideTheDamageRing() {
        List<double[]> cols = BoneMath.columnBases(5, 5, 0.4);
        assertEquals(BoneMath.GROWTH_COLUMNS, cols.size());
        for (double[] c : cols) assertEquals(BoneMath.GROWTH_RING, Math.hypot(c[0] - 5, c[1] - 5), 1e-9);
        assertTrue(BoneMath.GROWTH_RING > BoneMath.GARDEN_RADIUS, "growths never sit inside the ring that hurts");
        assertTrue(PortalMath.minSeparation(cols) > 2.5, "columns are not stacked");
        assertEquals(12, BoneMath.cellsPerSpot());
        assertEquals(1, BoneMath.columnHeight(0));
        assertEquals(2, BoneMath.columnHeight(1));
        assertEquals(3, BoneMath.columnHeight(2));
        assertEquals(1, BoneMath.columnHeight(3));
        assertEquals(3, BoneMath.columnHeight(-1));
    }

    @Test
    void aWholeGardenStaysUnderTheCellCap() {
        assertTrue(BoneMath.MAX_SPOTS * BoneMath.cellsPerSpot() <= BoneMath.GROWTH_CELL_CAP * 1, "a nine-spot garden is at most " + BoneMath.MAX_SPOTS * BoneMath.cellsPerSpot() + " blocks");
        assertTrue(BoneMath.GROWTH_CELL_CAP * 10 < 3072, "ten casts' worth fit in the block ledger (3072 entries)");
        assertTrue(BoneMath.GROWTH_LIFE >= 100, "the bone stays long enough to be terrain");
    }

    @Test
    void theShellIsAShareOfHisHealthThatGrowsWithPower() {
        assertEquals(47.5, BoneMath.shellPool(950, 20), 1e-9, "5% at the opening");
        assertEquals(85.5, BoneMath.shellPool(950, 100), 1e-9, "9% at the Gates");
        double last = 0;
        for (int p = 0; p <= 100; p++) {
            double s = BoneMath.shellPool(950, p);
            assertTrue(s >= last && s < 950 * 0.1, "power " + p);
            last = s;
        }
        assertTrue(BoneMath.shellPool(0, 50) >= 1);
        assertTrue(BoneMath.SHELL_TICKS >= 100 && BoneMath.SHELL_TICKS <= 240);
    }

    @Test
    void theShellSwallowsBlowsUntilItBreaksThenPassesTheRemainder() {
        BoneShell s = new BoneShell();
        assertFalse(s.active(0));
        assertEquals(10, s.absorb(0, 10), E, "no shell, the whole blow");
        s.raise(30, 200);
        assertTrue(s.active(0));
        assertEquals(0, s.absorb(5, 10), E);
        assertEquals(0, s.absorb(6, 10), E);
        assertEquals(1.0 / 3, s.fraction(), 1e-9);
        assertTrue(s.active(7));
        assertEquals(5, s.absorb(7, 15), E, "the blow that breaks it passes 15 - 10 = 5");
        assertFalse(s.active(8));
        assertEquals(7, s.absorb(8, 7), E, "broken: everything gets through");
    }

    @Test
    void aBlowJustUnderThePoolLeavesASliverAndPassesNothing() {
        BoneShell s = new BoneShell();
        s.raise(10, 100);
        assertEquals(0, s.absorb(1, 9.5), E, "nothing gets through, never a negative remainder");
        assertTrue(s.active(2), "half a point of armour is still armour");
        assertEquals(0.05, s.fraction(), 1e-9);
        assertEquals(0.5, s.absorb(3, 1.0), E, "the next blow breaks it and passes 0.5");
        assertFalse(s.active(4));
    }

    @Test
    void theShellFadesWithTimeAndCanBeDropped() {
        BoneShell s = new BoneShell();
        s.raise(100, 50);
        assertTrue(s.active(49));
        assertFalse(s.active(50), "gone at its time");
        assertEquals(9, s.absorb(50, 9), E);
        s.raise(100, 500);
        assertTrue(s.active(0));
        s.drop();
        assertFalse(s.active(0));
        assertEquals(0.0, s.fraction(), E);
        assertEquals(4, s.absorb(0, 4), E);
        s.raise(20, 100);
        assertEquals(0, s.absorb(1, 0), E, "a zero blow costs nothing");
        assertEquals(0, s.absorb(1, -5), E, "a negative blow is nothing");
        assertEquals(20, s.pool(), E);
    }

    @Test
    void aVolleyHitsAnyOneVictimOnlyAFewTimes() {
        VolleyBudget b = new VolleyBudget(BoneMath.MAX_HITS_PER_VICTIM);
        int landed = 0;
        for (int spear = 0; spear < BoneMath.MAX_SPEARS; spear++) if (b.tryHit(7)) landed++;
        assertEquals(BoneMath.MAX_HITS_PER_VICTIM, landed);
        assertEquals(BoneMath.MAX_HITS_PER_VICTIM, b.hitsOn(7));
        assertTrue(b.tryHit(8), "another victim is not affected");
        assertEquals(0, b.hitsOn(99));
        assertEquals(1, new VolleyBudget(0).maxHits(), "at least one hit");
        assertEquals(BoneMath.MAX_HITS_PER_VICTIM, b.maxHits());
    }
}
