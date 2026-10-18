package com.newuniverse.nusmp.heroweapon.weapon.vassalkit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: a sign or an off-by-one in the shared geometry hides until someone plays, so yaw directions, fans, segment distance, cones, reflection and the pane sweep are proved exactly. */
class GeoTest {
    private static final double E = 1e-9;

    @Test
    void yawZeroLooksAlongPlusZAndNinetyAlongMinusX() {
        double[] a = Geo.yawDir(0), b = Geo.yawDir(90), c = Geo.yawDir(180);
        assertEquals(0.0, a[0], E);
        assertEquals(1.0, a[1], E);
        assertEquals(-1.0, b[0], E);
        assertEquals(0.0, b[1], E);
        assertEquals(-1.0, c[1], E);
    }

    @Test
    void turningByAnAngleIsAddingItToTheYaw() {
        double[] d = Geo.yawDir(30);
        double[] t = Geo.turn(d[0], d[1], 25);
        double[] want = Geo.yawDir(55);
        assertEquals(want[0], t[0], E);
        assertEquals(want[1], t[1], E);
        double[] back = Geo.turn(t[0], t[1], -25);
        assertEquals(d[0], back[0], E);
        assertEquals(d[1], back[1], E);
    }

    @Test
    void aFanOfFiveSpansTheSpreadAndKeepsOneLineStraight() {
        double[] fwd = Geo.yawDir(0);
        double[][] f = Geo.fan(fwd[0], fwd[1], 5, 80);
        assertEquals(5, f.length);
        assertEquals(0.0, f[2][0], E, "the middle line points straight ahead");
        assertEquals(1.0, f[2][1], E);
        double[] left = Geo.yawDir(-40), right = Geo.yawDir(40);
        assertEquals(left[0], f[0][0], E);
        assertEquals(left[1], f[0][1], E);
        assertEquals(right[0], f[4][0], E);
        assertEquals(right[1], f[4][1], E);
        for (double[] d : f) assertEquals(1.0, Math.hypot(d[0], d[1]), E, "every line is a unit direction");
        assertEquals(1, Geo.fan(fwd[0], fwd[1], 0, 80).length, "a fan never has fewer than one line");
    }

    @Test
    void distanceToASegmentUsesTheNearestPointAndClampsAtTheEnds() {
        assertEquals(2.0, Geo.segDist(5, 2, 0, 0, 0, 0, 10, 0, 0), E);
        assertEquals(5.0, Geo.segDist(-3, 4, 0, 0, 0, 0, 10, 0, 0), E, "beyond the start the end point counts");
        assertEquals(5.0, Geo.segDist(13, 4, 0, 0, 0, 0, 10, 0, 0), E, "beyond the end the end point counts");
        assertEquals(0.5, Geo.segParam(5, 9, 9, 0, 0, 0, 10, 0, 0), E);
        assertEquals(0.0, Geo.segParam(1, 1, 1, 3, 3, 3, 3, 3, 3), E, "a point-sized segment does not divide by zero");
    }

    @Test
    void aConeAcceptsWhatIsWithinItsHalfAngle() {
        assertTrue(Geo.inCone(10, 0, 0, 1, 0, 0, 30));
        assertTrue(Geo.inCone(10, 5, 0, 1, 0, 0, 30), "26.6 degrees is inside 30");
        assertFalse(Geo.inCone(10, 7, 0, 1, 0, 0, 30), "35 degrees is outside 30");
        assertFalse(Geo.inCone(-1, 0, 0, 1, 0, 0, 90));
        assertTrue(Geo.inCone(0, 0, 0, 1, 0, 0, 10), "the wielder's own spot is inside");
    }

    @Test
    void reflectingOffAWallFlipsTheNormalPartOnly() {
        double[] r = Geo.reflect(3, 1, -2, 0, 0, 1);
        assertEquals(3.0, r[0], E);
        assertEquals(1.0, r[1], E);
        assertEquals(2.0, r[2], E);
    }

    @Test
    void aShotThatCrossesThePaneIsCaughtEvenWhenItMovesPastTheSlabInOneTick() {
        // a pane at the origin facing +z (towards the foes), radius 1.5, 0.5 thick on each side
        assertTrue(Geo.sweepsDisc(0, 0, 0, 0, 0, 1, 1.5, 0.5, 0, 0, 2, 0, 0, -2), "it jumped from in front to behind in one tick");
        assertTrue(Geo.sweepsDisc(0, 0, 0, 0, 0, 1, 1.5, 0.5, 0, 0, 3, 0, 0, 0.4), "it ended its tick just inside the slab");
        assertTrue(Geo.sweepsDisc(0, 0, 0, 0, 0, 1, 1.5, 0.5, 0, 0, 0.4, 0, 0, -1), "it started the tick inside the slab");
        assertFalse(Geo.sweepsDisc(0, 0, 0, 0, 0, 1, 1.5, 0.5, 0, 0, 9, 0, 0, 6), "still far in front of the slab");
        assertFalse(Geo.sweepsDisc(0, 0, 0, 0, 0, 1, 1.5, 0.5, 0, 0, -3, 0, 0, -6), "already well behind it");
    }

    @Test
    void aShotOutsideTheDiscOrLeavingItsFaceIsNotCaught() {
        assertFalse(Geo.sweepsDisc(0, 0, 0, 0, 0, 1, 1.5, 0.5, 3, 0, 2, 3, 0, -2), "it passes 3 blocks to the side");
        assertFalse(Geo.sweepsDisc(0, 0, 0, 0, 0, 1, 1.5, 0.5, 0, 0, -2, 0, 0, 2), "it comes from behind and moves away from the face");
        assertTrue(Geo.sweepsDisc(0, 0, 0, 0, 0, 1, 1.5, 0.5, 1.2, 0.5, 1, 1.2, 0.5, -1), "just inside the rim");
        assertFalse(Geo.sweepsDisc(0, 0, 0, 0, 0, 1, 1.5, 0.5, 1.4, 1.0, 1, 1.4, 1.0, -1), "just outside the rim");
    }

    @Test
    void aPullIsGentleNearTheGoalAndStopsThere() {
        assertEquals(0.0, Geo.pullSpeed(2.0, 2.5, 1.5), E, "already inside the stopping distance");
        assertEquals(0.0, Geo.pullSpeed(2.5, 2.5, 1.5), E);
        assertEquals(0.2 + 1.0 * 0.35, Geo.pullSpeed(3.5, 2.5, 1.5), E, "one block short of the stop");
        assertEquals(1.5, Geo.pullSpeed(30.0, 2.5, 1.5), E, "never faster than the maximum");
        assertTrue(Geo.pullSpeed(4.0, 2.5, 1.5) < Geo.pullSpeed(8.0, 2.5, 1.5), "it slows as it nears");
    }

    @Test
    void aScatteredAimIsAUnitVectorTurnedAndRaisedByTheAngles() {
        double[] same = Geo.aimOff(0, 0, 1, 0, 0);
        assertEquals(0.0, same[0], E);
        assertEquals(0.0, same[1], E);
        assertEquals(1.0, same[2], E);
        double[] right = Geo.aimOff(0, 0, 1, 90, 0);
        assertEquals(-1.0, right[0], E, "yaw 90 looks along -x");
        assertEquals(0.0, right[2], 1e-9);
        double[] up = Geo.aimOff(0, 0, 1, 0, 30);
        assertEquals(0.5, up[1], 1e-9);
        assertEquals(Math.cos(Math.toRadians(30)), up[2], 1e-9);
        for (double[] v : new double[][]{Geo.aimOff(1, 2, 3, 17, -9), Geo.aimOff(0, 1, 0, 25, 5), Geo.aimOff(0, -5, 0.0001, 40, 80)})
            assertEquals(1.0, Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]), 1e-9, "always a unit vector");
        double[] zero = Geo.aimOff(0, 0, 0, 10, 10);
        assertEquals(1.0, zero[2], E, "a zero look falls back to +z");
    }

    @Test
    void aScatteredAimRaisedFromAnAlreadyRaisedLookStaysRaised() {
        double[] v = Geo.aimOff(0, 0.9, 0.4359, 0, 10);
        assertTrue(v[1] > 0.9, "raised from an already raised look");
    }
}
