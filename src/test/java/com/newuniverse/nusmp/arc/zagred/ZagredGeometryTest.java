package com.newuniverse.nusmp.arc.zagred;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The flat geometry of the floor shapes: the band, the lane, the ring, the curve of letters, the field of marks. */
class ZagredGeometryTest {
    @Test
    void unitAndRotate() {
        double[] u = ZagredGeometry.unit(3, 4);
        assertEquals(0.6, u[0], 1e-9);
        assertEquals(0.8, u[1], 1e-9);
        double[] z = ZagredGeometry.unit(0, 0);
        assertEquals(0.0, z[0], 1e-9);
        assertEquals(1.0, z[1], 1e-9, "a vector with no length faces +z");
        double[] r = ZagredGeometry.rotate(1, 0, Math.PI / 2);
        assertEquals(0.0, r[0], 1e-9);
        assertEquals(1.0, r[1], 1e-9);
    }

    @Test
    void theHaltBandSparesTheCoreAndTheFarSide() {
        double core = ZagredMath.haltCore(), outer = ZagredMath.haltOuter(0);
        assertFalse(ZagredGeometry.inBand(core - 0.01, core, outer), "standing close is safe");
        assertTrue(ZagredGeometry.inBand(core, core, outer));
        assertTrue(ZagredGeometry.inBand((core + outer) / 2, core, outer));
        assertTrue(ZagredGeometry.inBand(outer, core, outer));
        assertFalse(ZagredGeometry.inBand(outer + 0.01, core, outer), "leaving the ring is safe");
    }

    @Test
    void theWaveHurtsOnlyWithinHalfABandOfTheRing() {
        assertTrue(ZagredGeometry.onRing(6.0, 6.0, 1.3));
        assertTrue(ZagredGeometry.onRing(7.3, 6.0, 1.3));
        assertFalse(ZagredGeometry.onRing(7.4, 6.0, 1.3));
        assertFalse(ZagredGeometry.onRing(4.6, 6.0, 1.3));
        // a foe standing still is passed over by the wave for about two rings (the rings are 1.6 apart, the band 2.6 wide)
        int hits = 0;
        for (int k = 0; k < ZagredMath.fallRings(0); k++) if (ZagredGeometry.onRing(8.0, ZagredMath.fallRingRadius(k), ZagredMath.fallBand())) hits++;
        assertTrue(hits >= 1 && hits <= 2, "rings that reach one spot: " + hits);
    }

    @Test
    void distanceToASegment() {
        assertEquals(0.0, ZagredGeometry.distanceToSegment(5, 0, 0, 0, 10, 0), 1e-9);
        assertEquals(3.0, ZagredGeometry.distanceToSegment(5, 3, 0, 0, 10, 0), 1e-9);
        assertEquals(5.0, ZagredGeometry.distanceToSegment(-3, 4, 0, 0, 10, 0), 1e-9, "beyond the start the distance is to the start");
        assertEquals(5.0, ZagredGeometry.distanceToSegment(13, 4, 0, 0, 10, 0), 1e-9, "beyond the end the distance is to the end");
        assertEquals(5.0, ZagredGeometry.distanceToSegment(3, 4, 0, 0, 0, 0), 1e-9, "a segment of no length is a point");
    }

    @Test
    void theSentenceIsACurveOfLettersLeadingTowardsTheTarget() {
        double[][] c = ZagredGeometry.curve(0, 0, 0, 1, 7, 1);
        assertEquals(7, c.length);
        for (int i = 1; i < c.length; i++) assertTrue(c[i][1] > c[i - 1][1], "each letter is further along the line");
        assertEquals(3.0, c[0][1], 1e-9, "the first letter is three blocks out");
        assertEquals(0.0, c[0][0], 1e-9, "and the first sway is zero");
        double[][] mirror = ZagredGeometry.curve(0, 0, 0, 1, 7, -1);
        for (int i = 0; i < c.length; i++) assertEquals(-c[i][0], mirror[i][0], 1e-9, "the other bend is the mirror image");
        assertEquals(0, ZagredGeometry.curve(0, 0, 1, 0, 0, 1).length);
        for (int i = 1; i < c.length; i++) {
            double dx = c[i][0] - c[i - 1][0], dz = c[i][1] - c[i - 1][1];
            assertTrue(Math.sqrt(dx * dx + dz * dz) < 4.5, "the gap between two letters can always be walked: " + Math.sqrt(dx * dx + dz * dz));
        }
    }

    @Test
    void theFieldOfMarksIsDeterministicInsideTheSpreadAndSpacedOut() {
        double[][] a = ZagredGeometry.spots(10, -4, 10, 6.0, 2.2, 42L);
        double[][] b = ZagredGeometry.spots(10, -4, 10, 6.0, 2.2, 42L);
        assertEquals(10, a.length);
        int close = 0;
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i][0], b[i][0], 1e-12);
            assertEquals(a[i][1], b[i][1], 1e-12);
            double dx = a[i][0] - 10, dz = a[i][1] + 4;
            assertTrue(Math.sqrt(dx * dx + dz * dz) <= 6.0 + 1e-9, "inside the spread");
            for (int j = 0; j < i; j++) {
                double ox = a[i][0] - a[j][0], oz = a[i][1] - a[j][1];
                if (Math.sqrt(ox * ox + oz * oz) < 2.2) close++;
            }
        }
        assertTrue(close <= 3, "marks are kept apart where there is room: " + close + " pairs too close");
        assertEquals(0, ZagredGeometry.spots(0, 0, 0, 5, 2, 1L).length);
    }
}
