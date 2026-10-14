package com.newuniverse.nusmp.relics.djinn.weapon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeaponGeoTest {
    @Test
    void aConeHoldsWhatIsAheadAndWithinRange() {
        assertTrue(WeaponGeo.inCone(0, 0, 0, 1, 0, 5, 6, 30));
        assertFalse(WeaponGeo.inCone(0, 0, 0, 1, 0, 7, 6, 30), "out of range");
        assertFalse(WeaponGeo.inCone(0, 0, 0, 1, 0, -2, 6, 60), "behind");
        assertTrue(WeaponGeo.inCone(0, 0, 0, 1, 2, 4, 6, 30), "26.6 degrees off");
        assertFalse(WeaponGeo.inCone(0, 0, 0, 1, 4, 4, 8, 30), "45 degrees off");
        assertTrue(WeaponGeo.inCone(1, 1, 0, 1, 1, 1, 3, 10), "the origin is inside");
    }

    @Test
    void aFullCircleConeIsEveryDirection() {
        for (int a = 0; a < 360; a += 15) assertTrue(WeaponGeo.inCone(0, 0, 0, 1, Math.cos(Math.toRadians(a)) * 3, Math.sin(Math.toRadians(a)) * 3, 4, 180));
    }

    @Test
    void turnRotatesByTheAngle() {
        double[] v = WeaponGeo.turn(1, 0, 90);
        assertEquals(0, v[0], 1e-9);
        assertEquals(1, v[1], 1e-9);
        double[] w = WeaponGeo.turn(0, 1, -180);
        assertEquals(0, w[0], 1e-9);
        assertEquals(-1, w[1], 1e-9);
    }

    @Test
    void segmentDistanceIsThePerpendicularOrTheEndpoint() {
        assertEquals(1.0, WeaponGeo.segmentDistance(0, 0, 0, 10, 0, 0, 5, 1, 0), 1e-9);
        assertEquals(5.0, WeaponGeo.segmentDistance(0, 0, 0, 10, 0, 0, 15, 0, 0), 1e-9, "past the end it is the distance to the end");
        assertEquals(5.0, WeaponGeo.segmentDistance(0, 0, 0, 10, 0, 0, -3, 4, 0), 1e-9);
        assertEquals(5.0, WeaponGeo.segmentDistance(1, 1, 1, 1, 1, 1, 4, 5, 1), 1e-9, "a point");
    }

    @Test
    void aWallIsAheadAndNarrow() {
        assertTrue(WeaponGeo.inWall(0, 0, 0, 1, 2, 3, 2, 4, 3.5));
        assertFalse(WeaponGeo.inWall(0, 0, 0, 1, 4, 3, 2, 4, 3.5), "too wide");
        assertFalse(WeaponGeo.inWall(0, 0, 0, 1, 0, 1, 2, 4, 3.5), "too near");
        assertFalse(WeaponGeo.inWall(0, 0, 0, 1, 0, 5, 2, 4, 3.5), "too far");
    }

    @Test
    void twoCrescentsLeaveTheMiddleOut() {
        assertTrue(WeaponGeo.inCrescents(0, 0, 0, 1, 4, 8, 10, 30, 15));
        assertTrue(WeaponGeo.inCrescents(0, 0, 0, 1, -4, 8, 10, 30, 15));
        assertFalse(WeaponGeo.inCrescents(0, 0, 0, 1, 0, 8, 10, 30, 10), "dead ahead is between the crescents");
    }

    @Test
    void aChainLosesTheSameShareEachLink() {
        assertEquals(1.0, WeaponGeo.chainFactor(0, 0.85), 1e-12);
        assertEquals(0.7225, WeaponGeo.chainFactor(2, 0.85), 1e-9);
        assertEquals(1.0, WeaponGeo.chainFactor(-3, 0.85), 1e-12);
    }
}
