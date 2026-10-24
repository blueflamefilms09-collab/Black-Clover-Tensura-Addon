package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.MobilityRules.Step;
import com.newuniverse.nusmp.mana.method.RuneCharge.Charge;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The two mobility runes: what the air step does each tick, where its blocks go, the caps; the dash maths. */
class MobilityRulesTest {
    private static final int MIN = RuneCharge.DEFAULT_MIN_HOLD, MAX = RuneCharge.DEFAULT_MAX_HOLD;

    // ---------------------------------------------------------------- air step
    @Test
    void sneakingAlwaysDropsAndTheAirHoldsAndTheGroundNeedsNoHelp() {
        for (boolean ground : new boolean[]{false, true})
            for (boolean own : new boolean[]{false, true}) assertEquals(Step.DROP, MobilityRules.decide(true, ground, own));
        assertEquals(Step.PLACE, MobilityRules.decide(false, false, false));
        assertEquals(Step.PLACE, MobilityRules.decide(false, false, true));
        assertEquals(Step.REFRESH, MobilityRules.decide(false, true, true), "standing on our own block keeps it");
        assertEquals(Step.NONE, MobilityRules.decide(false, true, false), "real ground needs no platform");
    }

    @Test
    void theCellUnderTheFeetIsTheBlockBelowThem() {
        assertEquals(63, MobilityRules.cellY(64.0));
        assertEquals(63, MobilityRules.cellY(64.0004), "a hair above the floor is still standing on it");
        assertEquals(64, MobilityRules.cellY(65.0));
        assertEquals(63, MobilityRules.cellY(64.9));
        assertEquals(-1, MobilityRules.cellY(0.0));
        assertEquals(-2, MobilityRules.cellY(-0.5));
    }

    @Test
    void theFootprintIsOneToFourDistinctCellsOnOneLevel() {
        int[] out = new int[12];
        assertEquals(1, MobilityRules.footprint(10.5, 70.0, 20.5, out), "the middle of a block: one cell");
        assertArrayEquals(new int[]{10, 69, 20}, new int[]{out[0], out[1], out[2]});
        int two = MobilityRules.footprint(10.95, 70.0, 20.5, out);
        assertEquals(2, two, "on an edge: two");
        assertDistinct(out, two);
        int four = MobilityRules.footprint(10.98, 70.0, 20.98, out);
        assertEquals(4, four, "on a corner: four");
        assertDistinct(out, four);
        assertEquals(1, MobilityRules.footprint(-0.5, 70.0, -0.5, out));
        assertEquals(-1, out[0]);
        assertEquals(-1, out[2]);
    }

    private static void assertDistinct(int[] out, int n) {
        for (int i = 0; i < n; i++) {
            assertEquals(out[1], out[i * 3 + 1], "one level");
            for (int j = i + 1; j < n; j++) assertFalse(out[i * 3] == out[j * 3] && out[i * 3 + 2] == out[j * 3 + 2], "cells " + i + " and " + j + " repeat");
        }
    }

    @Test
    void theCapsAreFourPerCasterAndSixtyFourInTheWorld() {
        assertTrue(MobilityRules.cellAllowed(0, 0) && MobilityRules.cellAllowed(3, 10));
        assertFalse(MobilityRules.cellAllowed(MobilityRules.MAX_CELLS_PER_CASTER, 0));
        assertFalse(MobilityRules.cellAllowed(0, MobilityRules.MAX_CELLS_GLOBAL));
        assertEquals(MobilityRules.MAX_SESSIONS * MobilityRules.MAX_CELLS_PER_CASTER, MobilityRules.MAX_CELLS_GLOBAL, "the world cap is exactly sessions x cells per caster");
    }

    @Test
    void aBlockNobodyNeedsGoesAfterTheTimeToLive() {
        assertFalse(MobilityRules.cellExpired(100, 100));
        assertFalse(MobilityRules.cellExpired(100 + MobilityRules.CELL_TTL - 1, 100));
        assertTrue(MobilityRules.cellExpired(100 + MobilityRules.CELL_TTL, 100));
    }

    @Test
    void theDurationIsKeptInRange() {
        assertEquals(MobilityRules.MIN_SECONDS, MobilityRules.seconds(-4));
        assertEquals(MobilityRules.MAX_SECONDS, MobilityRules.seconds(100000));
        assertEquals(6, MobilityRules.seconds(6));
        assertEquals(6, MobilityRules.DEFAULT_SECONDS);
    }

    // ---------------------------------------------------------------- dash
    @Test
    void theDashStrengthIsKeptInRangeAndNanIsTheDefault() {
        assertEquals(MobilityRules.DEFAULT_STRENGTH, MobilityRules.strength(Double.NaN));
        assertEquals(MobilityRules.MIN_STRENGTH, MobilityRules.strength(-3));
        assertEquals(MobilityRules.MAX_STRENGTH, MobilityRules.strength(99));
    }

    @Test
    void aLongerHoldDashesFurtherAndNothingExceedsTheMaxSpeed() {
        double tap = MobilityRules.dashSpeed(0.9, RuneCharge.of(0, MIN, MAX), MAX), mid = MobilityRules.dashSpeed(0.9, RuneCharge.of(MAX / 2, MIN, MAX), MAX), full = MobilityRules.dashSpeed(0.9, RuneCharge.of(MAX, MIN, MAX), MAX);
        assertTrue(tap < mid && mid < full);
        assertEquals(0.9 * MobilityRules.TAP_MULT, tap, 1e-12);
        assertEquals(0.9 * MobilityRules.FULL_MULT, full, 1e-12);
        assertEquals(0.9, MobilityRules.dashSpeed(0.9, Charge.LEGACY, MAX), 1e-12, "no measured hold: the configured strength as it is");
        assertEquals(0.9, MobilityRules.dashSpeed(0.9, null, MAX), 1e-12);
        assertEquals(MobilityRules.MAX_SPEED, MobilityRules.dashSpeed(50, RuneCharge.of(MAX, MIN, MAX), MAX), 1e-12);
        assertTrue(MobilityRules.dashSpeed(2.0, RuneCharge.of(500, MIN, MAX), 0) <= MobilityRules.MAX_SPEED);
    }

    @Test
    void theVelocityPointsWhereYouLookAndNeverPastTheMaxSpeed() {
        double[] v = new double[3];
        MobilityRules.dashVelocity(0, 0, 1, 1.0, false, v);
        assertArrayEquals(new double[]{0, 0, 1.0}, v, 1e-12);
        MobilityRules.dashVelocity(3, 4, 0, 1.0, false, v);
        assertEquals(1.0, Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]), 1e-12, "an unnormalised look is normalised");
        assertEquals(0.6, v[0], 1e-12);
        assertEquals(0.8, v[1], 1e-12);
        MobilityRules.dashVelocity(0, -1, 0, 9.0, false, v);
        assertEquals(-MobilityRules.MAX_SPEED, v[1], 1e-12);
    }

    @Test
    void aGroundDashGetsALiftAndAnAirDashDoesNot() {
        double[] v = new double[3];
        MobilityRules.dashVelocity(1, 0, 0, 1.0, true, v);
        assertEquals(MobilityRules.GROUND_LIFT, v[1], 1e-12);
        MobilityRules.dashVelocity(1, 0, 0, 1.0, false, v);
        assertEquals(0.0, v[1], 1e-12);
        MobilityRules.dashVelocity(0, 1, 0, 1.0, true, v);
        assertEquals(1.0, v[1], 1e-12, "a steep look is not lifted further");
    }

    @Test
    void aBrokenLookIsNoVelocity() {
        double[] v = {9, 9, 9};
        MobilityRules.dashVelocity(0, 0, 0, 1.0, true, v);
        assertArrayEquals(new double[]{0, 0, 0}, v, 0.0);
        MobilityRules.dashVelocity(Double.NaN, 0, 1, 1.0, true, v);
        assertArrayEquals(new double[]{0, 0, 0}, v, 0.0);
        MobilityRules.dashVelocity(1, 0, 0, Double.NaN, true, v);
        assertArrayEquals(new double[]{0, 0, 0}, v, 0.0);
    }

    @Test
    void theCooldownIsShortAndTheFallIsForgivenOnlyALittle() {
        assertFalse(MobilityRules.dashReady(100, 100));
        assertFalse(MobilityRules.dashReady(100 + MobilityRules.DASH_COOLDOWN - 1, 100));
        assertTrue(MobilityRules.dashReady(100 + MobilityRules.DASH_COOLDOWN, 100));
        assertEquals(0f, MobilityRules.fallAfterDash(0f));
        assertEquals(0f, MobilityRules.fallAfterDash(2f), "a small fall is forgiven");
        assertEquals(17f, MobilityRules.fallAfterDash(20f), 1e-6, "a long fall still hurts: only the grace is taken off");
        assertEquals(0f, MobilityRules.fallAfterDash(-5f));
    }
}
