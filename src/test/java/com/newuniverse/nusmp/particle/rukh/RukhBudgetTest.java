package com.newuniverse.nusmp.particle.rukh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The per-tick particle budget and the pure geometry/caps of the Rukh emitters. */
class RukhBudgetTest {
    @Test
    void grantsUpToTheCapPerTickAndDropsTheRest() {
        RukhBudget b = new RukhBudget(96);
        assertEquals(60, b.take(10, 60));
        assertEquals(36, b.take(10, 60));
        assertEquals(0, b.take(10, 1));
        assertEquals(0, b.remaining(10));
    }

    @Test
    void aNewTickResetsTheCounter() {
        RukhBudget b = new RukhBudget(10);
        assertEquals(10, b.take(1, 50));
        assertEquals(10, b.take(2, 50));
        assertEquals(10, b.remaining(3));
    }

    @Test
    void badRequestsGrantNothingAndAClockGoingBackStartsFresh() {
        RukhBudget b = new RukhBudget(5);
        assertEquals(0, b.take(1, 0));
        assertEquals(0, b.take(1, -4));
        assertEquals(5, b.take(100, 9));
        assertEquals(5, b.take(50, 9));
    }

    @Test
    void theCapCanBeChangedAndNeverGoesNegative() {
        RukhBudget b = new RukhBudget(-3);
        assertEquals(0, b.cap());
        b.setCap(4);
        assertEquals(4, b.take(1, 9));
        assertEquals(RukhBudget.DEFAULT_CAP, 96);
    }

    @Test
    void callCapsClamp() {
        assertEquals(12, RukhMath.clamp(500, RukhMath.MAX_BURST));
        assertEquals(6, RukhMath.clamp(7, RukhMath.MAX_STREAM));
        assertEquals(0, RukhMath.clamp(-1, RukhMath.MAX_GATHER));
        assertEquals(2, RukhMath.clamp(9, RukhMath.MAX_AURA));
    }

    @Test
    void towardCoversTheDistanceButNeverExceedsTheSpeedLimit() {
        double[] v = RukhMath.toward(18, 0, 0, 18);
        assertEquals(1.0 > RukhMath.MAX_SPEED ? RukhMath.MAX_SPEED : 1.0, v[0]);
        double[] far = RukhMath.toward(1000, 1000, 0, 18);
        assertTrue(Math.sqrt(far[0] * far[0] + far[1] * far[1]) <= RukhMath.MAX_SPEED + 1e-9);
        double[] near = RukhMath.toward(0, 3.6, 0, 18);
        assertEquals(0.2, near[1], 1e-9);
    }

    @Test
    void theGatheringRingStaysBetween2p5And4Blocks() {
        for (int i = 0; i < 12; i++) {
            for (double u = 0; u < 1; u += 0.25) {
                double[] p = RukhMath.ring(u, RukhMath.slotAngle(i, 12, u));
                double r = Math.sqrt(p[0] * p[0] + p[1] * p[1]);
                assertTrue(r >= RukhMath.RING_MIN - 1e-9 && r <= RukhMath.RING_MAX + 1e-9);
            }
        }
        double[] over = RukhMath.ring(5, 0);
        assertTrue(over[0] <= RukhMath.RING_MAX);
    }

    @Test
    void scaleAndAlphaCurvesAreSmoothAndBounded() {
        assertEquals(0f, RukhMath.scaleCurve(0f));
        assertEquals(0f, RukhMath.scaleCurve(1f));
        assertEquals(1f, RukhMath.scaleCurve(0.5f));
        assertEquals(1f, RukhMath.alphaCurve(0.3f));
        assertEquals(0f, RukhMath.alphaCurve(1f));
        float prev = 1f;
        for (float f = 0.55f; f <= 1f; f += 0.05f) {
            float a = RukhMath.alphaCurve(f);
            assertTrue(a <= prev + 1e-6f && a >= 0f);
            prev = a;
        }
        assertEquals(0.6, RukhMath.clampSpeed(9), 1e-9);
        assertEquals(-0.6, RukhMath.clampSpeed(-9), 1e-9);
    }
}
