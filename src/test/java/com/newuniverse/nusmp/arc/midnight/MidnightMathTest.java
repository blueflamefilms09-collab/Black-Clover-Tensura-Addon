package com.newuniverse.nusmp.arc.midnight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the telegraph and the hit test of every Midnight Sun strike call the same geometry, and the one thing a boss must never do is hit where nothing was shown, so every edge (lane ends, ring rim, cone corner, the spot behind a body) is pinned here. */
class MidnightMathTest {

    @Test
    void segmentDistanceHasTheRightEnds() {
        assertEquals(0.0, MidnightMath.distToSegment(5, 0, 0, 0, 10, 0), 1e-9, "on the lane");
        assertEquals(2.0, MidnightMath.distToSegment(5, 2, 0, 0, 10, 0), 1e-9, "beside the lane");
        assertEquals(3.0, MidnightMath.distToSegment(-3, 0, 0, 0, 10, 0), 1e-9, "behind the start: the distance to the start point");
        assertEquals(5.0, MidnightMath.distToSegment(13, 4, 0, 0, 10, 0), 1e-9, "past the end: the distance to the end point");
        assertEquals(5.0, MidnightMath.distToSegment(3, 4, 0, 0, 0, 0), 1e-9, "a zero-length lane is a point");
    }

    @Test
    void alongSegmentOrdersFoesFromTheBossOutwards() {
        assertEquals(0.0, MidnightMath.alongSegment(-4, 1, 0, 0, 10, 0), 1e-9);
        assertEquals(0.5, MidnightMath.alongSegment(5, 7, 0, 0, 10, 0), 1e-9);
        assertEquals(1.0, MidnightMath.alongSegment(40, 0, 0, 0, 10, 0), 1e-9);
        assertEquals(0.0, MidnightMath.alongSegment(1, 1, 2, 2, 2, 2), 1e-9, "degenerate lane");
        assertTrue(MidnightMath.alongSegment(2, 0, 0, 0, 10, 0) < MidnightMath.alongSegment(8, 0, 0, 0, 10, 0));
    }

    @Test
    void coneEdgesAreInclusiveAndBodiesWidenThem() {
        // a cone pointing along +x, 6 blocks long, 45 degrees each side
        double half = Math.PI / 4;
        assertTrue(MidnightMath.inCone(3, 0, 1, 0, 6, half, 0), "straight ahead");
        assertTrue(MidnightMath.inCone(6, 0, 1, 0, 6, half, 0), "the very end");
        assertFalse(MidnightMath.inCone(6.5, 0, 1, 0, 6, half, 0), "past the end");
        assertTrue(MidnightMath.inCone(3, 3, 1, 0, 6, half, 0), "exactly on the edge");
        assertFalse(MidnightMath.inCone(3, 3.6, 1, 0, 6, half, 0), "outside the edge");
        assertTrue(MidnightMath.inCone(3, 3.6, 1, 0, 6, half, 0.6), "a wide body touching the edge is hit");
        assertFalse(MidnightMath.inCone(-3, 0, 1, 0, 6, half, 0), "behind");
        assertTrue(MidnightMath.inCone(0, 0, 1, 0, 6, half, 0), "on the tip");
        assertFalse(MidnightMath.inCone(3, 0, 0, 0, 6, half, 0), "no direction, no cone");
        assertTrue(MidnightMath.inCone(6.4, 0, 1, 0, 6, half, 0.5), "the body's radius extends the reach");
    }

    @Test
    void ringIsInclusiveAndCountsTheBody() {
        assertTrue(MidnightMath.inRing(0, 0, 3, 0, 3, 0));
        assertFalse(MidnightMath.inRing(0, 0, 3.01, 0, 3, 0));
        assertTrue(MidnightMath.inRing(0, 0, 3.3, 0, 3, 0.4));
        assertTrue(MidnightMath.inRing(5, 5, 5, 5, 0, 0), "a ring of nothing still holds its centre");
    }

    @Test
    void heightWindowIsInclusive() {
        assertTrue(MidnightMath.withinHeight(0, 4, 4));
        assertTrue(MidnightMath.withinHeight(-4, 4, 4));
        assertTrue(MidnightMath.withinHeight(4, 4, 4));
        assertFalse(MidnightMath.withinHeight(4.1, 4, 4));
        assertFalse(MidnightMath.withinHeight(-4.1, 4, 4));
    }

    @Test
    void fanIsCentredAndEvenlySpaced() {
        assertEquals(1, MidnightMath.fan(1, 1.0).length);
        assertEquals(0.0, MidnightMath.fan(1, 1.0)[0], 1e-12, "one bolt goes straight");
        for (int n = 2; n <= 7; n++) {
            double[] f = MidnightMath.fan(n, 1.2);
            assertEquals(n, f.length);
            double sum = 0;
            for (double a : f) sum += a;
            assertEquals(0.0, sum, 1e-9, "never leans to one side: n=" + n);
            assertEquals(-0.6, f[0], 1e-9);
            assertEquals(0.6, f[n - 1], 1e-9);
            for (int i = 1; i < n; i++) assertEquals(1.2 / (n - 1), f[i] - f[i - 1], 1e-9);
        }
        assertEquals(1, MidnightMath.fan(0, 1.0).length, "a fan is never empty");
    }

    @Test
    void ringPointsAreEvenlyAroundTheCircle() {
        double[][] p = MidnightMath.ring(4, 5, 0);
        assertEquals(4, p.length);
        assertEquals(5.0, p[0][0], 1e-9);
        assertEquals(0.0, p[0][1], 1e-9);
        assertEquals(0.0, p[1][0], 1e-9);
        assertEquals(5.0, p[1][1], 1e-9);
        for (double[] q : p) assertEquals(5.0, Math.hypot(q[0], q[1]), 1e-9);
        double[][] r = MidnightMath.ring(3, 2, Math.PI / 2);
        assertEquals(0.0, r[0][0], 1e-9);
        assertEquals(2.0, r[0][1], 1e-9);
    }

    @Test
    void aFlashOnlyBlindsThoseWhoLookAtIt() {
        double quarter = Math.PI / 2.5;
        assertTrue(MidnightMath.looksToward(1, 0, 10, 0, quarter), "facing it");
        assertTrue(MidnightMath.looksToward(1, 0.3, 10, 0, quarter), "roughly facing it");
        assertFalse(MidnightMath.looksToward(-1, 0, 10, 0, quarter), "back turned");
        assertFalse(MidnightMath.looksToward(0, 1, 10, 0, quarter), "side on");
        assertTrue(MidnightMath.looksToward(1, 0, 0, 0, quarter), "standing in the light itself");
        assertFalse(MidnightMath.looksToward(0, 0, 10, 0, quarter), "looking straight up or down sees no flat direction");
    }

    @Test
    void behindIsOppositeTheFacing() {
        double[] b = MidnightMath.behind(10, 10, 1, 0, 2);
        assertEquals(8.0, b[0], 1e-9);
        assertEquals(10.0, b[1], 1e-9);
        double[] c = MidnightMath.behind(0, 0, 0, -3, 2.5);
        assertEquals(0.0, c[0], 1e-9);
        assertEquals(2.5, c[1], 1e-9);
        double[] d = MidnightMath.behind(4, 4, 0, 0, 2);
        assertEquals(4.0, d[0], 1e-9);
        assertEquals(6.0, d[1], 1e-9, "no facing: from the south, but always defined");
    }

    @Test
    void scheduleSpacesTheStrikes() {
        int[] s = MidnightMath.schedule(4, 10, 6);
        assertEquals(4, s.length);
        assertEquals(10, s[0]);
        assertEquals(16, s[1]);
        assertEquals(28, s[3]);
        assertEquals(0, MidnightMath.schedule(0, 5, 5).length);
        assertEquals(0, MidnightMath.schedule(-3, 5, 5).length);
        assertEquals(0, MidnightMath.schedule(1, -4, 5)[0], "never before now");
    }

    @Test
    void aMarkIsFairWhenItCanBeWalkedOut() {
        // a 4.5 block ring with 24 ticks of warning: 5.2 blocks can be walked, so it can be left
        assertTrue(MidnightMath.escapeReach(24) > 4.5);
        assertTrue(MidnightMath.escapeReach(16) < 4.5, "sixteen ticks is not enough for a 4.5 ring: the boss tables must give such marks more time");
        assertEquals(0.0, MidnightMath.escapeReach(0), 1e-12);
    }

    @Test
    void clampsAndLerp() {
        assertEquals(3, MidnightMath.clamp(9, 0, 3));
        assertEquals(0, MidnightMath.clamp(-9, 0, 3));
        assertEquals(1.5, MidnightMath.clamp(1.5, 0.0, 3.0), 1e-12);
        assertEquals(5.0, MidnightMath.lerp(0, 10, 0.5), 1e-12);
        assertEquals(10.0, MidnightMath.lerp(0, 10, 4), 1e-12, "t is clamped");
    }

    @Test
    void aGuardCutsWhatItCanCut() {
        assertEquals(1.0, MidnightMath.guardFactor(false, false, true), 1e-12);
        assertEquals(0.5, MidnightMath.guardFactor(true, false, false), 1e-12, "a shield halves any blow");
        assertEquals(0.5, MidnightMath.guardFactor(true, false, true), 1e-12);
        assertEquals(0.55, MidnightMath.guardFactor(false, true, true), 1e-12, "anti-magic cuts a spell");
        assertEquals(1.0, MidnightMath.guardFactor(false, true, false), 1e-12, "but not a claw");
        assertEquals(0.3, MidnightMath.guardFactor(true, true, true), 1e-12, "both together");
        assertEquals(0.5, MidnightMath.guardFactor(true, true, false), 1e-12);
        assertTrue(MidnightMath.guardFactor(true, true, true) > 0.25, "a guard never makes a blow vanish");
    }
}
