package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Zenon's Spatial Magic geometry: behind a target, exit portals, warp hops, retreat, entry arc, and the counts by power. */
class PortalMathTest {
    static final double E = 1e-9;

    @Test
    void behindIsOppositeTheFacingAndAheadIsTheFacing() {
        // Minecraft yaw 0 faces +Z, 90 faces -X, 180 faces -Z, -90 faces +X
        assertArrayEquals(new double[]{0, -3}, PortalMath.behind(0, 0, 0, 3), 1e-9);
        assertArrayEquals(new double[]{3, 0}, PortalMath.behind(0, 0, 90, 3), 1e-9);
        assertArrayEquals(new double[]{0, 3}, PortalMath.behind(0, 0, 180, 3), 1e-9);
        assertArrayEquals(new double[]{-3, 0}, PortalMath.behind(0, 0, -90, 3), 1e-9);
        assertArrayEquals(new double[]{0, 3}, PortalMath.ahead(0, 0, 0, 3), 1e-9);
        assertArrayEquals(new double[]{-3, 0}, PortalMath.ahead(0, 0, 90, 3), 1e-9);
        for (double yaw = -180; yaw <= 180; yaw += 22.5) {
            double[] b = PortalMath.behind(7, -4, yaw, 2.2), a = PortalMath.ahead(7, -4, yaw, 2.2);
            assertEquals(2.2, Math.hypot(b[0] - 7, b[1] + 4), 1e-9);
            assertEquals(2.2, Math.hypot(a[0] - 7, a[1] + 4), 1e-9);
            assertEquals(14, b[0] + a[0], 1e-9, "opposite sides of the target");
            assertEquals(-8, b[1] + a[1], 1e-9);
        }
    }

    @Test
    void exitPortalsSpreadRoundTheTargetAndNeverCrowd() {
        for (int n = 1; n <= 5; n++) {
            for (double phase = 0; phase < 6.3; phase += 0.45) {
                List<double[]> pts = PortalMath.exitPoints(n, 100, -50, 6, 9, phase);
                assertEquals(n, pts.size());
                for (int k = 0; k < n; k++) {
                    double d = Math.hypot(pts.get(k)[0] - 100, pts.get(k)[1] + 50);
                    assertEquals(k % 2 == 0 ? 6 : 9, d, 1e-9);
                }
                if (n > 1) assertTrue(PortalMath.minSeparation(pts) >= PortalMath.MIN_EXIT_SEPARATION, "n=" + n + " phase " + phase + " gap " + PortalMath.minSeparation(pts));
            }
        }
        assertEquals(8, PortalMath.exitPoints(40, 0, 0, 6, 9, 0).size(), "capped at eight");
        assertTrue(PortalMath.exitPoints(0, 0, 0, 6, 9, 0).isEmpty());
        assertTrue(PortalMath.exitPoints(-2, 0, 0, 6, 9, 0).isEmpty());
    }

    @Test
    void warpHopsAreFarApartAndNeverRepeatASpot() {
        double r = 8;
        double[] prev = null;
        java.util.List<double[]> spots = new java.util.ArrayList<>();
        for (int k = 0; k < 5; k++) {
            double a = PortalMath.hopAngle(k, 0.4);
            double[] s = PortalMath.onRing(0, 0, r, a);
            assertEquals(r, Math.hypot(s[0], s[1]), 1e-9);
            if (prev != null) assertTrue(Math.hypot(s[0] - prev[0], s[1] - prev[1]) > r * 1.5, "consecutive hops are across the ring: " + k);
            prev = s;
            spots.add(s);
        }
        assertTrue(PortalMath.minSeparation(spots) > 5, "five hops land on five different spots: " + PortalMath.minSeparation(spots));
    }

    @Test
    void retreatLiesOnTheLineFromTheTargetThroughTheBoss() {
        double[] r = PortalMath.retreat(3, 4, 0, 0, 10);
        assertEquals(10, Math.hypot(r[0], r[1]), 1e-9);
        assertEquals(0.6, r[0] / 10, 1e-9);
        assertEquals(0.8, r[1] / 10, 1e-9);
        double[] same = PortalMath.retreat(5, 5, 5, 5, 8);
        assertEquals(8, Math.hypot(same[0] - 5, same[1] - 5), 1e-9, "a boss on top of the target still goes somewhere");
        double[] far = PortalMath.retreat(-30, 0, 0, 0, 9);
        assertArrayEquals(new double[]{-9, 0}, far, 1e-9, "it can also pull him in");
    }

    @Test
    void entryPortalsOpenInAnArcInFrontOfTheBoss() {
        for (double yaw : new double[]{0, 90, 180, -90, 33}) {
            for (int n = 1; n <= 5; n++) {
                List<double[]> arc = PortalMath.entryArc(10, 10, yaw, n, 1.8, 2.4);
                assertEquals(n, arc.size());
                double yr = Math.toRadians(yaw), fx = -Math.sin(yr), fz = Math.cos(yr);
                for (double[] p : arc) assertTrue((p[0] - 10) * fx + (p[1] - 10) * fz >= 2.4 - 1e-9, "in front of him");
                if (n > 1) assertTrue(PortalMath.minSeparation(arc) >= 1.7, "gates do not overlap: " + PortalMath.minSeparation(arc));
                double cx = 0, cz = 0;
                for (double[] p : arc) { cx += p[0]; cz += p[1]; }
                double rx = Math.cos(yr), rz = Math.sin(yr);
                assertEquals(0, (cx / n - 10) * rx + (cz / n - 10) * rz, 1e-9, "centred on where he faces");
            }
        }
        assertEquals(8, PortalMath.entryArc(0, 0, 0, 99, 1, 2).size());
        assertTrue(PortalMath.entryArc(0, 0, 0, 0, 1, 2).isEmpty());
    }

    @Test
    void minSeparationOfFewPointsIsInfinite() {
        assertEquals(Double.POSITIVE_INFINITY, PortalMath.minSeparation(List.of()));
        assertEquals(Double.POSITIVE_INFINITY, PortalMath.minSeparation(List.of(new double[]{1, 2})));
        assertEquals(5, PortalMath.minSeparation(List.of(new double[]{0, 0}, new double[]{3, 4}, new double[]{30, 40})), 1e-9);
    }

    @Test
    void portalCountsGrowWithThePowerStages() {
        assertEquals(2, PortalMath.linkPairs(40));
        assertEquals(3, PortalMath.linkPairs(60));
        assertEquals(4, PortalMath.linkPairs(80));
        assertEquals(5, PortalMath.linkPairs(100));
        assertEquals(2, PortalMath.linkPairs(20), "never under two");
        int last = 0;
        for (int p = 20; p <= 100; p++) {
            assertTrue(PortalMath.linkPairs(p) >= last);
            last = PortalMath.linkPairs(p);
        }
        assertEquals(1, PortalMath.linkWaves(80));
        assertEquals(2, PortalMath.linkWaves(100), "the portals chain at the Gates");
        assertEquals(3, PortalMath.warpHops(40));
        assertEquals(4, PortalMath.warpHops(60));
        assertEquals(4, PortalMath.warpHops(80));
        assertEquals(5, PortalMath.warpHops(100));
    }

    @Test
    void theBlinkAndTheEmergeLeaveTimeToReadThem() {
        assertTrue(PortalMath.BLINK_TICKS >= 4 && PortalMath.BLINK_TICKS <= 8, "a short blink time");
        assertTrue(PortalMath.EMERGE_TICKS >= 12, "the exit portals are visible for most of a second before a spear comes out");
    }
}
