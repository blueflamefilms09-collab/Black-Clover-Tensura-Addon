package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The timing, the size and the integrity of Spatial Mana Domination: the seal bites late, ends on time, grows with power, and can be broken. */
class DomainRulesTest {
    @Test
    void theSealBitesOnlyAfterTheCubeHasClosed() {
        DomainClock c = DomainClock.of(180);
        assertEquals(180, c.total());
        assertEquals(DomainClock.FORM_TICKS, c.form());
        for (int age = 0; age < DomainClock.FORM_TICKS; age++) {
            assertTrue(c.forming(age), "age " + age);
            assertFalse(c.sealing(age), "age " + age);
        }
        assertTrue(c.sealing(DomainClock.FORM_TICKS));
        assertTrue(c.sealing(179));
        assertFalse(c.sealing(180));
        assertTrue(c.over(180));
        assertFalse(c.over(179));
        assertFalse(c.forming(-1));
        assertFalse(c.sealing(-1));
    }

    @Test
    void sealLeftCountsDownToZeroAndNeverGrows() {
        DomainClock c = DomainClock.of(140);
        assertEquals(140 - DomainClock.FORM_TICKS, c.sealLeft(0), "before the seal starts the whole seal is still to come");
        assertEquals(140 - DomainClock.FORM_TICKS, c.sealLeft(DomainClock.FORM_TICKS));
        int prev = Integer.MAX_VALUE;
        for (int age = 0; age <= 160; age++) {
            int left = c.sealLeft(age);
            assertTrue(left <= prev, "never grows at age " + age);
            assertTrue(left >= 0);
            prev = left;
        }
        assertEquals(0, c.sealLeft(140));
        assertEquals(1, c.sealLeft(139));
        assertEquals(DomainClock.FORM_TICKS, c.untilSealing(0));
        assertEquals(5, c.untilSealing(DomainClock.FORM_TICKS - 5));
        assertEquals(0, c.untilSealing(DomainClock.FORM_TICKS));
        assertEquals(0, c.untilSealing(100));
    }

    @Test
    void theClockIsClampedToSomethingSane() {
        assertEquals(DomainClock.FORM_TICKS + DomainClock.MIN_SEALED, DomainClock.of(1).total(), "always some seal to survive");
        assertEquals(DomainClock.FORM_TICKS + DomainClock.MIN_SEALED, DomainClock.of(-50).total());
        assertEquals(DomainClock.MAX_TOTAL, DomainClock.of(100000).total());
        assertTrue(new DomainClock(100, 999).form() <= DomainClock.MAX_TOTAL - DomainClock.MIN_SEALED);
        assertEquals(0, new DomainClock(100, -4).form());
        assertEquals(0.0, DomainClock.of(100).progress(0), 1e-9);
        assertEquals(0.5, DomainClock.of(100).progress(50), 1e-9);
        assertEquals(1.0, DomainClock.of(100).progress(500), 1e-9);
    }

    @Test
    void sizeAndDurationGrowWithPower() {
        double lastHalf = 0;
        int lastTicks = 0;
        for (int p = 60; p <= 100; p++) {
            double h = DomainMath.half(p);
            int t = DomainMath.ticks(p);
            assertTrue(h >= lastHalf && t >= lastTicks, "monotone at " + p);
            assertTrue(h >= 8 && h <= 11 && t >= 140 && t <= 260);
            lastHalf = h;
            lastTicks = t;
        }
        assertEquals(8, DomainMath.half(60), 1e-9);
        assertEquals(9.5, DomainMath.half(80), 1e-9);
        assertEquals(11, DomainMath.half(100), 1e-9);
        assertEquals(140, DomainMath.ticks(60));
        assertEquals(180, DomainMath.ticks(80));
        assertEquals(260, DomainMath.ticks(100));
        assertEquals(8, DomainMath.half(20), 1e-9, "under the minimum power it uses the smallest cube");
        assertEquals(11, DomainMath.half(500), 1e-9);
    }

    @Test
    void theGatesMakeItLargerLongerAndChained() {
        assertTrue(DomainMath.half(100) > DomainMath.half(80));
        assertTrue(DomainMath.ticks(100) > DomainMath.ticks(80) + 40);
        assertEquals(0, DomainMath.chainWaves(60));
        assertEquals(0, DomainMath.chainWaves(80));
        assertEquals(2, DomainMath.chainWaves(100));
        assertTrue(DomainMath.RULER_DAMAGE > 1.0 && DomainMath.RULER_DAMAGE <= 1.25);
    }

    @Test
    void theCubeCanAlwaysBeWalkedOutOfInTheTelegraph() {
        for (int p = 60; p <= 100; p++) {
            int wind = Math.round(ZenonTable.byId(ZenonTable.DOMINATION).windUpTicks() * (float) PowerCurve.tuning(p).windUpMultiplier());
            assertTrue(wind >= 8, "a real telegraph at " + p);
            assertTrue(DomainMath.leavableOnFoot(DomainMath.half(p), wind), "walkable at " + p + ": " + DomainMath.ticksToLeave(DomainMath.half(p)) + " vs " + wind);
        }
        assertFalse(DomainMath.leavableOnFoot(24, 45), "a huge cube with a short telegraph would not be fair");
    }

    @Test
    void theDomainTableRowAgreesWithTheDomainNumbers() {
        ZenonTable.Spec d = ZenonTable.byId(ZenonTable.DOMINATION);
        assertEquals(DomainMath.MIN_POWER, d.minPower(), "the move opens exactly where the domain's numbers start");
        assertTrue(d.ultimate());
        assertTrue(d.recovery() >= 40, "exposed while the cube closes");
        assertTrue(d.windUpTicks() >= 45, "a telegraph of a few seconds, in which a walker leaves");
    }

    @Test
    void theSealIsLongEnoughToMatterAndShortEnoughToSurvive() {
        for (int p = 60; p <= 100; p += 10) {
            DomainClock c = DomainMath.clock(p);
            int sealSeconds = c.sealLeft(DomainClock.FORM_TICKS) / 20;
            assertTrue(sealSeconds >= 6 && sealSeconds <= 13, "power " + p + ": " + sealSeconds + " s");
        }
    }

    @Test
    void integrityBreaksAfterTheBossLosesAShareOfItsHealth() {
        double max = DomainIntegrity.maxFor(950, 60);
        assertEquals(76, max, 1e-6, "8% of 950");
        DomainIntegrity d = new DomainIntegrity(max);
        assertFalse(d.broken());
        assertEquals(1.0, d.fraction(), 1e-9);
        assertFalse(d.hit(50, false));
        assertEquals(1 - 50 / 76.0, d.fraction(), 1e-9);
        assertTrue(d.hit(26, false), "the blow that reaches the total breaks it");
        assertTrue(d.broken());
        assertFalse(d.hit(500, false), "it only breaks once");
        assertEquals(0.0, d.fraction(), 1e-9);
        assertEquals(76, d.taken(), 1e-9);
    }

    @Test
    void antiMagicCutsTheCubeTwiceAsDeep() {
        DomainIntegrity a = new DomainIntegrity(100), b = new DomainIntegrity(100);
        a.hit(40, true);
        b.hit(80, false);
        assertEquals(b.taken(), a.taken(), 1e-9);
        assertTrue(a.hit(10, true), "40*2 + 10*2 = 100");
        assertFalse(b.broken());
        assertEquals(2.0, DomainIntegrity.ANTI_MAGIC_FACTOR, 1e-9);
    }

    @Test
    void zeroAndNegativeBlowsDoNothing() {
        DomainIntegrity d = new DomainIntegrity(10);
        assertFalse(d.hit(0, false));
        assertFalse(d.hit(-5, true));
        assertFalse(d.hit(Double.NaN, false));
        assertEquals(1.0, d.fraction(), 1e-9);
        assertEquals(1, new DomainIntegrity(-3).max(), 1e-9, "never a zero-integrity cube");
    }

    @Test
    void integrityIsTougherAtHigherPowerButNeverAbsurd() {
        double last = 0;
        for (int p = 60; p <= 100; p++) {
            double m = DomainIntegrity.maxFor(1000, p);
            assertTrue(m >= last, "monotone");
            assertTrue(m >= 80 && m <= 140, "between 8% and 14% of the boss's health at " + p);
            last = m;
        }
        assertEquals(DomainIntegrity.maxFor(1000, 60), DomainIntegrity.maxFor(1000, 20), 1e-9);
        assertEquals(DomainIntegrity.maxFor(1000, 100), DomainIntegrity.maxFor(1000, 900), 1e-9);
        assertTrue(DomainIntegrity.maxFor(0, 80) >= 1);
    }
}
