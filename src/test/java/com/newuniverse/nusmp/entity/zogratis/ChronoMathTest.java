package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Lucius's Time Magic numbers: stops are short and followed by a grace, the combo is marked before it lands, light steps have a line. */
class ChronoMathTest {
    @Test
    void noStopIsEverLongerThanThreeSeconds() {
        for (int p = 0; p <= 120; p += 5) {
            assertTrue(ChronoMath.haltTicks(p) <= 40, "single stasis at " + p);
            assertTrue(ChronoMath.stopTicks(p) <= ChronoMath.HARD_STOP_CAP, "time stop at " + p);
            assertTrue(ChronoMath.stopTicks(p) >= ChronoMath.haltTicks(p));
        }
        assertEquals(60, ChronoMath.HARD_STOP_CAP, "three seconds");
        assertEquals(60, ChronoMath.stopTicks(100));
        assertEquals(50, ChronoMath.stopTicks(80));
        assertTrue(ChronoMath.GRACE >= 100, "a long grace after a stop");
    }

    @Test
    void everyBlowOfTheComboIsMarkedBeforeItLands() {
        for (int k = 0; k < 6; k++) {
            assertEquals(ChronoMath.MARK_LEAD, ChronoMath.blowDelay(k) - ChronoMath.markDelay(k), "blow " + k);
            assertTrue(ChronoMath.markDelay(k) >= 0);
            if (k > 0) assertEquals(14, ChronoMath.blowDelay(k) - ChronoMath.blowDelay(k - 1), "a beat between blows");
        }
        assertTrue(ChronoMath.blowDelay(0) >= 16, "time to leave after the release");
        assertTrue(ChronoMath.comboBlows(100) > ChronoMath.comboBlows(80));
        assertTrue(ChronoMath.comboBlows(60) >= 2);
    }

    @Test
    void theStopComboFitsInsideTheGraceSoItCannotBeChained() {
        for (int p : new int[]{80, 100}) {
            int lastBlow = ChronoMath.blowDelay(ChronoMath.comboBlows(p) - 1);
            assertTrue(lastBlow < ChronoMath.GRACE, "the last blow lands inside the six seconds in which nobody can be stopped again");
        }
    }

    @Test
    void rewindsGoBackFurtherAndHealABitMoreWithPower() {
        assertEquals(60, ChronoMath.rewindTicks(20));
        assertEquals(80, ChronoMath.rewindTicks(60));
        assertEquals(100, ChronoMath.rewindTicks(100));
        assertEquals(100, ChronoMath.rewindTicks(500), "clamped");
        assertEquals(60, ChronoMath.rewindTicks(0));
        assertTrue(ChronoMath.rewindHealCap(100) <= 0.10 && ChronoMath.rewindHealCap(60) >= 0.05);
        assertTrue(ChronoMath.rewindHealCap(100) > ChronoMath.rewindHealCap(60));
        assertTrue(ChronoMath.rewindTicks(100) <= (ChronoMath.ringCapacity() - 2) * ChronoMath.SNAP_EVERY, "the ring remembers far enough");
    }

    @Test
    void accelerationIsABoundedBurst() {
        assertEquals(160, ChronoMath.ACCEL_TICKS);
        assertTrue(ChronoMath.ACCEL_SPEED > 0 && ChronoMath.ACCEL_SPEED <= 0.5);
        assertTrue(ChronoMath.ACCEL_MELEE > 1f && ChronoMath.ACCEL_MELEE <= 1.3f);
    }

    @Test
    void aStepEndsPastTheTargetAndNeverFartherThanTheMaximum() {
        double[] e = ChronoMath.stepEnd(0, 0, 10, 0, 2.2);
        assertEquals(12.2, e[0], 1e-9);
        assertEquals(0, e[1], 1e-9);
        double[] diag = ChronoMath.stepEnd(0, 0, 3, 4, 5);
        assertEquals(10, Math.hypot(diag[0], diag[1]), 1e-9, "5 to the target + 5 past");
        double[] far = ChronoMath.stepEnd(0, 0, 100, 0, 2.2);
        assertEquals(ChronoMath.STEP_MAX, far[0], 1e-9);
        double[] none = ChronoMath.stepEnd(4, 4, 4, 4, 2);
        assertEquals(4, none[0]);
        assertEquals(4, none[1], "a target on top of him: he stays");
    }

    @Test
    void theDamageLineIsAStrip() {
        // a step along x from 0 to 20
        assertTrue(ChronoMath.inLine(10, 1.0, 0, 0, 20, 0, 0));
        assertTrue(ChronoMath.inLine(10, 1.5, 0, 0, 20, 0, 0), "on the edge");
        assertFalse(ChronoMath.inLine(10, 1.6, 0, 0, 20, 0, 0), "just outside");
        assertTrue(ChronoMath.inLine(10, 1.8, 0, 0, 20, 0, 0.4), "a wide victim is hit sooner");
        assertFalse(ChronoMath.inLine(-3, 0, 0, 0, 20, 0, 0), "behind the start");
        assertFalse(ChronoMath.inLine(24, 0, 0, 0, 20, 0, 0), "past the end");
        assertTrue(ChronoMath.inLine(21, 0, 0, 0, 20, 0, 0), "the rounded end");
        assertEquals(5, ChronoMath.distToSegment(5, 5, 0, 0, 10, 0), 1e-9);
        assertEquals(5, ChronoMath.distToSegment(0, 5, 0, 0, 0, 0), 1e-9, "a zero-length segment is a point");
        assertEquals(Math.hypot(3, 4), ChronoMath.distToSegment(-3, 4, 0, 0, 10, 0), 1e-9, "beyond the start measures to the start");
    }

    @Test
    void comboStepsGrowWithPower() {
        assertEquals(2, ChronoMath.comboSteps(60));
        assertEquals(3, ChronoMath.comboSteps(80));
        assertEquals(4, ChronoMath.comboSteps(100));
        assertTrue(ChronoMath.STEP_LEAD >= 12, "the line is shown for most of a second");
    }

    @Test
    void frozenAlliesAreFreedByEnoughDamage() {
        assertFalse(ChronoMath.breaks(7.9f));
        assertTrue(ChronoMath.breaks(8f));
        assertTrue(ChronoMath.MAX_HELD >= 4);
    }
}
