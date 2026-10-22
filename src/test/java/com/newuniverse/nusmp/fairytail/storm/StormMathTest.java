package com.newuniverse.nusmp.fairytail.storm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The functions of (seed, age) the server hurts things with and the clients draw with: they must be deterministic, bounded and agree with each other. */
class StormMathTest {
    private static final double E = 1e-9;

    /** Element-wise equality within E (the plain assertion of the test shim has no double[] form). */
    private static void near(double[] e, double[] a, String msg) {
        assertEquals(e.length, a.length, msg);
        for (int i = 0; i < e.length; i++) assertTrue(Math.abs(e[i] - a[i]) <= E, (msg == null ? "" : msg + ": ") + "element " + i + " expected " + e[i] + " but was " + a[i]);
    }

    @Test
    void theHashIsDeterministicAndInRange() {
        for (int i = 0; i < 200; i++) {
            float a = StormMath.hash(123456789L, i, 7), b = StormMath.hash(123456789L, i, 7);
            assertEquals(a, b);
            assertTrue(a >= 0f && a < 1f, "hash " + a);
        }
        assertNotEquals(StormMath.hash(1L, 0, 0), StormMath.hash(2L, 0, 0));
    }

    @Test
    void theSeedCarriesTheWeatherPartsAndTheSpell() {
        for (StormSpell s : StormSpell.values()) {
            long seed = StormMath.packSeed(0x123456789ABCL, s.mask, s.ordinal());
            assertEquals(s.mask, StormMath.maskOf(seed), s.id);
            assertEquals(s.ordinal(), StormMath.spellOf(seed), s.id);
            assertEquals(0x123456789ABCL, seed & 0xFFFFFFFFFFFFL, "the random bits are kept");
        }
        // a big random value never leaks into the mask bits
        long seed = StormMath.packSeed(-1L, 0, 0);
        assertEquals(0, StormMath.maskOf(seed));
        assertEquals(0, StormMath.spellOf(seed));
    }

    @Test
    void theCellDriftsInAStraightLineFromStartToEnd() {
        double[] a = {10, 64, -4}, b = {16, 64, 2};
        near(a, StormMath.center(a, b, 0, 100), null);
        near(b, StormMath.center(a, b, 100, 100), null);
        near(b, StormMath.center(a, b, 500, 100), "past the end it stays at the end");
        double[] mid = StormMath.center(a, b, 50, 100);
        near(new double[]{13, 64, -1}, mid, null);
    }

    @Test
    void theDriftEndIsTheDriftSpeedTimesTheDuration() {
        double[] end = StormMath.driftEnd(new double[]{0, 70, 0}, 3, 4, 1.6, 160);
        assertEquals(0.6 * 1.6 * 8, end[0], 1e-9);
        assertEquals(0.8 * 1.6 * 8, end[2], 1e-9);
        assertEquals(70, end[1], E);
        double[] still = StormMath.driftEnd(new double[]{1, 2, 3}, 0, 0, 1.6, 160);
        near(new double[]{1, 2, 3}, still, "no direction, no drift");
    }

    @Test
    void theEnvelopeBuildsHoldsAndFades() {
        int dur = 160;
        assertEquals(0.0, StormMath.envelope(0, dur), E);
        assertEquals(0.0, StormMath.envelope(dur, dur), E);
        assertEquals(1.0, StormMath.envelope(80, dur), E);
        double prev = -1;
        for (int a = 0; a <= StormMath.FADE_IN; a++) {
            double e = StormMath.envelope(a, dur);
            assertTrue(e >= prev - 1e-12, "builds up monotonically");
            prev = e;
        }
        for (int a = 0; a < dur; a++) {
            double e = StormMath.envelope(a, dur);
            assertTrue(e >= 0 && e <= 1.0 + 1e-12);
        }
        assertEquals(StormMath.envelope(3.5, dur), StormMath.envelope(3.5, dur), E, "fractional ages (a client between ticks) work");
        double r = StormMath.radiusAt(7.0, 80, dur);
        assertEquals(7.0, r, E);
        assertTrue(StormMath.radiusAt(7.0, 2, dur) < 7.0 * 0.6, "a young cell is small");
    }

    @Test
    void everyHailVolleyLandsExactlyOnceAndItsStonesFallInsideTheCell() {
        int dur = 120;
        int landed = 0;
        for (int age = 0; age < dur; age++) {
            int v = StormMath.hailVolleyLanding(age);
            if (v < 0) continue;
            landed++;
            assertEquals(age, v * StormMath.HAIL_PERIOD + StormMath.HAIL_FALL, "the volley lands HAIL_FALL ticks after it starts");
            double r = StormMath.radiusAt(6.5, v * StormMath.HAIL_PERIOD, dur);
            for (int i = 0; i < StormMath.hailPerVolley(6.5); i++) {
                double[] off = StormMath.hailStone(777L, v, i, r);
                assertTrue(Math.hypot(off[0], off[1]) <= r * 0.92 + 1e-9, "inside the disc, never at the rim");
                near(off, StormMath.hailStone(777L, v, i, r), "the same stone for the server and every client");
            }
        }
        assertEquals((dur - 1 - StormMath.HAIL_FALL) / StormMath.HAIL_PERIOD + 1, landed);
        assertEquals(-1, StormMath.hailVolleyLanding(StormMath.HAIL_FALL - 1), "nothing lands before the first stone has fallen");
        assertTrue(StormMath.hailPerVolley(100) <= StormMath.HAIL_MAX_PER_VOLLEY);
        assertTrue(StormMath.hailPerVolley(0.1) >= 1);
    }

    @Test
    void boltsComeOnTheBeatAfterTheLeadAndStopAtTheCap() {
        int dur = 140, every = 14, max = 7, count = 0;
        for (int age = 0; age < dur; age++) {
            boolean due = StormMath.boltDue(age, dur, every, count, max);
            if (due) {
                assertTrue(age >= StormMath.BOLT_LEAD);
                assertEquals(0, (age - StormMath.BOLT_LEAD) % every);
                assertTrue(age <= dur - 10, "no bolt while the cell dies");
                count++;
            }
        }
        assertTrue(count > 0 && count <= max, "bolts: " + count);
        assertFalse(StormMath.boltDue(StormMath.BOLT_LEAD, dur, every, max, max), "the cap holds");
        assertFalse(StormMath.boltDue(StormMath.BOLT_LEAD, dur, 0, 0, max), "a spell with no beat throws no bolt");
        assertFalse(StormMath.boltDue(StormMath.BOLT_LEAD, dur, every, 0, 0));
    }

    @Test
    void aBoltPathStartsInTheCloudEndsOnTheMarkAndIsTheSameForTheSameFrame() {
        double[] from = {0, 20, 0}, to = {3, 11, -2};
        double[][] p = StormMath.boltPath(99L, 2, from, to, 9, 0.8);
        assertEquals(10, p.length);
        near(from, p[0], null);
        near(to, p[9], null);
        double[][] q = StormMath.boltPath(99L, 2, from, to, 9, 0.8);
        for (int i = 0; i < p.length; i++) near(p[i], q[i], null);
        double[][] other = StormMath.boltPath(99L, 3, from, to, 9, 0.8);
        boolean differs = false;
        for (int i = 1; i < 9; i++) if (Math.abs(p[i][0] - other[i][0]) > 1e-9) differs = true;
        assertTrue(differs, "a new frame re-rolls the path (the flicker)");
        // the sideways jitter is bounded by the jitter number (the vertical one by a third of it)
        for (int i = 0; i < p.length; i++) {
            double t = i / 9.0;
            double lx = from[0] + (to[0] - from[0]) * t, lz = from[2] + (to[2] - from[2]) * t;
            assertTrue(Math.hypot(p[i][0] - lx, p[i][2] - lz) <= 0.8 + 1e-9);
        }
    }

    @Test
    void aStraightDownBoltDoesNotDivideByZero() {
        double[][] p = StormMath.boltPath(5L, 0, new double[]{1, 10, 1}, new double[]{1, 0, 1}, 6, 0.5);
        for (double[] pt : p) for (double c : pt) assertTrue(Double.isFinite(c));
    }

    @Test
    void aFrontRunsFastThenSlowsAndEndsAtItsLength() {
        assertEquals(0.0, StormMath.frontPos(0, 40, 20.0), E);
        assertEquals(20.0, StormMath.frontPos(40, 40, 20.0), E);
        double prev = -1;
        for (int a = 0; a <= 40; a++) {
            double f = StormMath.frontPos(a, 40, 20.0);
            assertTrue(f >= prev);
            prev = f;
        }
        assertTrue(StormMath.frontPos(10, 40, 20.0) > 20.0 * 0.25, "fast at the start");
        assertTrue(StormMath.inFront(9.0, 1.0, 10.0, 3.0, 5.5));
        assertFalse(StormMath.inFront(9.0, 6.0, 10.0, 3.0, 5.5), "outside the width");
        assertFalse(StormMath.inFront(5.0, 0.0, 10.0, 3.0, 5.5), "far behind the head");
        assertFalse(StormMath.inFront(14.0, 0.0, 10.0, 3.0, 5.5), "ahead of the head");
    }

    @Test
    void aGustRingWidensAndGoesOut() {
        assertEquals(-1.0, StormMath.gustRingRadius(10, 1, 12.0), E, "ring 1 has not left yet");
        double r0 = StormMath.gustRingRadius(0, 0, 12.0), r1 = StormMath.gustRingRadius(30, 0, 12.0);
        assertTrue(r0 > 0 && r1 > r0 && r1 <= 12.0 + E);
        assertEquals(-1.0, StormMath.gustRingRadius(StormMath.GUST_PERIOD, 0, 12.0), E, "ring 0 is gone after its life");
    }

    @Test
    void aPushIsLimitedSidewaysAndUp() {
        double[] v = StormMath.limitPush(3, 2, 4);
        assertEquals(StormMath.MAX_PUSH, Math.hypot(v[0], v[2]), 1e-9);
        assertEquals(StormMath.MAX_LIFT, v[1], E);
        double[] small = StormMath.limitPush(0.1, -9, 0.1);
        assertEquals(0.1, small[0], E);
        assertEquals(-StormMath.MAX_LIFT, small[1], E);
    }

    @Test
    void theAllocationFreeFormsAgreeWithTheArrayForms() {
        double[] a = {10, 64, -4}, b = {16, 66, 2};
        for (int age = 0; age <= 100; age += 7) {
            double[] c = StormMath.center(a, b, age, 100);
            assertEquals(c[0], StormMath.lerpAt(a[0], b[0], age, 100), E);
            assertEquals(c[1], StormMath.lerpAt(a[1], b[1], age, 100), E);
            assertEquals(c[2], StormMath.lerpAt(a[2], b[2], age, 100), E);
        }
        for (int v = 0; v < 6; v++) {
            for (int i = 0; i < 4; i++) {
                double[] st = StormMath.hailStone(31L, v, i, 6.0);
                assertEquals(st[0], StormMath.hailStoneX(31L, v, i, 6.0), E);
                assertEquals(st[1], StormMath.hailStoneZ(31L, v, i, 6.0), E);
            }
        }
        double[][] buf = new double[10][3];
        double[] from = {0, 20, 0}, to = {3, 11, -2};
        StormMath.boltPath(7L, 1, from[0], from[1], from[2], to[0], to[1], to[2], 9, 0.8, buf);
        double[][] ref = StormMath.boltPath(7L, 1, from, to, 9, 0.8);
        for (int i = 0; i < 10; i++) for (int k = 0; k < 3; k++) assertEquals(ref[i][k], buf[i][k], E);
    }

    @Test
    void theCostIsAShareOfTheAuraWithAFloorAndTheLawsMultiplier() {
        assertEquals(StormMath.MIN_COST, StormMath.cost(10, 5, 1.0), E, "the floor");
        assertEquals(500.0, StormMath.cost(10000, 5, 1.0), E);
        assertEquals(250.0, StormMath.cost(10000, 5, 0.5), E, "a cheaper law (a Mana Zone) halves it");
        assertEquals(0.0, StormMath.cost(10000, 5, -3), E, "a negative multiplier never pays out");
    }
}
