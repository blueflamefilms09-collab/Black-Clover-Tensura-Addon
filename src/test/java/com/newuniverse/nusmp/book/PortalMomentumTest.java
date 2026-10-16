package com.newuniverse.nusmp.book;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: a crossing must keep heading the way a door pair implies; these pin the angles, the rotation and the exit point. */
class PortalMomentumTest {
    private static final double EPS = 1e-9;

    @Test
    void facingPointsFromGateToCaster() {
        assertEquals(0, PortalMomentum.facing(0, 0, 5, 0), EPS);
        assertEquals(Math.PI / 2, PortalMomentum.facing(0, 0, 0, 5), EPS);
        assertEquals(0, PortalMomentum.facing(1, 1, 1, 1), EPS);
    }

    @Test
    void walkingStraightThroughTwoParallelGatesKeepsDirection() {
        // gate A faces +x (front at x>0), gate B faces +x as well: walking -x into A must leave B walking +x
        double t = PortalMomentum.turn(0, 0);
        double[] v = PortalMomentum.rotate(-1, 0, t);
        assertEquals(1, v[0], EPS);
        assertEquals(0, v[1], EPS);
    }

    @Test
    void exitDirectionIsTheExitGatesFrontNormal() {
        double fa = 0.7, fb = -2.1;
        double[] v = PortalMomentum.rotate(Math.cos(fa + Math.PI), Math.sin(fa + Math.PI), PortalMomentum.turn(fa, fb));
        assertEquals(Math.cos(fb), v[0], 1e-9);
        assertEquals(Math.sin(fb), v[1], 1e-9);
    }

    @Test
    void rotationKeepsSpeed() {
        double[] v = PortalMomentum.rotate(0.3, -0.4, 1.234);
        assertEquals(0.5, Math.hypot(v[0], v[1]), 1e-12);
    }

    @Test
    void turnIsWrapped() {
        for (double a = -7; a < 7; a += 0.37) {
            double t = PortalMomentum.turn(a, a * 0.5 + 1);
            assertTrue(t > -Math.PI - 1e-12 && t <= Math.PI + 1e-12);
        }
    }

    @Test
    void sideOfAGate() {
        assertEquals(1, PortalMomentum.side(0, 0, 0, 1, 0));
        assertEquals(-1, PortalMomentum.side(0, 0, 0, -1, 0));
        assertEquals(0, PortalMomentum.side(0, 0, 0, 0, 3));
    }

    @Test
    void enteringFromTheFrontExitsInFrontOfTheOtherGate() {
        double[] at = PortalMomentum.exitPoint(0, 0, 0, 10, 10, Math.PI / 2, 0.5, 0, 1);
        assertEquals(10, at[0], 1e-9);
        assertEquals(10 + PortalMomentum.EXIT_OFFSET, at[1], 1e-9);
        double[] back = PortalMomentum.exitPoint(0, 0, 0, 10, 10, Math.PI / 2, -0.5, 0, -1);
        assertEquals(10 - PortalMomentum.EXIT_OFFSET, back[1], 1e-9);
    }

    @Test
    void sidewaysOffsetIsMirroredAcrossTheDoor() {
        // A faces +x, its tangent is +z; B faces +x too: an entity 0.4 to A's +z side comes out 0.4 to B's -z side
        double[] at = PortalMomentum.exitPoint(0, 0, 0, 10, 0, 0, 0.3, 0.4, 1);
        assertEquals(10 + PortalMomentum.EXIT_OFFSET, at[0], 1e-9);
        assertEquals(-0.4, at[1], 1e-9);
    }
}
