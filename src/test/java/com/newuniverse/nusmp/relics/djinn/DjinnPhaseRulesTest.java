package com.newuniverse.nusmp.relics.djinn;

import org.junit.jupiter.api.Test;

import static com.newuniverse.nusmp.relics.djinn.DjinnPhaseRules.*;
import static org.junit.jupiter.api.Assertions.*;

/** Costs, durations, lockouts, assimilation caps: the numbers of the four phases. */
class DjinnPhaseRulesTest {
    private static Snapshot owner(int phase, int trust, double magoi) {
        return new Snapshot(phase, true, false, true, trust, 0f, magoi, 10_000L, 0L, 0L, 0L, false);
    }

    private static Snapshot lent(int phase, float assimilation) {
        return new Snapshot(phase, false, true, false, 20, assimilation, 1.0, 10_000L, 0L, 0L, 0L, false);
    }

    @Test
    void phasesClimbOneStepAtATime() {
        assertEquals(Verdict.OK, request(owner(0, 50, 1.0), LESSER));
        assertEquals(Verdict.ORDER, request(owner(0, 50, 1.0), HALF));
        assertEquals(Verdict.ORDER, request(owner(0, 50, 1.0), FULL));
        assertEquals(Verdict.OK, request(owner(1, 50, 1.0), HALF));
        assertEquals(Verdict.OK, request(owner(2, 50, 1.0), FULL));
        assertEquals(Verdict.OK, request(owner(3, 80, 1.0), EXTREME));
        assertEquals(Verdict.NOT_IN_FULL, request(owner(2, 80, 1.0), EXTREME));
        assertEquals(Verdict.NOT_IN_FULL, request(owner(0, 80, 1.0), EXTREME));
    }

    @Test
    void steppingDownIsAlwaysAllowedEvenWithNothing() {
        Snapshot broke = new Snapshot(3, true, false, false, 0, 100f, 0.0, 5L, 99999L, 99999L, 99999L, false);
        assertEquals(Verdict.OK, request(broke, OFF));
        assertEquals(Verdict.OK, request(broke, HALF));
    }

    @Test
    void magoiTrustLockoutAndCooldownGateTheEntries() {
        assertEquals(Verdict.NEED_MAGOI, request(owner(0, 50, 0.05), LESSER));
        assertEquals(Verdict.NEED_MAGOI, request(owner(2, 50, 0.30), FULL));
        assertEquals(Verdict.NEED_TRUST, request(owner(2, FULL_TRUST - 1, 1.0), FULL));
        assertEquals(Verdict.OK, request(owner(2, FULL_TRUST, 1.0), FULL));
        assertEquals(Verdict.NEED_TRUST, request(owner(3, EXTREME_TRUST - 1, 1.0), EXTREME));
        assertEquals(Verdict.NEED_MAGOI, request(owner(3, 90, 0.59), EXTREME));
        Snapshot locked = new Snapshot(2, true, false, true, 90, 0f, 1.0, 100L, 500L, 0L, 0L, false);
        assertEquals(Verdict.LOCKED_OUT, request(locked, FULL));
        Snapshot cooling = new Snapshot(3, true, false, true, 90, 0f, 1.0, 100L, 0L, 0L, 500L, false);
        assertEquals(Verdict.COOLDOWN, request(cooling, EXTREME));
        Snapshot lesserWait = new Snapshot(0, true, false, true, 90, 0f, 1.0, 100L, 0L, 500L, 0L, false);
        assertEquals(Verdict.COOLDOWN, request(lesserWait, LESSER));
    }

    @Test
    void aSealWithoutABondOrAVesselDoesNothing() {
        assertEquals(Verdict.NOT_BOUND, request(new Snapshot(0, false, false, true, 50, 0f, 1.0, 1L, 0, 0, 0, false), LESSER));
        assertEquals(Verdict.NO_VESSEL, request(new Snapshot(0, true, false, false, 50, 0f, 1.0, 1L, 0, 0, 0, false), LESSER));
    }

    @Test
    void creativePlayersPayAndWaitForNothing() {
        Snapshot c = new Snapshot(2, true, false, true, 0, 0f, 0.0, 1L, 99999L, 99999L, 99999L, true);
        assertEquals(Verdict.OK, request(c, FULL));
        Snapshot c0 = new Snapshot(0, true, false, true, 0, 0f, 0.0, 1L, 99999L, 99999L, 99999L, true);
        assertEquals(Verdict.OK, request(c0, LESSER));
    }

    @Test
    void aHouseholdMemberReachesOnlyTheLesserSummonAndIsRefusedPastHalf() {
        assertEquals(Verdict.OK, request(lent(0, 0f), LESSER));
        assertEquals(Verdict.HOUSEHOLD_LIMIT, request(lent(1, 0f), HALF));
        assertEquals(Verdict.HOUSEHOLD_LIMIT, request(lent(1, 0f), FULL));
        assertEquals(Verdict.OK, request(lent(0, HOUSEHOLD_CAP - 0.1f), LESSER));
        assertEquals(Verdict.ASSIMILATED, request(lent(0, HOUSEHOLD_CAP), LESSER));
    }

    @Test
    void tenLesserSummonsOfAHouseholdMemberReachTheCap() {
        float a = 0f;
        int summons = 0;
        while (a < HOUSEHOLD_CAP) { a += assimilationPerSecond(LESSER, true) * (LESSER_TICKS / 20); summons++; }
        assertEquals(10, summons);
        float resting = 0f;
        for (int s = 0; s < 3600; s++) resting += assimilationDecayPerSecond(true);
        assertTrue(resting >= 18f - 0.01f, "an hour of rest takes back 18%");
    }

    @Test
    void theOwnerIsStoppedAtOneHundredAndStrainedFromNinety() {
        Snapshot full = new Snapshot(1, true, false, true, 90, OWNER_CAP, 1.0, 1L, 0, 0, 0, false);
        assertEquals(Verdict.ASSIMILATED, request(full, HALF));
        Snapshot nine = new Snapshot(1, true, false, true, 90, OWNER_STRAIN - 1, 1.0, 1L, 0, 0, 0, false);
        assertEquals(Verdict.OK, request(nine, HALF));
        assertEquals(1.25, strainMultiplier(OWNER_STRAIN));
        assertEquals(1.0, strainMultiplier(OWNER_STRAIN - 0.1f));
        assertEquals(1.4 * 1.25, drainFactor(BondRules.DJINN | BondRules.SPIRIT, true, 95f), 1e-9);
    }

    @Test
    void aFullMasoCostsLessThanTheBarButNotMuchLess() {
        double total = entryFraction(FULL) + upkeepFraction(FULL) * (FULL_MAX_TICKS / 20.0);
        assertTrue(total < 1.0 && total > 0.7, "a whole Maso from a full bar: " + total);
        assertTrue(entryFraction(EXTREME) == 0.60);
        assertTrue(requiredFraction(EXTREME) >= entryFraction(EXTREME));
        for (int p = LESSER; p <= EXTREME; p++) assertTrue(requiredFraction(p) >= entryFraction(p), "phase " + p);
        assertTrue(upkeepFraction(LESSER) < upkeepFraction(HALF) && upkeepFraction(HALF) <= upkeepFraction(FULL) + 0.001);
    }

    @Test
    void endingMasoEarlyIsCheapEndingItLateOrBrokeIsNot() {
        assertEquals(0.0, severity(EndReason.VOLUNTARY, BACKLASH_FREE - 1));
        assertEquals(0.8 * BACKLASH_FREE / FULL_MAX_TICKS, severity(EndReason.VOLUNTARY, BACKLASH_FREE), 1e-9);
        assertEquals(0.8, severity(EndReason.VOLUNTARY, FULL_MAX_TICKS), 1e-9);
        assertEquals(1.0, severity(EndReason.TIMEOUT, FULL_MAX_TICKS));
        assertEquals(0.6, severity(EndReason.DEPLETED, 10), 1e-9);
        assertEquals(0.0, severity(EndReason.DEATH, 1000));
        assertEquals(0.0, severity(EndReason.LOGOUT, 1000));
        assertEquals(LOCKOUT_MAX, lockoutTicks(EndReason.TIMEOUT, FULL_MAX_TICKS), "five minutes");
        assertEquals(LOCKOUT_MIN, lockoutTicks(EndReason.VOLUNTARY, 0));
        assertEquals(LOCKOUT_MAX, lockoutTicks(EndReason.VOLUNTARY, FULL_MAX_TICKS));
        assertEquals(0, lockoutTicks(EndReason.DEATH, 500));
        assertTrue(lockoutTicks(EndReason.DEPLETED, 0) >= LOCKOUT_MIN + 0.6 * (LOCKOUT_MAX - LOCKOUT_MIN) - 1);
        int prev = -1;
        for (int used = 0; used <= FULL_MAX_TICKS; used += 100) {
            int l = lockoutTicks(EndReason.VOLUNTARY, used);
            assertTrue(l >= prev, "a longer Maso never locks for less");
            prev = l;
        }
    }

    @Test
    void theFullBacklashIsSlownessWeaknessNauseaAndHunger() {
        Backlash b = backlash(1.0);
        assertTrue(b.slowTicks() > 0 && b.slowAmp() >= 1 && b.weakTicks() > 0 && b.nauseaTicks() > 0 && b.hungerTicks() > 0 && b.foodLoss() > 0);
        assertTrue(backlash(0).none());
        assertTrue(backlash(-1).none());
        Backlash mild = backlash(0.1);
        assertEquals(0, mild.nauseaTicks(), "nausea needs a heavy backlash");
        assertTrue(mild.slowTicks() < b.slowTicks() && mild.weakTicks() < b.weakTicks());
        assertEquals(0, mild.healthLoss(), "a mild backlash takes no health");
        assertEquals(4, b.healthLoss(), "a full one takes two hearts of maximum health for three minutes");
        assertEquals(3600, b.strainTicks());
        assertEquals(60, b.blindTicks());
        assertEquals(0, backlash(0.7).blindTicks());
    }

    @Test
    void theStrainHealsBackLinearly() {
        assertEquals(-4.0, strainAmount(4, 3600, 3600), 1e-9);
        assertEquals(-2.0, strainAmount(4, 1800, 3600), 1e-9);
        assertEquals(0.0, strainAmount(4, 0, 3600));
        assertEquals(0.0, strainAmount(0, 100, 3600));
        assertEquals(-4.0, strainAmount(4, 9999, 3600), 1e-9, "never more than the loss");
    }

    @Test
    void drawRukhIsBoundedAndFollowsTheLocalDensity() {
        assertEquals(RUKH_PER_SECOND, rukhGain(1.0), 1e-12);
        assertTrue(rukhGain(1.8) > rukhGain(1.0), "a Grand Magic Zone gives more");
        assertEquals(RUKH_PER_SECOND * RUKH_MAX_DENSITY, rukhGain(50), 1e-12);
        assertEquals(RUKH_PER_SECOND * 0.25, rukhGain(-3), 1e-12);
        double wholeChannel = rukhGain(1.0) * (RUKH_MAX_TICKS / 20);
        assertTrue(wholeChannel <= 0.3, "a whole channel at normal density is a quarter of the pool at most: " + wholeChannel);
        assertTrue(VESSEL_REGEN > 0 && VESSEL_REGEN <= 0.3);
    }

    @Test
    void trustRisesOnACleanExitAndFallsOnABurnOut() {
        assertEquals(2, trustDelta(EndReason.VOLUNTARY, BACKLASH_FREE));
        assertEquals(0, trustDelta(EndReason.VOLUNTARY, 10));
        assertTrue(trustDelta(EndReason.TIMEOUT, FULL_MAX_TICKS) < 0);
        assertTrue(trustDelta(EndReason.DEPLETED, 100) < trustDelta(EndReason.TIMEOUT, 100));
    }

    @Test
    void theWholeMasoMeasuresUpAgainstItsOwnLimits() {
        assertEquals(1800, FULL_MAX_TICKS, "90 seconds");
        assertEquals(6000, LOCKOUT_MAX, "5 minutes");
        assertEquals(60, EXTREME_CHARGE, "3 seconds of warning");
        assertTrue(EXTREME_COOLDOWN > LOCKOUT_MAX);
        assertTrue(MASO_ATTACK <= 6 && MASO_SPEED <= 0.35, "big, not unbounded");
    }
}
