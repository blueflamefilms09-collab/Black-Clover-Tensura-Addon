package com.newuniverse.nusmp.dungeon.nexus.vault;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The spatial hash behind "which Descent Gate is nearest?". */
class GateGridTest {
    @Test
    void emptyGridFindsNothing() {
        GateGrid g = new GateGrid();
        assertNull(g.nearest(0, 0, 0, 1000));
        assertEquals(0, g.size());
    }

    @Test
    void addRemoveAndContains() {
        GateGrid g = new GateGrid();
        assertTrue(g.add(5, -40, 9));
        assertFalse(g.add(5, -40, 9), "twice is once");
        assertEquals(1, g.size());
        assertTrue(g.contains(5, -40, 9));
        assertFalse(g.contains(5, -40, 10));
        assertTrue(g.remove(5, -40, 9));
        assertFalse(g.remove(5, -40, 9));
        assertEquals(0, g.size());
        assertNull(g.nearest(5, -40, 9, 5));
    }

    @Test
    void theNearestWithinTheRadiusWins() {
        GateGrid g = new GateGrid();
        g.add(100, 30, 100);
        g.add(40, 30, 0);
        g.add(-10, 28, 3);
        GateGrid.P p = g.nearest(0, 30, 0, 200);
        assertEquals(new GateGrid.P(-10, 28, 3), p);
        assertEquals(new GateGrid.P(40, 30, 0), g.nearest(35, 30, 0, 200));
        assertNull(g.nearest(0, 30, 0, 9), "nothing that close");
        assertEquals(new GateGrid.P(-10, 28, 3), g.nearest(0, 30, 0, 11));
    }

    @Test
    void theRadiusIsInThreeDimensions() {
        GateGrid g = new GateGrid();
        g.add(0, 100, 0);
        assertNull(g.nearest(0, 0, 0, 99));
        assertNotNull(g.nearest(0, 0, 0, 100));
    }

    @Test
    void worksAcrossBucketEdgesAndNegativeCoordinates() {
        GateGrid g = new GateGrid();
        g.add(-1, -1, -1);
        g.add(31, 31, 31);
        g.add(32, 32, 32);
        g.add(-33, -33, -33);
        assertEquals(new GateGrid.P(-1, -1, -1), g.nearest(-2, -2, -2, 5));
        assertEquals(new GateGrid.P(31, 31, 31), g.nearest(31, 31, 31, 0));
        assertEquals(new GateGrid.P(-33, -33, -33), g.nearest(-34, -34, -34, 3));
        assertEquals(new GateGrid.P(32, 32, 32), g.nearest(33, 33, 33, 3));
    }

    @Test
    void ties_goToTheLowestPosition() {
        GateGrid g = new GateGrid();
        g.add(10, 0, 0);
        g.add(-10, 0, 0);
        assertEquals(new GateGrid.P(-10, 0, 0), g.nearest(0, 0, 0, 20));
    }

    @Test
    void aSphereWiderThanTheMapStillFindsTheNearestAndIsFast() {
        GateGrid g = new GateGrid();
        g.add(3000, 20, -2500);
        g.add(-100, 40, 80);
        g.add(900, -30, 900);
        long t0 = System.nanoTime();
        assertEquals(new GateGrid.P(-100, 40, 80), g.nearest(0, 0, 0, 4000));
        assertEquals(new GateGrid.P(3000, 20, -2500), g.nearest(2800, 0, -2400, 4000));
        assertNull(g.nearest(0, 0, 0, 50));
        assertTrue((System.nanoTime() - t0) / 1_000_000 < 500, "a huge radius must not walk every bucket of the sphere");
    }

    @Test
    void matchesABruteForceSearch() {
        Random r = new Random(42);
        GateGrid g = new GateGrid();
        int[][] pts = new int[400][];
        for (int i = 0; i < pts.length; i++) {
            pts[i] = new int[]{r.nextInt(4000) - 2000, r.nextInt(180) - 60, r.nextInt(4000) - 2000};
            g.add(pts[i][0], pts[i][1], pts[i][2]);
        }
        for (int i = 0; i < pts.length; i += 7) g.remove(pts[i][0], pts[i][1], pts[i][2]);
        for (int q = 0; q < 300; q++) {
            int x = r.nextInt(4000) - 2000, y = r.nextInt(180) - 60, z = r.nextInt(4000) - 2000, radius = 50 + r.nextInt(600);
            GateGrid.P best = null;
            long bestD = Long.MAX_VALUE;
            for (int i = 0; i < pts.length; i++) {
                if (i % 7 == 0) continue;
                long dx = pts[i][0] - x, dy = pts[i][1] - y, dz = pts[i][2] - z, d = dx * dx + dy * dy + dz * dz;
                if (d > (long) radius * radius) continue;
                GateGrid.P p = new GateGrid.P(pts[i][0], pts[i][1], pts[i][2]);
                if (d < bestD || (d == bestD && (p.x() < best.x() || p.x() == best.x() && (p.y() < best.y() || p.y() == best.y() && p.z() < best.z())))) {
                    bestD = d;
                    best = p;
                }
            }
            assertEquals(best, g.nearest(x, y, z, radius), "query " + q);
        }
        assertEquals(pts.length - (pts.length + 6) / 7, g.size());
        assertEquals(g.size(), g.all().size());
    }
}
