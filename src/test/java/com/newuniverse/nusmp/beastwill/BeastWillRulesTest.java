package com.newuniverse.nusmp.beastwill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why (3.45.0): the arithmetic of the Beast Will system (ranks, strain tiers, compatibility, progress, daily budgets) decides whether the system can be farmed, abused or locked, so it is pure and pinned here.
 */
class BeastWillRulesTest {
    @Test
    void ranksComeFromEpAndThereIsNoRankBelowC() {
        assertNull(BeastWillRules.rankOf(10, null));
        assertEquals(BeastRank.C, BeastWillRules.rankOf(1_000, null));
        assertEquals(BeastRank.A, BeastWillRules.rankOf(20_000, null));
        assertEquals(BeastRank.A, BeastWillRules.rankOf(99_999, null));
        assertEquals(BeastRank.S, BeastWillRules.rankOf(100_000, null));
        assertEquals(BeastRank.SS, BeastWillRules.rankOf(9e9, null));
        assertEquals(BeastRank.B, BeastWillRules.rankOf(50, new double[] {1, 10, 100, 1000, 10000}), "custom thresholds");
        assertEquals(BeastRank.A, BeastWillRules.rankOf(50_000, new double[] {1}), "a malformed threshold list falls back to the defaults");
        assertTrue(BeastRank.S.atLeast(BeastRank.A) && !BeastRank.B.atLeast(BeastRank.A));
        assertEquals(BeastRank.SS, BeastRank.byName(" ss "));
        assertNull(BeastRank.byName("Z"));
    }

    @Test
    void aCreatureWithoutEpIsJudgedByItsHealthButATensuraOneKeepsItsEp() {
        assertEquals(20_000, BeastWillRules.effectiveEp(0, 200, false), 1e-9);
        assertEquals(0, BeastWillRules.effectiveEp(0, 200, true), 1e-9);
        assertEquals(30_000, BeastWillRules.effectiveEp(30_000, 10, false), 1e-9);
    }

    @Test
    void strainTiersAreOrderedAndLoseBonuses() {
        assertEquals(BeastWillRules.Tier.CALM, BeastWillRules.tier(0));
        assertEquals(BeastWillRules.Tier.CALM, BeastWillRules.tier(29.9f));
        assertEquals(BeastWillRules.Tier.STRAINED, BeastWillRules.tier(30));
        assertEquals(BeastWillRules.Tier.EXHAUSTED, BeastWillRules.tier(60));
        assertEquals(BeastWillRules.Tier.OVERLOAD, BeastWillRules.tier(85));
        assertEquals(BeastWillRules.Tier.OVERLOAD, BeastWillRules.tier(100));
        double last = 2;
        for (BeastWillRules.Tier t : BeastWillRules.Tier.values()) { assertTrue(t.effectiveness < last); last = t.effectiveness; }
        assertEquals(100f, BeastWillRules.clampStrain(900));
        assertEquals(0f, BeastWillRules.clampStrain(-5));
    }

    @Test
    void poorCompatibilityAndEarlyHoldingRaiseStrainWhileRestingAndMeditationShedIt() {
        double base = BeastWillRules.secondStrain(0.4, 1.0, 1.0, 1.0);
        assertEquals(0.4, base, 1e-9);
        assertEquals(0.8, BeastWillRules.secondStrain(0.4, 0.0, 1.0, 1.0), 1e-9, "no compatibility doubles it");
        assertEquals(1.2, BeastWillRules.secondStrain(0.4, 1.0, 3.0, 1.0), 1e-9, "holding it far too early triples it");
        assertEquals(1.2, BeastWillRules.secondStrain(0.4, 1.0, 9.0, 1.0), 1e-9, "the early-holding factor is capped");
        assertEquals(0.0, BeastWillRules.secondStrain(0.4, 1.0, 1.0, 0.0), 1e-9, "the server can switch strain off");
        assertEquals(1.0, BeastWillRules.prematureMult(BeastWillRules.ACQUIRE, 0.5, 0), 1e-9);
        assertEquals(3.0, BeastWillRules.prematureMult(BeastWillRules.ACQUIRE, 0.0, 1), 1e-9);
        assertEquals(1.0, BeastWillRules.prematureMult(BeastWillRules.INTEGRATE, 1, 0.7), 1e-9);
        assertTrue(BeastWillRules.prematureMult(BeastWillRules.INTEGRATE, 1, 0.35) > 1.9);
        assertEquals(1.5, BeastWillRules.restDecay(0.5, true), 1e-9);
        assertEquals(0.5, BeastWillRules.restDecay(0.5, false), 1e-9);
        assertEquals(0.2, BeastWillRules.compat(false, 0.0), 1e-9, "a poor fit is slow, never impossible");
        assertEquals(1.0, BeastWillRules.compat(true, 0.1), 1e-9);
    }

    @Test
    void theBonusScaleGrowsWithProgressAndShrinksWithStrainAndNeverRunsAway() {
        double fresh = BeastWillRules.statScale(0.1, 0, BeastWillRules.Tier.CALM, 1);
        double full = BeastWillRules.statScale(1, 1, BeastWillRules.Tier.CALM, 1);
        assertTrue(fresh > 0.3 && fresh < 0.5);
        assertEquals(1.0, full, 1e-9);
        assertTrue(BeastWillRules.statScale(1, 1, BeastWillRules.Tier.OVERLOAD, 1) < 0.4);
        assertTrue(BeastWillRules.statScale(1, 1, BeastWillRules.Tier.CALM, 99) <= 2.0, "even a hand-edited server scale is clamped");
        assertEquals(0.0, BeastWillRules.statScale(1, 1, BeastWillRules.Tier.CALM, 0), 1e-9);
    }

    @Test
    void theDailyBudgetCutsAGainToWhatIsLeftOfTheDay() {
        assertEquals(0.05, BeastWillRules.budgeted(0.05, 0.0, 0.15), 1e-9);
        assertEquals(0.03, BeastWillRules.budgeted(0.05, 0.12, 0.15), 1e-9);
        assertEquals(0.0, BeastWillRules.budgeted(0.05, 0.15, 0.15), 1e-9);
        assertEquals(0.0, BeastWillRules.budgeted(0.05, 0.40, 0.15), 1e-9, "never negative");
        assertEquals(3L, BeastWillRules.dayOf(3 * 24000L + 5));
    }

    @Test
    void phasesUnlockOnlyWithTheirProgress() {
        assertTrue(BeastWillRules.phaseUnlocked(BeastWillRules.ACQUIRE, 0.1, 0, 0.1, 0));
        assertFalse(BeastWillRules.phaseUnlocked(BeastWillRules.ACQUIRE, 0.09, 1, 0.1, 0));
        assertFalse(BeastWillRules.phaseUnlocked(BeastWillRules.INTEGRATE, 1.0, 0.4, 0.95, 0.5));
        assertFalse(BeastWillRules.phaseUnlocked(BeastWillRules.INTEGRATE, 0.9, 1.0, 0.95, 0.5));
        assertTrue(BeastWillRules.phaseUnlocked(BeastWillRules.INTEGRATE, 0.95, 0.5, 0.95, 0.5));
    }

    @Test
    void cooldownsStayBetweenHalfAndDoubleTheWrittenValue() {
        assertEquals(100, BeastWillRules.cooldown(100, 1.0));
        assertEquals(50, BeastWillRules.cooldown(100, 0.01));
        assertEquals(200, BeastWillRules.cooldown(100, 50));
    }

    @Test
    void everyKindHasLimitsAndEveryStatTargetHasACap() {
        assertEquals(15, BeastWillRules.KINDS.size(), "the twelve first kinds and venom_spit, maelstrom, dread_gaze");
        for (var e : BeastWillRules.KINDS.entrySet()) assertTrue(e.getValue().minCooldown() > 0 && e.getValue().maxTargets() >= 1, e.getKey());
        assertTrue(BeastWillRules.KINDS.get("time_exclusion").maxDuration() <= 100, "time exclusion is capped at five seconds");
        for (var e : BeastWillRules.ATTR_CAPS.entrySet()) assertTrue(e.getKey().startsWith("minecraft:generic.") && (e.getValue().maxAdd() > 0 || e.getValue().maxMult() > 0), e.getKey());
    }
}
