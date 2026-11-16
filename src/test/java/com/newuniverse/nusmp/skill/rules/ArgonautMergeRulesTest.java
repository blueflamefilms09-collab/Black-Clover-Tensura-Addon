package com.newuniverse.nusmp.skill.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Firebolt was folded into Argonaut (docs/argonaut_rework.md); the mode table, the burn and the migration answers must hold, and the old mode numbers must not move. */
class ArgonautMergeRulesTest {
    @Test
    void modeCountsGrowByOneWithTheMerge() {
        assertEquals(1, ArgonautMergeRules.modes(false, false));
        assertEquals(2, ArgonautMergeRules.modes(false, true));
        assertEquals(2, ArgonautMergeRules.modes(true, false));
        assertEquals(3, ArgonautMergeRules.modes(true, true));
    }

    @Test
    void oldModeIndexesKeepTheirMeaning() {
        assertEquals(ChargeRules.Strike.STRIKE, ArgonautMergeRules.kind(0, false, true));
        assertEquals(ChargeRules.Strike.STRIKE, ArgonautMergeRules.kind(0, true, true));
        assertEquals(ChargeRules.Strike.BEAM, ArgonautMergeRules.kind(1, true, true));
        assertEquals(ChargeRules.Strike.BEAM, ArgonautMergeRules.kind(1, true, false));
        assertEquals(ChargeRules.Strike.FIREBOLT, ArgonautMergeRules.kind(1, false, true));
        assertEquals(ChargeRules.Strike.FIREBOLT, ArgonautMergeRules.kind(2, true, true));
        assertEquals(ChargeRules.Strike.FIREBOLT.ordinal(), ChargeRules.Strike.values().length - 1, "Firebolt is appended last");
    }

    @Test
    void aSavedModeOutsideTheLiveRangeIsTheStrike() {
        assertEquals(ChargeRules.Strike.STRIKE, ArgonautMergeRules.kind(2, true, false), "merge turned off: the Firebolt index is gone");
        assertEquals(ChargeRules.Strike.STRIKE, ArgonautMergeRules.kind(1, false, false));
        assertEquals(ChargeRules.Strike.STRIKE, ArgonautMergeRules.kind(-1, true, true));
        assertEquals(ChargeRules.Strike.STRIKE, ArgonautMergeRules.kind(99, true, true));
    }

    @Test
    void theModeCycleWrapsAndStaysInRange() {
        assertEquals(1, ArgonautMergeRules.nextMode(0, false, false, true));
        assertEquals(0, ArgonautMergeRules.nextMode(1, false, false, true));
        assertEquals(2, ArgonautMergeRules.nextMode(1, false, true, true));
        assertEquals(0, ArgonautMergeRules.nextMode(2, false, true, true));
        assertEquals(2, ArgonautMergeRules.nextMode(0, true, true, true));
        assertEquals(0, ArgonautMergeRules.nextMode(0, false, false, false), "one mode: it stays");
        assertEquals(1, ArgonautMergeRules.nextMode(0, false, true, false), "the old Hero toggle 0 <-> 1");
        assertEquals(0, ArgonautMergeRules.nextMode(1, false, true, false));
        assertEquals(0, ArgonautMergeRules.nextMode(7, false, false, true), "a stale index is clamped first");
        for (int m = -2; m < 6; m++) for (int u = 0; u < 2; u++) for (int g = 0; g < 2; g++) for (int r = 0; r < 2; r++) {
            int n = ArgonautMergeRules.nextMode(m, r == 1, u == 1, g == 1);
            assertTrue(n >= 0 && n < ArgonautMergeRules.modes(u == 1, g == 1));
        }
    }

    @Test
    void aChargedStrikeBurnsInProportionAndIsCapped() {
        assertEquals(0, ArgonautMergeRules.chargedIgniteSeconds(0));
        assertEquals(0, ArgonautMergeRules.chargedIgniteSeconds(1), "a single bell is a plain strike");
        assertEquals(4, ArgonautMergeRules.chargedIgniteSeconds(2));
        assertEquals(7, ArgonautMergeRules.chargedIgniteSeconds(5));
        assertEquals(ArgonautMergeRules.MAX_IGNITE_SECONDS, ArgonautMergeRules.chargedIgniteSeconds(10));
        assertEquals(ArgonautMergeRules.MAX_IGNITE_SECONDS, ArgonautMergeRules.chargedIgniteSeconds(1000));
        assertEquals(0, ArgonautMergeRules.chargedIgniteSeconds(-3));
    }

    @Test
    void firebolMasteryIsHitOnlyAndSlowerThanTheOldSkill() {
        assertEquals(0.0, ArgonautMergeRules.fireboltMastery(false));
        assertTrue(ArgonautMergeRules.fireboltMastery(true) > 0 && ArgonautMergeRules.fireboltMastery(true) < FireboltRules.masteryGain(true));
    }

    @Test
    void theMigrationAnswers() {
        assertTrue(ArgonautMergeRules.migrates(true, true));
        assertFalse(ArgonautMergeRules.migrates(false, true), "merge off: nothing is moved");
        assertFalse(ArgonautMergeRules.migrates(true, false));
        assertTrue(ArgonautMergeRules.grantsArgonaut(false, false));
        assertFalse(ArgonautMergeRules.grantsArgonaut(true, false));
        assertFalse(ArgonautMergeRules.grantsArgonaut(false, true), "the Ultimate already has the Firebolt mode");
        assertTrue(ArgonautMergeRules.restores(false, true, false));
        assertFalse(ArgonautMergeRules.restores(false, true, true), "never a second Firebolt");
        assertFalse(ArgonautMergeRules.restores(false, false, false), "only a migrated holder gets it back");
        assertFalse(ArgonautMergeRules.restores(true, true, false));
    }
}
