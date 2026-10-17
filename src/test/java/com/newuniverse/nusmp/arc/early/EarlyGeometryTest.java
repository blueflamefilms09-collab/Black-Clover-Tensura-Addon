package com.newuniverse.nusmp.arc.early;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: every telegraph is a shape on the floor and the damage must land exactly where it was drawn, so the corridor, cone, fan, polygon, ring wave and falloff are proven in plain maths. */
class EarlyGeometryTest {
    @Test
    void aCorridorEndsWhereItsSegmentEnds() {
        // a line from (0,0) to (10,0), one block either side
        assertTrue(EarlyGeometry.inCorridor(5, 0.9, 0, 0, 10, 0, 1.0));
        assertFalse(EarlyGeometry.inCorridor(5, 1.1, 0, 0, 10, 0, 1.0), "outside the half width");
        assertTrue(EarlyGeometry.inCorridor(10.5, 0, 0, 0, 10, 0, 1.0), "the end is round: still inside half a block past it");
        assertFalse(EarlyGeometry.inCorridor(11.5, 0, 0, 0, 10, 0, 1.0), "past the end");
        assertFalse(EarlyGeometry.inCorridor(-1.5, 0, 0, 0, 10, 0, 1.0), "behind the start");
        assertEquals(0.5, EarlyGeometry.segmentT(5, 3, 0, 0, 10, 0), 1e-9);
        assertEquals(0.0, EarlyGeometry.segmentT(-5, 3, 0, 0, 10, 0), 1e-9, "clamped at the start");
        assertEquals(1.0, EarlyGeometry.segmentT(50, 3, 0, 0, 10, 0), 1e-9, "clamped at the end");
        assertEquals(5.0, EarlyGeometry.segmentDistance(3, 4, 0, 0, 0, 0), 1e-9, "a segment with no length is a point");
    }

    @Test
    void aConeHasReachAndAnAngle() {
        double half = Math.toRadians(55);
        assertTrue(EarlyGeometry.inCone(0, 0, 1, 0, 7, half, 5, 0), "straight ahead");
        assertTrue(EarlyGeometry.inCone(0, 0, 1, 0, 7, half, 4, 5), "inside the angle");
        assertFalse(EarlyGeometry.inCone(0, 0, 1, 0, 7, half, 2, 5), "outside the angle");
        assertFalse(EarlyGeometry.inCone(0, 0, 1, 0, 7, half, 8, 0), "beyond the reach");
        assertFalse(EarlyGeometry.inCone(0, 0, 1, 0, 7, half, -3, 0), "behind");
        assertTrue(EarlyGeometry.inCone(0, 0, 1, 0, 7, half, 0, 0), "the origin itself is in");
        assertFalse(EarlyGeometry.inCone(0, 0, 0, 0, 7, half, 3, 0), "a boss with no facing has no cone");
        assertEquals(Math.PI, EarlyGeometry.angleBetween(1, 0, -1, 0), 1e-9);
        assertEquals(Math.PI / 2, EarlyGeometry.angleBetween(1, 0, 0, 4), 1e-9);
        assertEquals(Math.PI, EarlyGeometry.angleBetween(0, 0, 1, 0), "no facing counts as behind");
    }

    @Test
    void aFanIsCentredAndEvenlySpread() {
        double[] one = EarlyGeometry.fan(1, 1.0);
        assertEquals(1, one.length);
        assertEquals(0.0, one[0], 1e-12);
        double[] five = EarlyGeometry.fan(5, 0.8);
        assertEquals(5, five.length);
        assertEquals(-0.4, five[0], 1e-12);
        assertEquals(0.0, five[2], 1e-12);
        assertEquals(0.4, five[4], 1e-12);
        assertEquals(five[1] - five[0], five[4] - five[3], 1e-12);
        assertEquals(1, EarlyGeometry.fan(0, 1.0).length, "never an empty fan");
    }

    @Test
    void aPolygonIsRegularAndRotates() {
        double[][] tri = EarlyGeometry.polygon(10, 20, 4, 3, 0);
        assertEquals(3, tri.length);
        assertEquals(14.0, tri[0][0], 1e-9);
        assertEquals(20.0, tri[0][1], 1e-9);
        for (double[] p : tri) assertEquals(4.0, Math.hypot(p[0] - 10, p[1] - 20), 1e-9);
        double d01 = Math.hypot(tri[0][0] - tri[1][0], tri[0][1] - tri[1][1]), d12 = Math.hypot(tri[1][0] - tri[2][0], tri[1][1] - tri[2][1]);
        assertEquals(d01, d12, 1e-9);
        double[][] turned = EarlyGeometry.polygon(10, 20, 4, 3, Math.PI);
        assertEquals(6.0, turned[0][0], 1e-9, "the phase turns the first corner");
        double[] r = EarlyGeometry.rotate(1, 0, Math.PI / 2);
        assertEquals(0.0, r[0], 1e-9);
        assertEquals(1.0, r[1], 1e-9);
    }

    @Test
    void aRingWaveRunsOutAndAJumpClearsIt() {
        assertEquals(0.5, EarlyGeometry.waveRadius(0), 1e-9);
        assertEquals(0.5 + 1.1 * 10, EarlyGeometry.waveRadius(10), 1e-9);
        assertEquals(EarlyGeometry.waveRadius(0), EarlyGeometry.waveRadius(-5), 1e-9, "no negative age");
        assertTrue(EarlyGeometry.waveTicks(8) * EarlyGeometry.WAVE_SPEED + EarlyGeometry.WAVE_START >= 8, "the wave reaches the edge");
        // a foe at 5 blocks is hit while the edge passes 5, and not before or long after
        assertFalse(EarlyGeometry.ringHits(5, 3.0));
        assertTrue(EarlyGeometry.ringHits(5, 5.2));
        assertTrue(EarlyGeometry.ringHits(5, 6.0));
        assertFalse(EarlyGeometry.ringHits(5, 7.0), "the edge has passed");
        // no foe moving less than a wave step is skipped: some tick has the edge within thickness of every distance
        for (double dist = 1; dist < 12; dist += 0.37) {
            boolean hit = false;
            for (int age = 0; age < 20; age++) hit |= EarlyGeometry.ringHits(dist, EarlyGeometry.waveRadius(age));
            assertTrue(hit, "distance " + dist + " is caught by the wave");
        }
        assertFalse(EarlyGeometry.jumpsRing(0.5));
        assertTrue(EarlyGeometry.jumpsRing(0.75));
    }

    @Test
    void aBlastFallsOffInAStraightLine() {
        assertEquals(1.0, EarlyGeometry.falloff(3, 5, 15, 0.45), 1e-9);
        assertEquals(1.0, EarlyGeometry.falloff(5, 5, 15, 0.45), 1e-9);
        assertEquals(0.725, EarlyGeometry.falloff(10, 5, 15, 0.45), 1e-9);
        assertEquals(0.45, EarlyGeometry.falloff(15, 5, 15, 0.45), 1e-9);
        assertEquals(0.0, EarlyGeometry.falloff(15.01, 5, 15, 0.45), 1e-9);
        assertEquals(1.0, EarlyGeometry.falloff(4, 6, 6, 0.5), 1e-9, "a blast with no slope is flat");
    }
}
