package com.newuniverse.nusmp.arc.sds;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: a flier is easy to get wrong in ways a fight shows at once (drifting away for ever, never coming down, out-running the player), so the hover, the drift and the dive are bounded and tested. */
class KingFlightTest {
    @Test
    void heHoversOnlyWhereThereIsGroundBelowWithinTheProbe() {
        assertTrue(KingFlight.canHover(0) && KingFlight.canHover(6.0) && KingFlight.canHover(KingFlight.PROBE));
        assertFalse(KingFlight.canHover(Double.NaN), "nothing below: he falls like any mob, he never drifts into the sky");
        assertFalse(KingFlight.canHover(KingFlight.PROBE + 0.1));
        assertFalse(KingFlight.canHover(-1.0));
        assertEquals(0.0, KingFlight.verticalSpeed(Double.NaN, KingFlight.HIGH), 1e-12);
        assertEquals(0.0, KingFlight.verticalSpeed(40, KingFlight.HIGH), 1e-12);
    }

    @Test
    void theVerticalSpeedClimbsAndSinksTowardsTheWantedHeightAndIsBounded() {
        assertEquals(0.0, KingFlight.verticalSpeed(KingFlight.HIGH, KingFlight.HIGH), 1e-12);
        assertTrue(KingFlight.verticalSpeed(1.0, KingFlight.HIGH) > 0);
        assertTrue(KingFlight.verticalSpeed(1.0, KingFlight.HIGH) <= KingFlight.MAX_RISE + 1e-12);
        assertTrue(KingFlight.verticalSpeed(9.0, KingFlight.HIGH) < 0);
        assertTrue(KingFlight.verticalSpeed(13.0, KingFlight.LOW) >= -KingFlight.MAX_SINK - 1e-12);
        assertTrue(KingFlight.verticalSpeed(KingFlight.HIGH, KingFlight.LOW) < 0, "coming down for a close move");
        assertTrue(KingFlight.HIGH - (1.62 + 1.25) > 3.0, "from the ground, even at the top of a jump, a sword's reach (3 blocks from an eye 1.62 up, 1.25 higher at the top of a jump) does not meet his feet at the high hover");
        assertTrue(KingFlight.LOW - 1.62 < 3.0, "at the low hover a sword reaches his feet");
        assertTrue(KingFlight.verticalSpeed(0.0, KingFlight.LANDED) == 0.0);
    }

    @Test
    void theHoverSettlesOnTheWantedHeightFromAnywhere() {
        for (double start : new double[]{0.2, 2.0, 9.0}) {
            double h = start;
            for (int i = 0; i < 200; i++) h += KingFlight.verticalSpeed(h, KingFlight.HIGH);
            assertEquals(KingFlight.HIGH, h, 0.05, "from " + start);
        }
    }

    @Test
    void theBobIsSmall() {
        for (long t = 0; t < 400; t++) assertTrue(Math.abs(KingFlight.bob(t)) <= 0.35 + 1e-9);
    }

    @Test
    void theDriftClosesInBacksOffOrCirclesAndNeverOutRunsASprint() {
        assertTrue(KingFlight.MAX_DRIFT < 0.28, "a sprinting player (0.28 blocks a tick) can always get away from his drift");
        double[] far = KingFlight.drift(30, 7, 13, 1);
        assertTrue(far[0] > 0 && far[1] == 0, "too far: closes in");
        double[] near = KingFlight.drift(2, 7, 13, 1);
        assertTrue(near[0] < 0 && near[1] == 0, "too close: backs off");
        double[] mid = KingFlight.drift(10, 7, 13, 1);
        assertEquals(0.0, mid[0], 1e-12);
        assertEquals(0.07, mid[1], 1e-12);
        assertEquals(-0.07, KingFlight.drift(10, 7, 13, -1)[1], 1e-12);
        for (double d = 0; d < 80; d += 0.5) {
            double[] v = KingFlight.drift(d, 7, 13, 1);
            assertTrue(Math.hypot(v[0], v[1]) <= KingFlight.MAX_DRIFT + 1e-9, "at " + d);
        }
        SdsAsserts.assertNear(new double[]{0, 0}, KingFlight.drift(Double.NaN, 7, 13, 1), 1e-12);
    }

    @Test
    void aDiveTakesAsLongAsItsDistanceNeedsWithinBounds() {
        assertEquals(KingFlight.DIVE_MIN, KingFlight.diveTicks(0));
        assertEquals(KingFlight.DIVE_MIN, KingFlight.diveTicks(-5));
        assertEquals(KingFlight.DIVE_MIN, KingFlight.diveTicks(5));
        assertEquals(14, KingFlight.diveTicks(20));
        assertEquals(KingFlight.DIVE_MAX, KingFlight.diveTicks(500));
    }

    @Test
    void aDiveStartsAtTheStartEndsExactlyOnTheMarkAndSpeedsUp() {
        double[] from = {0, 6, 0}, to = {12, 0.5, -9};
        int n = KingFlight.diveTicks(Math.sqrt(144 + 30.25 + 81));
        SdsAsserts.assertNear(from, KingFlight.divePoint(from, to, 0, n), 1e-12);
        SdsAsserts.assertNear(to, KingFlight.divePoint(from, to, n, n), 1e-12);
        SdsAsserts.assertNear(to, KingFlight.divePoint(from, to, n + 5, n), 1e-12);
        double prevStep = 0;
        double[] prev = from;
        for (int k = 1; k <= n; k++) {
            double[] p = KingFlight.divePoint(from, to, k, n);
            double step = Math.sqrt(Math.pow(p[0] - prev[0], 2) + Math.pow(p[1] - prev[1], 2) + Math.pow(p[2] - prev[2], 2));
            assertTrue(step >= prevStep - 1e-9, "eased in: each tick covers at least as much as the last (tick " + k + ")");
            prevStep = step;
            prev = p;
        }
        double[] half = KingFlight.divePoint(new double[]{0, 0, 0}, new double[]{8, 0, 0}, 5, 10);
        assertEquals(2.0, half[0], 1e-12, "halfway in time is a quarter of the way");
    }
}
