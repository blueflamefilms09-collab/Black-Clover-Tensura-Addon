package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The boss relics' pure rules: the gravity well, the twin gates, the Mimic's bite and the drop chance. */
class WaveBossRulesTest {
    @Test
    void theWellDragsMostInTheMiddleOfItsRangeAndNeverAtTheEdgesOrTheCore() {
        double r = WaveBossRules.WELL_RADIUS;
        assertEquals(0.0, WaveBossRules.pull(0.5, r), 1e-9, "inside the core: nothing is squashed into the caster");
        assertEquals(0.0, WaveBossRules.pull(r, r), 1e-9, "at the rim");
        assertEquals(0.0, WaveBossRules.pull(r + 3, r), 1e-9, "outside");
        double best = 0;
        for (double d = 0; d <= r; d += 0.25) {
            double p = WaveBossRules.pull(d, r);
            assertTrue(p >= 0 && p <= WaveBossRules.PULL_MAX + 1e-9, "pull stays within its cap at " + d);
            best = Math.max(best, p);
        }
        assertTrue(best > 0.08, "somewhere it really pulls");
        assertTrue(WaveBossRules.pull(4.0, r) > WaveBossRules.pull(7.5, r), "stronger inside than near the rim");
    }

    @Test
    void theWellSlowsHarderTowardsItsHeart() {
        double r = WaveBossRules.WELL_RADIUS;
        assertEquals(2, WaveBossRules.slowAmp(1.0, r));
        assertEquals(1, WaveBossRules.slowAmp(r * 0.5, r));
        assertEquals(0, WaveBossRules.slowAmp(r * 0.9, r));
        assertEquals(-1, WaveBossRules.slowAmp(r + 0.1, r), "outside: no effect");
        assertTrue(WaveBossRules.sink(1.0, r) < WaveBossRules.sink(6.0, r), "sinks faster at the heart (more negative)");
        assertEquals(0.0, WaveBossRules.sink(r + 1, r), 1e-9);
    }

    @Test
    void theWellLastsFourSecondsAndClosesOnItsLastStep() {
        assertEquals(80, WaveBossRules.WELL_TICKS, "four seconds");
        int closing = 0;
        for (int age = 0; age <= WaveBossRules.WELL_TICKS; age += WaveBossRules.WELL_STEP) if (WaveBossRules.closing(age)) closing++;
        assertEquals(1, closing, "the crush happens once");
        assertFalse(WaveBossRules.closing(0));
        assertFalse(WaveBossRules.closing(WaveBossRules.WELL_TICKS));
    }

    @Test
    void theGatesOpenAheadAndAreEnteredOnlyByStandingInThem() {
        double[] a = WaveBossRules.ahead(10, 10, 0, 5, 2.5);
        assertEquals(10.0, a[0], 1e-9);
        assertEquals(12.5, a[1], 1e-9, "a heading need not be a unit vector");
        double[] zero = WaveBossRules.ahead(1, 1, 0, 0, 3);
        assertEquals(4.0, zero[1], 1e-9, "a zero heading falls back to +z");
        double[] far = WaveBossRules.farTries();
        assertEquals(5, far.length);
        assertEquals(12.0, far[0], 1e-9);
        assertEquals(4.0, far[far.length - 1], 1e-9);
        double[] ga = {0, 64, 0}, gb = {10, 64, 0};
        assertEquals(1, WaveBossRules.which(0.3, 64, 0.3, ga, gb));
        assertEquals(2, WaveBossRules.which(10, 64.5, -0.5, ga, gb));
        assertEquals(0, WaveBossRules.which(5, 64, 0, ga, gb), "between the gates");
        assertEquals(0, WaveBossRules.which(0, 70, 0, ga, gb), "far above");
        assertEquals(0, WaveBossRules.which(1.2, 64, 0, ga, gb), "just outside the radius");
    }

    @Test
    void aGateWillNotTakeTheSameTravellerTwiceInARow() {
        assertFalse(WaveBossRules.reentryOk(100, 90));
        assertTrue(WaveBossRules.reentryOk(100, 70));
        assertTrue(WaveBossRules.reentryOk(100, 0), "never used");
        assertTrue(WaveBossRules.reentryOk(10, 50_000), "a stamp from the future (a reset clock) does not lock the gate");
    }

    @Test
    void theBitePicksTheNearestFoeInFrontWithinReach() {
        double[][] rel = {{0, 0, 3.0}, {0, 0, 1.5}, {0, 0, -1.0}, {0, 0, 6.0}, {3.0, 0, 0.5}};
        assertEquals(1, WaveBossRules.pickTarget(rel, 0, 0, 1), "the nearest of those in front");
        assertEquals(0, WaveBossRules.pickTarget(new double[][]{{0, 0, -1.0}}, 0, 0, -1), "looking back, the one behind is the one in front");
        assertEquals(-1, WaveBossRules.pickTarget(new double[][]{{0, 0, 6.0}}, 0, 0, 1), "out of reach");
        assertEquals(-1, WaveBossRules.pickTarget(new double[][]{{0, 0, -2.0}}, 0, 0, 1), "behind you");
        assertEquals(-1, WaveBossRules.pickTarget(new double[0][], 0, 0, 1));
        assertEquals(-1, WaveBossRules.pickTarget(new double[][]{{0, 0, 0}}, 0, 0, 1), "a zero vector is not a target");
    }

    @Test
    void theLungeEndsAboutAtTheFoeAndNeverRunsAway() {
        assertEquals(0.0, WaveBossRules.lungeSpeed(0), 1e-9);
        assertTrue(WaveBossRules.lungeSpeed(4.0) <= 0.9 + 1e-9);
        assertTrue(WaveBossRules.lungeSpeed(2.0) < WaveBossRules.lungeSpeed(4.0));
        assertEquals(0.0, WaveBossRules.lungeSpeed(-3), 1e-9);
        assertEquals(12, WaveBossRules.BITE_DELAY, "0.6 seconds of open jaws before the snap");
    }

    @Test
    void theBossDropChanceIsKeptInRange() {
        assertEquals(0.08, WaveBossRules.clampChance(Double.NaN), 1e-9);
        assertEquals(0.0, WaveBossRules.clampChance(-1), 1e-9);
        assertEquals(1.0, WaveBossRules.clampChance(5), 1e-9);
        assertTrue(WaveBossRules.drops(0.08, 0.0));
        assertFalse(WaveBossRules.drops(0.08, 0.08));
        assertFalse(WaveBossRules.drops(0.0, 0.0), "a chance of zero never drops");
        assertTrue(WaveBossRules.drops(1.0, 0.999999));
    }
}
