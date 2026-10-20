package com.newuniverse.nusmp.client.labyrinth.rift;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the mega-rare tear's renderer promises a vertex budget, a distance level of detail, a mesh cached by seed and a star-shaped outline a triangle fan can fill; the arithmetic is pure and pinned here.
 */
class TearGeometryTest {
    @Test
    void theMeshIsMadeOncePerSeedAndIsDeterministic() {
        TearGeometry.clearCache();
        TearGeometry a = TearGeometry.of(12345L), b = TearGeometry.of(12345L), c = TearGeometry.of(54321L);
        assertTrue(a == b, "cached by seed");
        assertTrue(a != c);
        TearGeometry.clearCache();
        TearGeometry d = TearGeometry.of(12345L);
        assertTrue(java.util.Arrays.equals(a.ox, d.ox) && java.util.Arrays.equals(a.sy, d.sy), "the same seed always gives the same tear");
        boolean differs = false;
        for (int i = 0; i < TearGeometry.N; i++) differs |= a.ox[i] != c.ox[i];
        assertTrue(differs, "another seed, another tear");
        for (long s = 0; s < 100; s++) TearGeometry.of(s);
        assertTrue(TearGeometry.cached() <= 24, "the cache is bounded: " + TearGeometry.cached());
    }

    @Test
    void theOutlineIsStarShapedAboutTheCentreAndInsideTheBounds() {
        for (long s = 0; s < 300; s++) {
            TearGeometry g = TearGeometry.of(s * 7919L);
            double prev = -1;
            for (int i = 0; i < TearGeometry.N; i++) {
                double dx = g.ox[i], dy = g.oy[i] - TearGeometry.CENTRE_Y;
                double ang = Math.atan2(dy, dx);
                if (ang < 0) ang += 2 * Math.PI;
                if (i > 0) assertTrue(ang > prev - 1e-6, "angles increase round the outline: seed " + s + " point " + i);
                prev = ang;
                assertTrue(Math.hypot(dx / TearGeometry.RX, dy / TearGeometry.RY) >= TearGeometry.MIN_RADIUS * 0.7, "the radius never falls under the minimum");
                assertTrue(Math.abs(dx) <= TearGeometry.RX * 1.01 && g.oy[i] >= -0.01 && g.oy[i] <= 2 * TearGeometry.CENTRE_Y * 1.4);
            }
            for (int k = 0; k < g.sx.length; k++) assertTrue(Math.abs(g.sx[k]) < 6 && g.sy[k] > -3 && g.sy[k] < 10, "a strand stays near the tear");
        }
    }

    @Test
    void theVertexBudgetHoldsAndFallsWithDistance() {
        assertTrue(TearGeometry.vertices(TearGeometry.FULL) <= TearGeometry.BUDGET, "full: " + TearGeometry.vertices(TearGeometry.FULL));
        assertTrue(TearGeometry.vertices(TearGeometry.FULL) > TearGeometry.vertices(TearGeometry.MIDDLE));
        assertTrue(TearGeometry.vertices(TearGeometry.MIDDLE) > TearGeometry.vertices(TearGeometry.FAR));
        assertTrue(TearGeometry.vertices(TearGeometry.FAR) > 0);
        assertEquals(0, TearGeometry.vertices(TearGeometry.NONE));
        assertEquals(TearGeometry.FULL, TearGeometry.lod(10 * 10));
        assertEquals(TearGeometry.MIDDLE, TearGeometry.lod(30 * 30));
        assertEquals(TearGeometry.FAR, TearGeometry.lod(60 * 60));
        assertEquals(TearGeometry.NONE, TearGeometry.lod(100 * 100));
    }

    @Test
    void theWobbleIsAHashOfIntegersInRange() {
        for (int i = 0; i < 1000; i++) {
            float h = TearGeometry.hash(99L, i, i * 3);
            assertTrue(h >= -1f && h <= 1f);
            assertEquals(h, TearGeometry.hash(99L, i, i * 3), 0f);
        }
    }
}
