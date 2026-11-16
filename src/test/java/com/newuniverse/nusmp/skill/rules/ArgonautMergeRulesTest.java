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
        assertEquals(0, ChargeRules.Strike.STRIKE.ordinal());
        assertEquals(1, ChargeRules.Strike.BEAM.ordinal());
        assertEquals(2, ChargeRules.Strike.FIREBOLT.ordinal(), "appended, never moved");
        assertEquals(3, ChargeRules.Strike.CHARGED_FIREBOLT.ordinal());
        assertEquals(4, ChargeRules.Strike.ARGO_VESTA.ordinal());
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

    // ---- wiki canon (danmachi.argonautWikiCanon), docs/argonaut_rework.md section 3

    @Test
    void canonAppendsChargedFireboltAndArgoVestaAfterFirebolt() {
        assertEquals(4, ArgonautMergeRules.modes(false, true, true));
        assertEquals(5, ArgonautMergeRules.modes(true, true, true));
        assertEquals(1, ArgonautMergeRules.modes(false, false, true), "canon needs the merge");
        assertEquals(2, ArgonautMergeRules.modes(true, false, true));
        assertEquals(ChargeRules.Strike.FIREBOLT, ArgonautMergeRules.kind(1, false, true, true), "the old indexes keep their meaning");
        assertEquals(ChargeRules.Strike.CHARGED_FIREBOLT, ArgonautMergeRules.kind(2, false, true, true));
        assertEquals(ChargeRules.Strike.ARGO_VESTA, ArgonautMergeRules.kind(3, false, true, true));
        assertEquals(ChargeRules.Strike.BEAM, ArgonautMergeRules.kind(1, true, true, true));
        assertEquals(ChargeRules.Strike.FIREBOLT, ArgonautMergeRules.kind(2, true, true, true));
        assertEquals(ChargeRules.Strike.CHARGED_FIREBOLT, ArgonautMergeRules.kind(3, true, true, true));
        assertEquals(ChargeRules.Strike.ARGO_VESTA, ArgonautMergeRules.kind(4, true, true, true));
        assertEquals(ChargeRules.Strike.STRIKE, ArgonautMergeRules.kind(3, false, true, false), "canon off: a saved canon mode is the plain strike");
        assertEquals(ChargeRules.Strike.STRIKE, ArgonautMergeRules.kind(4, true, true, false));
        for (int m = -2; m < 8; m++) for (int u = 0; u < 2; u++) for (int g = 0; g < 2; g++) for (int c = 0; c < 2; c++) for (int r = 0; r < 2; r++) {
            int n = ArgonautMergeRules.nextMode(m, r == 1, u == 1, g == 1, c == 1);
            assertTrue(n >= 0 && n < ArgonautMergeRules.modes(u == 1, g == 1, c == 1));
        }
    }

    @Test
    void canonBurnsOnlyTheFireboltInfusedReleases() {
        assertTrue(ArgonautMergeRules.burns(ChargeRules.Strike.ARGO_VESTA, true));
        assertTrue(ArgonautMergeRules.burns(ChargeRules.Strike.BEAM, true), "the pseudo-Afterglow flies on Firebolt");
        assertFalse(ArgonautMergeRules.burns(ChargeRules.Strike.STRIKE, true), "the Hero's Strike is a plain charge");
        assertFalse(ArgonautMergeRules.burns(ChargeRules.Strike.CHARGED_FIREBOLT, true), "it ignites through the bolt itself");
        assertTrue(ArgonautMergeRules.burns(ChargeRules.Strike.STRIKE, false), "4.4.0: the plain strike burned");
    }

    @Test
    void theCapIsThreeFourFiveByMasteryAndTenForTheUltimate() {
        assertEquals(3, ChargeRules.canonMaxCharge(Tier.BASE, 0.0));
        assertEquals(3, ChargeRules.canonMaxCharge(Tier.BASE, 0.49));
        assertEquals(4, ChargeRules.canonMaxCharge(Tier.BASE, ArgonautMergeRules.DUAL_CHARGE_MASTERY));
        assertEquals(5, ChargeRules.canonMaxCharge(Tier.MASTERED, 1.0));
        assertEquals(10, ChargeRules.canonMaxCharge(Tier.ULTIMATE, 0.0));
        assertEquals(10, ChargeRules.canonMaxCharge(Tier.ULTIMATE_MASTERED, 1.0));
        assertEquals(5, ChargeRules.maxCharge(Tier.BASE), "the old cap is untouched");
        assertTrue(ChargeRules.chimeNow(60, 3) && !ChargeRules.chimeNow(80, 3));
        assertEquals(3 * ChargeRules.TICKS_PER_CHARGE + 40, ChargeRules.maxHeldTicks(3));
    }

    @Test
    void masteryUnlocksMovingChargeAndArgoVesta() {
        assertFalse(ArgonautMergeRules.canChargeMoving(true, false, 0.1));
        assertTrue(ArgonautMergeRules.canChargeMoving(true, false, ArgonautMergeRules.MOVING_CHARGE_MASTERY));
        assertTrue(ArgonautMergeRules.canChargeMoving(true, true, 0.0), "the Ultimate always can");
        assertTrue(ArgonautMergeRules.canChargeMoving(false, false, 0.0), "canon off: as before");
        assertFalse(ArgonautMergeRules.unlocked(ChargeRules.Strike.ARGO_VESTA, true, false, 0.3));
        assertTrue(ArgonautMergeRules.unlocked(ChargeRules.Strike.ARGO_VESTA, true, false, 0.5));
        assertTrue(ArgonautMergeRules.unlocked(ChargeRules.Strike.ARGO_VESTA, true, true, 0.0));
        assertTrue(ArgonautMergeRules.unlocked(ChargeRules.Strike.ARGO_VESTA, false, false, 0.0));
        assertTrue(ArgonautMergeRules.unlocked(ChargeRules.Strike.CHARGED_FIREBOLT, true, false, 0.0));
        assertFalse(ArgonautMergeRules.movedTooFar(0.5, 0.5));
        assertTrue(ArgonautMergeRules.movedTooFar(1.0, 0.5));
    }

    @Test
    void aHitBreaksTheChargeAndRecoilGrowsWithIt() {
        assertTrue(ArgonautMergeRules.brokenByHit(20f, 17f));
        assertFalse(ArgonautMergeRules.brokenByHit(20f, 20f));
        assertFalse(ArgonautMergeRules.brokenByHit(17f, 20f), "healing is not a hit");
        assertEquals(0, ArgonautMergeRules.recoilWear(1, false));
        assertEquals(4, ArgonautMergeRules.recoilWear(5, false));
        assertEquals(4, ArgonautMergeRules.recoilWear(10, true), "halved for the Ultimate");
        assertEquals(0, ArgonautMergeRules.recoilWear(0, false));
        assertTrue(ArgonautMergeRules.staminaPerBell() > 0f);
        assertTrue(ArgonautMergeRules.vestaFactor() > 1.0 && ArgonautMergeRules.vestaKnockback() > 1.0);
    }

    @Test
    void fireboltIsWeakAtLongRangeAndAChargedBoltGrows() {
        assertEquals(1.0, ArgonautMergeRules.rangeFalloff(5, 32));
        assertEquals(1.0, ArgonautMergeRules.rangeFalloff(ArgonautMergeRules.SHORT_RANGE, 32));
        assertEquals(ArgonautMergeRules.FALLOFF_FLOOR, ArgonautMergeRules.rangeFalloff(32, 32), 1e-9);
        assertEquals(ArgonautMergeRules.FALLOFF_FLOOR, ArgonautMergeRules.rangeFalloff(100, 32), 1e-9, "never below the floor");
        assertTrue(ArgonautMergeRules.rangeFalloff(24, 32) < 1.0 && ArgonautMergeRules.rangeFalloff(24, 32) > ArgonautMergeRules.FALLOFF_FLOOR);
        assertEquals(1.0, FireboltRules.chargedFactor(0));
        assertEquals(5.0, FireboltRules.chargedFactor(5), 1e-9);
        assertTrue(FireboltRules.chargedFactor(10) > FireboltRules.chargedFactor(5));
        assertEquals(0.0, FireboltRules.chargedSplashBonus(0));
        assertEquals(3.0, FireboltRules.chargedSplashBonus(50), "capped");
    }
}
