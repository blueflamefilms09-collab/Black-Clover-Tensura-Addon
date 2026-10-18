package com.newuniverse.nusmp.heroweapon.weapon.glassworld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: a dash, a pull and a cone are arithmetic that cannot be seen to be wrong without a game, so they are pinned with numbers: the horizontal speed is the
 * requested one whatever the pitch, a look at the sky does not launch the wielder, a wall shortens a dash, a pull never exceeds its top speed and points the
 * right way, and the cone test agrees with the angle.
 */
class GeoTest {
    @Test
    void aDashKeepsItsHorizontalSpeedWhateverThePitch() {
        Geo.Impulse level = Geo.dash(0, 0, 1, 0.9, 0.3, 0.2, 0.18, false);
        assertEquals(0.9, level.horizontal(), 1e-9);
        assertEquals(0.0, level.y(), 1e-9);
        assertEquals(0.9, level.z(), 1e-9);
        Geo.Impulse steep = Geo.dash(0.0, 0.9, 0.436, 0.9, 0.3, 0.2, 0.0, false);
        assertEquals(0.9, steep.horizontal(), 1e-9, "the horizontal part is the speed even when looking up");
        assertEquals(0.3, steep.y(), 1e-9, "the climb is clamped");
        Geo.Impulse down = Geo.dash(1, -0.9, 0, 0.9, 0.3, 0.2, 0.0, false);
        assertEquals(-0.2, down.y(), 1e-9, "the fall is clamped");
        assertEquals(0.9, down.x(), 1e-9);
    }

    @Test
    void theGroundLiftIsAddedOnlyOnTheGround() {
        assertEquals(0.18, Geo.dash(1, 0, 0, 0.5, 0.3, 0.2, 0.18, true).y(), 1e-9);
        assertEquals(0.0, Geo.dash(1, 0, 0, 0.5, 0.3, 0.2, 0.18, false).y(), 1e-9);
        Geo.Impulse straightUp = Geo.dash(0, 1, 0, 0.5, 0.3, 0.2, 0.1, true);
        assertEquals(0.0, straightUp.horizontal(), 1e-9, "no horizontal part when looking straight up");
        assertEquals(0.4, straightUp.y(), 1e-9, "the clamped climb plus the lift");
    }

    @Test
    void dashSpeedAndStopShort() {
        assertEquals(1.12, Geo.dashSpeedFor(7.0), 1e-9);
        assertEquals(0.2, Geo.dashSpeedFor(0.5), 1e-9, "never slower than 0.2");
        assertEquals(1.4, Geo.dashSpeedFor(40.0), 1e-9, "never faster than 1.4");
        assertEquals(7.0, Geo.stopShort(7.0, Double.POSITIVE_INFINITY, 0.8), 1e-9, "no wall");
        assertEquals(3.2, Geo.stopShort(7.0, 4.0, 0.8), 1e-9, "stops short of the wall");
        assertEquals(0.0, Geo.stopShort(7.0, 0.5, 0.8), 1e-9, "a wall at the wielder's feet gives no dash");
        assertEquals(2.0, Geo.stopShort(2.0, 9.0, 0.8), 1e-9, "never farther than wanted");
    }

    @Test
    void aPullPointsAtThePointAndNeverExceedsItsTopSpeed() {
        Geo.Impulse far = Geo.pull(10, 0, 0, 0.4, 0.9);
        assertEquals(0.9, far.x(), 1e-9);
        assertEquals(0.0, far.z(), 1e-9);
        Geo.Impulse near = Geo.pull(0, 0, 1.0, 0.4, 0.9);
        assertEquals(0.4, near.z(), 1e-9, "a near thing is drawn gently");
        Geo.Impulse diagonal = Geo.pull(3, 4, 0, 1.0, 100.0);
        assertEquals(5.0, Math.sqrt(diagonal.x() * diagonal.x() + diagonal.y() * diagonal.y()), 1e-9);
        assertEquals(0.6, diagonal.x() / 5.0, 1e-9);
        Geo.Impulse there = Geo.pull(0.1, 0.1, 0.0, 0.4, 0.9);
        assertEquals(0.0, there.x(), 1e-9, "already there");
        assertEquals(0.0, there.y(), 1e-9);
    }

    @Test
    void theConeAgreesWithTheAngle() {
        double c60 = Geo.cosHalf(60);
        assertEquals(0.5, c60, 1e-9);
        assertTrue(Geo.inCone(0, 0, 5, 0, 0, 1, c60), "straight ahead");
        assertTrue(Geo.inCone(4, 0, 5, 0, 0, 1, c60), "about 38 degrees off");
        assertFalse(Geo.inCone(5, 0, 2, 0, 0, 1, c60), "about 68 degrees off");
        assertFalse(Geo.inCone(0, 0, -3, 0, 0, 1, c60), "behind");
        assertTrue(Geo.inCone(0, 0, 0, 0, 0, 1, c60), "the apex counts");
        assertFalse(Geo.inCone(1, 0, 1, 0, 0, 0, c60), "no look direction");
        assertEquals(-1.0, Geo.cosHalf(180), 1e-9);
        assertEquals(1.0, Geo.cosHalf(-20), 1e-9, "a negative angle is clamped to zero");
    }

    @Test
    void awayFromPointsAwayAndHasAFallback() {
        double[] a = Geo.awayFrom(5, 0, 2, 0, 0, 1);
        assertEquals(1.0, a[0], 1e-9);
        assertEquals(0.0, a[1], 1e-9);
        double[] same = Geo.awayFrom(1, 1, 1, 1, 0, -2);
        assertEquals(0.0, same[0], 1e-9);
        assertEquals(-1.0, same[1], 1e-9, "the fallback is normalised");
        double[] none = Geo.awayFrom(1, 1, 1, 1, 0, 0);
        assertEquals(1.0, Math.sqrt(none[0] * none[0] + none[1] * none[1]), 1e-9, "still a unit vector");
    }
}
