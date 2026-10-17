package com.newuniverse.nusmp.arc.sds;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the Sunflower is fair only if there is room to stand between the petals and never at the centre, so the geometry is checked. */
class SunflowerMathTest {
    @Test
    void thePetalsAreEvenlySpacedRoundTheCircle() {
        double[] a = SunflowerMath.angles(6, 0.3);
        assertEquals(6, a.length);
        assertEquals(0.3, a[0], 1e-12);
        for (int i = 1; i < a.length; i++) assertEquals(Math.PI / 3, a[i] - a[i - 1], 1e-12);
        assertEquals(1, SunflowerMath.angles(0, 0).length, "at least one petal");
    }

    @Test
    void theCentreIsNeverSafeAndAPetalReachesItsLength() {
        assertTrue(SunflowerMath.inPetal(0, 0, 6, 0, 0.9, 11));
        assertTrue(SunflowerMath.inPetal(0.5, 0.5, 6, 0.4, 0.9, 11));
        assertTrue(SunflowerMath.inPetal(5, 0, 6, 0, 0.9, 11), "on a petal's axis");
        assertTrue(SunflowerMath.inPetal(10.9, 0.5, 6, 0, 0.9, 11), "near the tip");
        assertFalse(SunflowerMath.inPetal(12.5, 0, 6, 0, 0.9, 11), "past the tip");
    }

    @Test
    void betweenTwoPetalsIsSafeFromAFewBlocksOut() {
        double mid = Math.PI / 6;                                           // half-way between the petals at 0 and 60 degrees
        for (double r = 3.0; r <= 10.5; r += 0.5) assertFalse(SunflowerMath.inPetal(Math.cos(mid) * r, Math.sin(mid) * r, 6, 0, 0.9, 11), "safe at r = " + r);
        assertTrue(SunflowerMath.inPetal(Math.cos(mid) * 1.0, Math.sin(mid) * 1.0, 6, 0, 0.9, 11), "right at the centre the lanes overlap");
    }

    @Test
    void theGapGrowsWithTheRadiusAndTheFitIsComputedFromIt() {
        assertTrue(SunflowerMath.gap(6, 0.9, 6) > SunflowerMath.gap(6, 0.9, 3));
        assertTrue(SunflowerMath.gap(6, 0.9, 1.0) < 0, "overlapping near the centre");
        double r = SunflowerMath.minSafeRadius(6, 0.9, 0.6, 0.5);
        assertEquals(0.6 + 0.5, SunflowerMath.gap(6, 0.9, r), 1e-9, "at that radius the gap is exactly the body and the margin");
        assertTrue(SunflowerMath.minSafeRadius(8, 0.9, 0.6, 0.5) > r, "more petals need more room");
        assertTrue(SunflowerMath.minSafeRadius(8, 0.9, 0.6, 0.5) < 5.0, "yet eight petals still leave room within five blocks");
    }
}
