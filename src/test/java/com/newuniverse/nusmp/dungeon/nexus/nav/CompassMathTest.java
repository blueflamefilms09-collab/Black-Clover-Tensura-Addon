package com.newuniverse.nusmp.dungeon.nexus.nav;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The needle of the Anti-Bird Compass: where it points, how it swings, how it spins and idles. */
class CompassMathTest {
    private static final double EPS = 1e-9;

    /** The yaw (degrees) of a player facing along (dx, dz): Minecraft's yaw 0 faces +z (south), 90 faces -x (west). */
    private static double yawToward(double dx, double dz) { return Math.toDegrees(Math.atan2(-dx, dz)); }

    @Test
    void wrapKeepsNumbersInsideOneTurn() {
        assertEquals(0.25, CompassMath.wrap01(1.25), EPS);
        assertEquals(0.75, CompassMath.wrap01(-0.25), EPS);
        assertEquals(0.0, CompassMath.wrap01(3.0), EPS);
        assertTrue(CompassMath.wrap01(-1e-18) < 1.0, "a tiny negative number must not round up to 1");
        for (double v = -7.3; v < 7.3; v += 0.37) {
            double w = CompassMath.wrap01(v);
            assertTrue(w >= 0 && w < 1, "" + v);
        }
    }

    @Test
    void theBearingIsInTurns() {
        assertEquals(0.0, CompassMath.bearing(0, 0, 10, 0), EPS, "+x");
        assertEquals(0.25, CompassMath.bearing(0, 0, 0, 10), EPS, "+z");
        assertEquals(0.5, Math.abs(CompassMath.bearing(0, 0, -10, 0)), EPS, "-x");
        assertEquals(-0.25, CompassMath.bearing(0, 0, 0, -10), EPS, "-z");
    }

    @Test
    void aTargetDeadAheadIsAtZeroTurns() {
        double[][] targets = {{40, 0}, {-40, 0}, {0, 40}, {0, -40}, {25, -31}, {-12, 7}};
        for (double[] t : targets) {
            double yaw = yawToward(t[0], t[1]);
            double angle = CompassMath.needle(yaw, CompassMath.bearing(0, 0, t[0], t[1]), null, 0);
            assertTrue(angle < 1e-9 || angle > 1 - 1e-9, "target " + t[0] + "," + t[1] + " gave " + angle);
        }
    }

    @Test
    void aTargetToTheRightIsAQuarterTurnAndToTheLeftThreeQuarters() {
        // facing south (+z), a target to the west (-x) is on the right hand
        double yaw = 0;
        assertEquals(0.25, CompassMath.needle(yaw, CompassMath.bearing(0, 0, -40, 0), null, 0), 1e-9, "right");
        assertEquals(0.75, CompassMath.needle(yaw, CompassMath.bearing(0, 0, 40, 0), null, 0), 1e-9, "left");
        assertEquals(0.5, CompassMath.needle(yaw, CompassMath.bearing(0, 0, 0, -40), null, 0), 1e-9, "behind");
    }

    @Test
    void turningRightMakesTheNeedleTurnLeft() {
        double bearing = CompassMath.bearing(0, 0, 0, 40);     // a target to the south
        double facingSouth = CompassMath.needle(0, bearing, null, 0);
        double turnedRight = CompassMath.needle(20, bearing, null, 0);     // yaw grows turning to the right (west)
        assertEquals(0.0, facingSouth, 1e-9);
        assertEquals(1 - 20 / 360.0, turnedRight, 1e-9);
    }

    @Test
    void theSmoothedNeedleSettlesOnTheExactOne() {
        NeedleWobble w = new NeedleWobble();
        double bearing = CompassMath.bearing(0, 0, -30, 12);
        double exact = CompassMath.needle(123.0, bearing, null, 0);
        double angle = 0;
        for (long t = 1; t <= 400; t++) angle = CompassMath.needle(123.0, bearing, w, t);
        double d = Math.abs(angle - exact);
        assertTrue(Math.min(d, 1 - d) < 1e-4, "settled at " + angle + " expected " + exact);
    }

    @Test
    void theSmoothedNeedleMovesOncePerTickNotOncePerFrame() {
        NeedleWobble w = new NeedleWobble();
        double a = CompassMath.needle(90, 0.1, w, 5);
        double b = CompassMath.needle(90, 0.1, w, 5);
        double c = CompassMath.needle(90, 0.1, w, 5);
        assertEquals(a, b, 0.0);
        assertEquals(a, c, 0.0);
        double d = CompassMath.needle(90, 0.1, w, 6);
        assertNotEquals(a, d);
    }

    @Test
    void theSmoothedNeedleTakesTheShortWayRound() {
        NeedleWobble w = new NeedleWobble();
        // the needle starts at rest at 0; chase 0.95 (which is only 0.05 behind it): it must go down, not 0.95 up
        w.update(1, 0.95);
        assertTrue(w.rotation() > 0.9, "moved backwards across zero: " + w.rotation());
    }

    @Test
    void theSpringStaysInsideOneTurnAndDoesNotRunAway() {
        NeedleWobble w = new NeedleWobble();
        for (long t = 1; t < 5000; t++) {
            w.update(t, (t * 0.013) % 1.0);
            assertTrue(w.rotation() >= 0 && w.rotation() < 1);
        }
    }

    @Test
    void theNeedleSpinsWildlyInWarpedSpace() {
        double total = 0;
        double prev = CompassMath.spin(0, 3);
        for (int t = 1; t <= 200; t++) {
            double a = CompassMath.spin(t, 3);
            assertTrue(a >= 0 && a < 1, "" + a);
            double d = Math.abs(a - prev);
            total += Math.min(d, 1 - d);
            prev = a;
        }
        assertTrue(total > 20.0, "it must turn many times over two hundred ticks, turned " + total);
        assertNotEquals(CompassMath.spin(40.5, 1), CompassMath.spin(40.5, 2), "two compasses spin a little differently");
        assertEquals(CompassMath.spin(40.5, 1), CompassMath.spin(40.5, 1), 0.0, "a pure function of the time");
    }

    @Test
    void theIdleNeedleSearchesSlowly() {
        double prev = CompassMath.idle(0, 9);
        double total = 0;
        for (int t = 1; t <= 400; t++) {
            double a = CompassMath.idle(t, 9);
            assertTrue(a >= 0 && a < 1);
            double d = Math.abs(a - prev);
            total += Math.min(d, 1 - d);
            prev = a;
        }
        assertEquals(1.0, total, 0.01, "one turn in twenty seconds");
        double spinTurns = 0, p = CompassMath.spin(0, 9);
        for (int t = 1; t <= 400; t++) {
            double a = CompassMath.spin(t, 9);
            double d = Math.abs(a - p);
            spinTurns += Math.min(d, 1 - d);
            p = a;
        }
        assertTrue(spinTurns > 10 * total, "the wild spin is nothing like the idle");
    }
}
