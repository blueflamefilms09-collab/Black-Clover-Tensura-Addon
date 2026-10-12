package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Dante's gravity numbers: fields, rock swords, waves, the pull and the Gravity Singularity (marks, bite, dodges). */
class GravityMathTest {
    @Test
    void aGravityWellIsStrongestAtTheCoreAndFadesToTheRim() {
        assertEquals(1.0, GravityMath.weight(0), 1e-9);
        assertEquals(0.35, GravityMath.weight(1), 1e-9);
        assertEquals(0.0, GravityMath.weight(1.01), 1e-9);
        double last = 2;
        for (double f = 0; f <= 1.0; f += 0.05) {
            double w = GravityMath.weight(f);
            assertTrue(w <= last + 1e-12, "monotonic");
            assertTrue(w >= 0.35 - 1e-9);
            last = w;
        }
    }

    @Test
    void theFieldBuildsUpAndNeverLocksInstantly() {
        assertEquals(0.4, GravityMath.ramp(0), 1e-9);
        assertEquals(1.0, GravityMath.ramp(GravityMath.RAMP), 1e-9);
        assertEquals(1.0, GravityMath.ramp(1000), 1e-9);
        // at the start of the strongest field the core is slowed less than at full build-up
        assertTrue(GravityMath.slowAmplifier(3, 1, GravityMath.ramp(0)) < GravityMath.slowAmplifier(3, 1, 1.0));
    }

    @Test
    void slownessIsExtremeAtTheCoreOfAStrongFieldAndMildAtTheRim() {
        assertEquals(5, GravityMath.slowAmplifier(3.0, 1.0, 1.0), "Slowness VI in the middle of a crushing field");
        assertEquals(-1, GravityMath.slowAmplifier(0.3, 0.35, 0.4), "a faint field slows nothing");
        int rim = GravityMath.slowAmplifier(2.0, GravityMath.weight(1), 1.0);
        int core = GravityMath.slowAmplifier(2.0, GravityMath.weight(0), 1.0);
        assertTrue(rim <= 1 && core >= 2 && core > rim);
        for (double s = 0.3; s <= 3.5; s += 0.1)
            for (double w = 0.35; w <= 1; w += 0.05) {
                int a = GravityMath.slowAmplifier(s, w, 1.0);
                assertTrue(a >= -1 && a <= 5);
            }
    }

    @Test
    void sinkingAndCrushingGrowWithStrengthAndAreBounded() {
        assertTrue(GravityMath.sinkSpeed(1, 1, 1) < 0);
        assertTrue(GravityMath.sinkSpeed(3, 1, 1) < GravityMath.sinkSpeed(1, 1, 1));
        assertTrue(GravityMath.sinkSpeed(GravityMath.MAX_STRENGTH, 1, 1) > -1.0, "a firm push, never a launch into the floor");
        assertTrue(GravityMath.crushBase(2, 1, 1) > GravityMath.crushBase(1, 1, 1));
        assertTrue(GravityMath.crushBase(GravityMath.MAX_STRENGTH, 1, 1) < 5, "one tick of crush is small");
    }

    @Test
    void clampsKeepAFieldBounded() {
        assertEquals(GravityMath.MIN_RADIUS, GravityMath.clampRadius(-3));
        assertEquals(GravityMath.MAX_RADIUS, GravityMath.clampRadius(500));
        assertEquals(GravityMath.MIN_TICKS, GravityMath.clampTicks(0));
        assertEquals(GravityMath.MAX_TICKS, GravityMath.clampTicks(100000));
        assertEquals(GravityMath.MIN_STRENGTH, GravityMath.clampStrength(0));
        assertEquals(GravityMath.MAX_STRENGTH, GravityMath.clampStrength(99));
        assertEquals(9.0, GravityMath.clampRadius(9.0));
    }

    @Test
    void swordVolleysGrowWithCastSlotsAndFanOutAroundTheAim() {
        assertEquals(3, GravityMath.swordCount(1));
        assertEquals(5, GravityMath.swordCount(2));
        assertEquals(7, GravityMath.swordCount(3));
        assertEquals(9, GravityMath.swordCount(4));
        assertEquals(3, GravityMath.swordCount(0));
        assertTrue(GravityMath.swordCount(99) <= GravityMath.MAX_SWORDS);
        for (int n = 1; n <= GravityMath.MAX_SWORDS; n++) {
            double[] f = GravityMath.fan(n);
            assertEquals(n, f.length);
            double sum = 0, lo = 9, hi = -9;
            for (double a : f) { sum += a; lo = Math.min(lo, a); hi = Math.max(hi, a); }
            assertEquals(0, sum, 1e-9, "centred on the aim");
            assertTrue(hi - lo <= 1.1 + 1e-9, "never wider than about 63 degrees");
            if (n % 2 == 1) assertEquals(0, f[n / 2], 1e-9, "an odd fan has a sword straight at the target");
            for (int i = 1; i < n; i++) assertTrue(f[i] > f[i - 1], "distinct, ordered");
        }
        assertEquals(1, GravityMath.fan(0).length, "never an empty fan");
    }

    @Test
    void theStompWaveTravelsOutwardAndCanBeJumped() {
        for (int a = 1; a < 40; a++) assertTrue(GravityMath.waveRadius(a) > GravityMath.waveRadius(a - 1));
        double reach = 9;
        int ticks = GravityMath.waveTicks(reach);
        assertTrue(GravityMath.waveRadius(ticks) >= reach, "the wave reaches its reach before it ends");
        assertTrue(GravityMath.waveRadius(ticks - 3) < reach + GravityMath.WAVE_THICK + 2);
        assertTrue(GravityMath.waveHits(5.0, 5.8) && !GravityMath.waveHits(5.0, 7.0));
        assertTrue(GravityMath.jumpsWave(1.0) && !GravityMath.jumpsWave(0.2));
        // every point of the floor inside the reach is passed by the wave (no gap between two ticks)
        for (double d = 1.5; d <= reach; d += 0.1) {
            boolean met = false;
            for (int a = 0; a <= ticks && !met; a++) met = GravityMath.waveHits(d, GravityMath.waveRadius(a));
            assertTrue(met, "dist " + d);
        }
    }

    @Test
    void spikesAndPullAreBounded() {
        assertEquals(0, GravityMath.spikeDelay(0));
        assertEquals(20, GravityMath.spikeDelay(10));
        assertEquals(60, GravityMath.spikeDelay(500));
        for (double d = 0; d <= 60; d += 1.5)
            for (double s = 0; s <= 4; s += 0.5) {
                double v = GravityMath.pullSpeed(d, s);
                assertTrue(v > 0 && v <= 2.2, "pull " + v);
            }
        assertTrue(GravityMath.pullSpeed(20, 1) > GravityMath.pullSpeed(5, 1));
        assertTrue(GravityMath.pullSpeed(20, 2) > GravityMath.pullSpeed(20, 1));
    }

    @Test
    void singularityNeedsSixtyPercentAndGrowsWithPower() {
        assertEquals(0, GravityMath.markCount(20));
        assertEquals(0, GravityMath.markCount(59));
        assertEquals(2, GravityMath.markCount(60));
        assertEquals(2, GravityMath.markCount(79));
        assertEquals(3, GravityMath.markCount(80));
        assertEquals(3, GravityMath.markCount(99));
        assertEquals(4, GravityMath.markCount(100));
        assertTrue(GravityMath.markCount(100) <= GravityMath.MAX_MARKS);
        assertEquals(7.0, GravityMath.zoneRadius(1.0), 1e-9);
        assertTrue(GravityMath.zoneRadius(0) >= 5.5 + 0.75 - 1e-9 && GravityMath.zoneRadius(99) <= 8.5 + 1e-9);
        assertEquals(34.0, GravityMath.collapseDamage(1.0), 1e-9);
        assertTrue(GravityMath.collapseDamage(2.0) > GravityMath.collapseDamage(1.0));
        assertTrue(GravityMath.GATHER > GravityMath.PULL, "the pull is the last stretch of the gather");
        assertTrue(GravityMath.GATHER >= 24, "the marked zone is shown for more than a second before it collapses");
    }

    @Test
    void shieldsAndAntiMagicSoftenTheCollapseAndJumpingDodgesIt() {
        assertEquals(1.0, GravityMath.mitigation(false, false));
        assertEquals(0.35, GravityMath.mitigation(true, false));
        assertEquals(0.40, GravityMath.mitigation(false, true));
        assertEquals(0.35, GravityMath.mitigation(true, true), "the better guard applies, they do not stack to nothing");
        assertTrue(GravityMath.jumpsCollapse(1.25) && !GravityMath.jumpsCollapse(1.0) && !GravityMath.jumpsCollapse(0));
        assertTrue(GravityMath.JUMP_CLEARANCE < 1.25, "a plain jump clears it");
    }

    @Test
    void marksStartOnTheTargetAndNeverOverlapOrExceedTheCount() {
        double r = GravityMath.zoneRadius(1.0);
        for (int n = 0; n <= 7; n++) {
            List<double[]> marks = GravityMath.planMarks(n, 10, -4, null, r, 0.7);
            assertEquals(Math.min(n, GravityMath.MAX_MARKS), marks.size(), "n=" + n);
            if (n > 0) {
                assertEquals(10, marks.get(0)[0]);
                assertEquals(-4, marks.get(0)[1]);
            }
            for (int i = 0; i < marks.size(); i++)
                for (int j = i + 1; j < marks.size(); j++)
                    assertTrue(dist(marks.get(i), marks.get(j)) >= r - 1e-9, "no two marks stacked, n=" + n);
        }
    }

    @Test
    void marksLeaveASafeWayOut() {
        // with the biggest layout, the zones cover well under half of the disc they sit in, and the ring marks keep a gap between them
        double r = GravityMath.zoneRadius(2.0);
        List<double[]> marks = GravityMath.planMarks(4, 0, 0, null, r, 0.3);
        assertEquals(4, marks.size());
        for (int i = 1; i < marks.size(); i++) {
            assertTrue(dist(marks.get(0), marks.get(i)) > 2 * r, "ring marks sit clear of the first");
            for (int j = i + 1; j < marks.size(); j++) assertTrue(dist(marks.get(i), marks.get(j)) > 2 * r, "and of each other");
        }
        // walk away from the first mark in some direction: there is always a straight path of 4 radii that crosses no zone
        boolean found = false;
        for (int k = 0; k < 72 && !found; k++) {
            double a = k * Math.PI / 36;
            boolean clear = true;
            for (double t = r + 0.5; t < r * 4 && clear; t += 0.25) {
                double[] p = {Math.cos(a) * t, Math.sin(a) * t};
                for (int i = 1; i < marks.size(); i++) if (dist(p, marks.get(i)) < r) clear = false;
            }
            if (clear) found = true;
        }
        assertTrue(found, "somewhere to run");
    }

    @Test
    void marksPreferTheOtherFoesAndSkipClusters() {
        double r = GravityMath.zoneRadius(1.0);
        double[][] foes = {{3, 3}, {30, 0}, {31, 1}, {-30, 5}};
        List<double[]> marks = GravityMath.planMarks(3, 0, 0, foes, r, 0.0);
        assertEquals(3, marks.size());
        assertEquals(0, marks.get(0)[0]);
        assertEquals(30, marks.get(1)[0], "the nearest-in-order foe that is not already covered");
        assertEquals(-30, marks.get(2)[0], "the cluster at 31,1 is skipped");
        for (int i = 0; i < marks.size(); i++)
            for (int j = i + 1; j < marks.size(); j++) assertTrue(dist(marks.get(i), marks.get(j)) >= r - 1e-9);
        // bad rows are ignored
        assertEquals(2, GravityMath.planMarks(2, 0, 0, new double[][]{null, {1}, {40, 0}}, r, 0).size());
    }

    @Test
    void theBiteIsABowlOrderedFromTheCentre() {
        for (int r = 3; r <= 6; r++) {
            int d = GravityMath.craterDepth(r);
            int[][] cells = GravityMath.crater(r, d);
            assertTrue(cells.length > 0);
            assertArrayEquals(new int[]{0, 0, 0}, cells[0], "starts at the surface cell");
            Set<Long> seen = new HashSet<>();
            int last = 0;
            for (int[] c : cells) {
                int d2 = c[0] * c[0] + c[1] * c[1] + c[2] * c[2];
                assertTrue(d2 <= r * r, "inside the ball");
                assertTrue(c[1] >= -d && c[1] <= r, "never deeper than the bowl");
                assertTrue(d2 >= last, "from the centre outward");
                last = d2;
                assertTrue(seen.add(((long) c[0] << 40) ^ ((long) c[1] << 20) ^ c[2] + 0x7FFFFL), "no duplicates");
            }
            assertTrue(cells.length < Math.pow(2 * r + 1, 3));
        }
        int[][] a = GravityMath.crater(5, 3), b = GravityMath.crater(5, 3);
        assertNotSame(a, b);
        assertArrayEquals(a[0], b[0]);
        assertArrayEquals(a[a.length - 1], b[b.length - 1], "deterministic");
    }

    @Test
    void craterSizesAreBoundedByTheLedger() {
        assertEquals(4, GravityMath.craterRadius(0.5));
        assertEquals(5, GravityMath.craterRadius(1.0));
        assertEquals(6, GravityMath.craterRadius(2.0));
        assertEquals(6, GravityMath.craterRadius(50.0));
        assertTrue(GravityMath.craterDepth(6) <= 4 && GravityMath.craterDepth(4) >= 2);
        // the BlockLedger holds 3072 blocks; one cast at the Gates (4 spheres) uses a bit over half and two casts fit
        assertTrue(GravityMath.MAX_MARKS * GravityMath.CRATER_CAP <= 3072, "even five spheres fit the ledger once");
        assertTrue(GravityMath.markCount(100) * GravityMath.CRATER_CAP * 2 <= 3072 + 400);
    }

    static double dist(double[] a, double[] b) { return Math.hypot(a[0] - b[0], a[1] - b[1]); }
}
