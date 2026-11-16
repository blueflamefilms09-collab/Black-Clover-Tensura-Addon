package com.newuniverse.nusmp.skill.rules;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the All Fiction god-class rework (sixteen modes) and Book Maker's lead-in modes only add to the 3.46.0 numbers; the new ones must be bounded and the setting must restore the old wheels. */
class FictionGodRulesTest {
    @Test
    void theWheelsGrowAndTheSettingRestoresTheOldOnes() {
        assertEquals(16, FictionRules.wheel(true, true));
        assertEquals(10, FictionRules.wheel(true, false), "allFictionGod off = the ten modes of 3.46.0");
        assertEquals(2, FictionRules.wheel(false, true), "no rebuild = the old two, whatever the god setting says");
        assertEquals(2, FictionRules.wheel(false, false));
        assertEquals(10, FictionRules.wheel(true), "the old one-argument form is kept");
        assertEquals(4, FictionRules.bookMakerModes(true));
        assertEquals(2, FictionRules.bookMakerModes(false));
        assertEquals(2, FictionRules.modes(), "the old Book Maker count is kept");
    }

    @Test
    void newModesWereOnlyAppendedAndHaveUniqueIds() {
        assertEquals(FictionRules.GOD_MODES, FictionRules.GOD_IDS.length);
        for (int i = 0; i < FictionRules.ALL_MODES; i++) assertEquals(FictionRules.ALL_IDS[i], FictionRules.GOD_IDS[i], "the first ten ids are unchanged");
        assertEquals(10, FictionRules.DENY);
        assertEquals(15, FictionRules.REWRITE_TIME);
        Set<String> ids = new HashSet<>();
        for (String id : FictionRules.GOD_IDS) assertTrue(ids.add(id), "duplicate id " + id);
        assertEquals(2, FictionRules.EQUALIZE);
        assertEquals(3, FictionRules.GLIMPSE);
    }

    @Test
    void everyNewModeHasABoundedPriceAndCooldown() {
        for (int m = FictionRules.DENY; m < FictionRules.GOD_MODES; m++) {
            assertTrue(FictionRules.cost(m) > 0 && FictionRules.cost(m) <= 10000, "cost of mode " + m);
            assertTrue(FictionRules.cooldown(m) >= 600 && FictionRules.cooldown(m) <= 6000, "cooldown of mode " + m);
        }
        assertTrue(FictionRules.cost(FictionRules.MASS) > FictionRules.cost(FictionRules.DENY), "the mass erase is dearer than the single one");
        assertEquals(FictionRules.undoCost(), FictionRules.cost(FictionRules.UNDO), "the old prices are unchanged");
    }

    @Test
    void windowsAreasAndCapsStayBounded() {
        assertTrue(FictionRules.ruleTicks() <= 600 && FictionRules.skinTicks() <= 1200 && FictionRules.reversalTicks() <= 400);
        assertTrue(FictionRules.reversalCharges() >= 1 && FictionRules.reversalCharges() <= 5);
        assertTrue(FictionRules.ruleRadius() <= 12 && FictionRules.massRadius() <= 16 && FictionRules.denyRange() <= 32);
        assertTrue(FictionRules.rulePulseMaxProjectiles() <= 32 && FictionRules.retconPerSecond() <= 2);
        assertTrue(FictionRules.ruleOwnerTakenMultiplier() > 0 && FictionRules.ruleOwnerTakenMultiplier() < 1);
    }

    @Test
    void reversalWritesBackAtMostItsCap() {
        assertEquals(15.0, FictionRules.reversalRaw(10.0), 1e-9);
        assertEquals(FictionRules.reversalRawCap(), FictionRules.reversalRaw(10000.0), 1e-9, "a huge blow is written back no harder than the cap");
        assertEquals(0.0, FictionRules.reversalRaw(-5.0), 1e-9);
        assertTrue(FictionRules.reversalOpen(100, 50, 1));
        assertFalse(FictionRules.reversalOpen(100, 50, 0), "no charges left");
        assertFalse(FictionRules.reversalOpen(100, 100, 3), "window closed");
    }

    @Test
    void massFictionIsMasteredOnlyAndNeverErasesAPlayerOrABoss() {
        assertFalse(FictionRules.massUnlocked(false));
        assertTrue(FictionRules.massUnlocked(true));
        assertTrue(FictionRules.massErases(false, true));
        assertFalse(FictionRules.massErases(true, true), "a player is hit, never erased");
        assertFalse(FictionRules.massErases(false, false), "a boss is hit, never erased");
    }

    @Test
    void zoneAndRewriteTimeRules() {
        assertTrue(FictionRules.inZone(99.0, 10.0));
        assertTrue(FictionRules.inZone(100.0, 10.0));
        assertFalse(FictionRules.inZone(100.1, 10.0));
        assertTrue(FictionRules.resettable(FictionRules.UNDO) && FictionRules.resettable(FictionRules.REVERSAL) && FictionRules.resettable(FictionRules.DENY));
        for (int big : new int[]{FictionRules.ERASE, FictionRules.WORLD, FictionRules.MASS, FictionRules.RULE, FictionRules.REWRITE_TIME, FictionRules.STAND_UP})
            assertFalse(FictionRules.resettable(big), "the big mode " + big + " keeps its cooldown");
    }

    @Test
    void bookMakerLeadInIsBoundedAndMasteryGated() {
        assertFalse(FictionRules.glimpseUnlocked(Tier.BASE));
        assertTrue(FictionRules.glimpseUnlocked(Tier.MASTERED));
        assertFalse(FictionRules.equalizeSilences(Tier.BASE));
        assertTrue(FictionRules.equalizeSilences(Tier.MASTERED));
        assertTrue(FictionRules.equalizeWeakAmp(Tier.MASTERED) > FictionRules.equalizeWeakAmp(Tier.BASE));
        assertEquals(5.0, FictionRules.glimpsePool(20.0), 1e-9);
        assertEquals(0.0, FictionRules.glimpsePool(-1.0));
        assertTrue(FictionRules.glimpseTicks() <= 200 && FictionRules.equalizeStripMax() <= 8);
        assertTrue(FictionRules.glimpseCost() > FictionRules.equalizeCost());
        assertTrue(FictionRules.glimpsePool(20.0) < FictionRules.annulPool(20.0), "the glimpse is the small taste of Annul Damage");
    }
}
