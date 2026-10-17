package com.newuniverse.nusmp.arc.sds.kit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The ground-plane geometry the four Sins share: arcs that wrap, fans, segments, cones, rings and spread marks. */
class FourSinsMathTest {
    @Test
    void anglesWrapTheShortWay() {
        assertEquals(0.2, FourSinsMath.angleDiff(0.1, -0.1), 1e-9);
        assertEquals(-0.2, FourSinsMath.angleDiff(Math.PI - 0.1, -Math.PI + 0.1), 1e-9, "across the +-pi seam the difference is small, not 2pi");
        assertTrue(FourSinsMath.inArc(Math.PI - 0.05, -Math.PI + 0.05, 0.2));
        assertFalse(FourSinsMath.inArc(0, Math.PI, 1.0));
        assertEquals(Math.PI / 2, FourSinsMath.bearing(0, 1), 1e-9);
    }

    @Test
    void aFanIsSymmetricAndCentred() {
        double[] one = FourSinsMath.fan(1, 1.0);
        assertEquals(1, one.length);
        assertEquals(0.0, one[0], 1e-9);
        double[] five = FourSinsMath.fan(5, 0.8);
        assertEquals(-0.4, five[0], 1e-9);
        assertEquals(0.0, five[2], 1e-9);
        assertEquals(0.4, five[4], 1e-9);
        assertEquals(1, FourSinsMath.fan(0, 1.0).length, "at least one ray");
    }

    @Test
    void segmentDistanceAndProgress() {
        assertEquals(3.0, FourSinsMath.distToSegment(5, 3, 0, 0, 10, 0), 1e-9);
        assertEquals(5.0, FourSinsMath.distToSegment(-3, 4, 0, 0, 10, 0), 1e-9, "past the end the distance is to the end");
        assertEquals(1.0, FourSinsMath.distToSegment(0, 1, 0, 0, 0, 0), 1e-9, "a zero-length segment is a point");
        assertEquals(0.5, FourSinsMath.along(5, 9, 0, 0, 10, 0), 1e-9);
        assertEquals(1.0, FourSinsMath.along(50, 0, 0, 0, 10, 0), 1e-9);
        assertEquals(0.0, FourSinsMath.along(-4, 0, 0, 0, 10, 0), 1e-9);
    }

    @Test
    void aConeHasARangeAndAnOpening() {
        assertTrue(FourSinsMath.inCone(0, 0, 1, 0, 5, 1, 8, Math.toRadians(30)));
        assertFalse(FourSinsMath.inCone(0, 0, 1, 0, 5, 5, 8, Math.toRadians(30)), "too far to the side");
        assertFalse(FourSinsMath.inCone(0, 0, 1, 0, 9, 0, 8, Math.toRadians(30)), "too far away");
        assertFalse(FourSinsMath.inCone(0, 0, 1, 0, -2, 0, 8, Math.toRadians(30)), "behind the tip");
        assertTrue(FourSinsMath.inCone(0, 0, 1, 0, 0, 0, 8, 0.1), "the tip itself");
    }

    @Test
    void aRingHasEvenlySpacedPointsAtTheRadius() {
        double[][] r = FourSinsMath.ring(4, 10, 20, 3, 0);
        assertEquals(4, r.length);
        for (double[] p : r) assertEquals(3.0, Math.hypot(p[0] - 10, p[1] - 20), 1e-9);
        assertEquals(13.0, r[0][0], 1e-9);
        assertEquals(23.0, r[1][1], 1e-9, "a quarter turn later it is straight on in z");
        assertEquals(1, FourSinsMath.ring(0, 0, 0, 1, 0).length);
    }

    @Test
    void marksFirstHitTheTargetsThenSpreadWithoutStacking() {
        double[][] m = FourSinsMath.spreadMarks(new double[]{0, 0}, new double[][]{{6, 0}, {6.5, 0.2}, {0, 9}}, 6, 4, 2.0, 0.3);
        assertEquals(6, m.length);
        assertEquals(0.0, m[0][0], 1e-9);
        assertEquals(6.0, m[1][0], 1e-9, "the second mark is the first other foe");
        for (int i = 0; i < m.length; i++)
            for (int j = i + 1; j < m.length; j++) assertTrue(Math.hypot(m[i][0] - m[j][0], m[i][1] - m[j][1]) >= 2.0 - 1e-9, "marks " + i + " and " + j + " do not stack");
    }

    @Test
    void anImpossibleRequestStopsInsteadOfLooping() {
        double[][] m = FourSinsMath.spreadMarks(new double[]{0, 0}, new double[0][], 50, 1.0, 5.0, 0);
        assertTrue(m.length >= 1 && m.length < 50, "no room for fifty marks five blocks apart in a ring of one block");
    }

    @Test
    void clampsAndLerpStayInRange() {
        assertEquals(3.0, FourSinsMath.clamp(5.0, 0.0, 3.0), 1e-9);
        assertEquals(0.0, FourSinsMath.clamp(-5.0, 0.0, 3.0), 1e-9);
        assertEquals(2.0f, FourSinsMath.clamp(2.0f, 0f, 3f), 1e-6f);
        assertEquals(5.0, FourSinsMath.lerp(0, 10, 0.5), 1e-9);
        assertEquals(10.0, FourSinsMath.lerp(0, 10, 7), 1e-9);
    }
}
