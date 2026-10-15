package com.newuniverse.nusmp.labyrinth.dnd.vital;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Vital Strike arithmetic: threshold, meter, execution, cooldown, bosses, overflow. */
class VitalMathTest {
    private static final double TH = 0.18, EX = 0.35;

    private static VitalMath.Outcome hit(float amount, float max, boolean eligible, boolean relic, double meter, boolean cd) {
        return VitalMath.resolve(amount, max, eligible, relic, meter, cd, TH, EX);
    }

    @Test
    void heavyHitNeedsTheThresholdShareOfMaxHealth() {
        assertTrue(VitalMath.heavyHit(18f, 100f, TH));
        assertFalse(VitalMath.heavyHit(17.9f, 100f, TH));
        assertTrue(VitalMath.heavyHit(500f, 100f, TH));
    }

    @Test
    void heavyHitBecomesAnExecutionAtLeastTheExecuteShare() {
        VitalMath.Outcome o = hit(20f, 100f, true, false, 0.0, false);
        assertTrue(o.vital());
        assertTrue(o.heavy());
        assertEquals(35f, o.damage(), 0.001f);
    }

    @Test
    void aHitStrongerThanTheExecutionKeepsItsOwnDamage() {
        VitalMath.Outcome o = hit(80f, 100f, true, false, 0.0, false);
        assertTrue(o.vital());
        assertEquals(80f, o.damage(), 0.001f);
    }

    @Test
    void aLightHitWithoutRelicIsUntouched() {
        VitalMath.Outcome o = hit(5f, 100f, true, false, 0.0, false);
        assertFalse(o.vital());
        assertEquals(5f, o.damage(), 0.0f);
        assertEquals(0.0, o.meter(), 0.0);
    }

    @Test
    void bossesAndUnbufferedMobsAreNeverStruck() {
        assertFalse(VitalMath.eligible(true, true));
        assertFalse(VitalMath.eligible(false, false));
        assertTrue(VitalMath.eligible(false, true));
        VitalMath.Outcome o = hit(1000f, 100f, false, true, 1.0, false);
        assertFalse(o.vital());
        assertEquals(1000f, o.damage(), 0.0f);
    }

    @Test
    void relicHitsChargeTheMeterAndTheFourthFillsIt() {
        double m = 0.0;
        for (int i = 1; i <= 3; i++) {
            VitalMath.Outcome o = hit(1f, 100f, true, true, m, false);
            assertFalse(o.vital(), "hit " + i);
            m = o.meter();
            assertEquals(0.25 * i, m, 1e-9);
        }
        VitalMath.Outcome fourth = hit(1f, 100f, true, true, m, false);
        assertTrue(fourth.vital());
        assertTrue(fourth.relicCharged());
        assertFalse(fourth.heavy());
        assertEquals(35f, fourth.damage(), 0.001f);
        assertEquals(0.0, fourth.meter(), 0.0);
    }

    @Test
    void theMeterDecaysFivePercentPerSecondAndNeverBelowZero() {
        assertEquals(0.75, VitalMath.meterAt(0.80, 100L, 120L), 1e-9);
        assertEquals(0.0, VitalMath.meterAt(0.10, 0L, 100000L), 0.0);
        assertEquals(0.5, VitalMath.meterAt(0.5, 200L, 100L), 0.0, "a clock that went backwards loses nothing");
        assertEquals(1.0, VitalMath.meterAt(7.0, 0L, 0L), 0.0, "an out-of-range stored value is clamped");
        assertEquals(0.0, VitalMath.meterAt(Double.NaN, 0L, 0L), 0.0);
    }

    @Test
    void slowHitsNeverFillTheMeter() {
        double m = 0.0;
        for (int i = 0; i < 50; i++) {
            m = VitalMath.meterAt(m, 0L, 100L);              // 5 s between hits: 25 % lost = 25 % gained
            m = VitalMath.gain(m);
        }
        assertFalse(VitalMath.full(m));
    }

    @Test
    void theCooldownBlocksBothPathsAndDoesNotChargeTheMeter() {
        VitalMath.Outcome heavy = hit(90f, 100f, true, false, 0.0, true);
        assertFalse(heavy.vital());
        assertEquals(90f, heavy.damage(), 0.0f);
        VitalMath.Outcome relic = hit(1f, 100f, true, true, 0.75, true);
        assertFalse(relic.vital());
        assertEquals(0.75, relic.meter(), 1e-9);
    }

    @Test
    void cooldownLeftCountsDownAndForgivesABackwardsClock() {
        assertEquals(100L, VitalMath.cooldownLeft(1000L, 1000L, 100));
        assertEquals(40L, VitalMath.cooldownLeft(1000L, 1060L, 100));
        assertEquals(0L, VitalMath.cooldownLeft(1000L, 1100L, 100));
        assertEquals(0L, VitalMath.cooldownLeft(1000L, 500L, 100));
        assertEquals(0L, VitalMath.cooldownLeft(Long.MIN_VALUE, 5L, 100));
        assertEquals(0L, VitalMath.cooldownLeft(10L, 11L, 0));
    }

    @Test
    void hostileNumbersNeverOverflowOrDivide() {
        assertFalse(VitalMath.heavyHit(Float.NaN, 100f, TH));
        assertFalse(VitalMath.heavyHit(Float.POSITIVE_INFINITY, 100f, TH));
        assertFalse(VitalMath.heavyHit(10f, 0f, TH));
        assertFalse(VitalMath.heavyHit(10f, -5f, TH));
        assertFalse(VitalMath.heavyHit(10f, Float.POSITIVE_INFINITY, TH));
        assertEquals(10f, VitalMath.executeDamage(10f, 0f, EX), 0f);
        assertEquals(0f, VitalMath.executeDamage(Float.NaN, 100f, 0.0), 0f);
        float huge = VitalMath.executeDamage(Float.MAX_VALUE, Float.MAX_VALUE, 1.0);
        assertFalse(Float.isInfinite(huge) || Float.isNaN(huge));
        assertEquals(100f, VitalMath.executeDamage(1f, 100f, 5.0), 0.001f, "a share above 1 is clamped to the whole health");
    }

    @Test
    void gradeGate() {
        assertTrue(VitalMath.gradeQualifies(2, 2));
        assertTrue(VitalMath.gradeQualifies(6, 2));
        assertFalse(VitalMath.gradeQualifies(1, 2));
    }

    @Test
    void aHeavyRelicHitIsOneStrikeAndResetsTheMeterOnlyWhenTheMeterTriggered() {
        VitalMath.Outcome o = hit(30f, 100f, true, true, 0.5, false);
        assertTrue(o.vital());
        assertTrue(o.heavy());
        assertFalse(o.relicCharged());
        assertEquals(0.75, o.meter(), 1e-9, "the heavy hit was its own trigger: the meter keeps what it earned");
    }
}
