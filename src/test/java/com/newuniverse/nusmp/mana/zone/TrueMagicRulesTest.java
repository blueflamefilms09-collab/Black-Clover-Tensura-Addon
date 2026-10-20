package com.newuniverse.nusmp.mana.zone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The True Magic page (Mana Method, an interpretation): natural attributes only, Stage 3 control, environment-fuelled, devils susceptible. */
class TrueMagicRulesTest {
    @Test
    void onlyTheNaturalAttributesAreOffered() {
        for (String n : new String[]{"FLAME", "WATER", "WIND", "EARTH", "LIGHTNING", "PLANT", "flame"}) assertTrue(TrueMagicRules.isNatural(n), n);
        for (String n : new String[]{"TIME", "SPATIAL", "ANTI_MAGIC", "DREAM", "MIRROR", "GRAVITY", "THREAD", null}) assertFalse(TrueMagicRules.isNatural(n), String.valueOf(n));
    }

    @Test
    void stageThreeOrHigherControlIsNeeded() {
        assertTrue(TrueMagicRules.controlEnough(3, false) && TrueMagicRules.controlEnough(0, false));
        assertFalse(TrueMagicRules.controlEnough(4, false) && TrueMagicRules.controlEnough(9, false));
        assertTrue(TrueMagicRules.controlEnough(9, true), "an Arcane or Saint stage holds the runes whatever its number");
    }

    @Test
    void theEnvironmentPaysMoreInDenserMana() {
        assertEquals(0.2, TrueMagicRules.fuelShare(1.0), 1e-9);
        assertEquals(0.4, TrueMagicRules.fuelShare(2.0), 1e-9);
        assertEquals(0.6, TrueMagicRules.fuelShare(3.0), 1e-9);
        assertEquals(0.8, TrueMagicRules.fuelShare(9.0), 1e-9);
        assertTrue(TrueMagicRules.fuelShare(0.0) >= 0.0);
        assertEquals(1.0, TrueMagicRules.powerMult(1.0), 1e-9);
        assertEquals(1.35, TrueMagicRules.powerMult(3.0), 1e-9);
        assertEquals(1.35, TrueMagicRules.powerMult(30.0), 1e-9);
    }

    @Test
    void devilsAreSusceptibleAndBossesLessSo() {
        assertEquals(1.0, TrueMagicRules.devilMult(false, false), 0.0);
        assertEquals(1.6, TrueMagicRules.devilMult(true, false), 0.0);
        assertEquals(1.3, TrueMagicRules.devilMult(true, true), 0.0);
        assertTrue(TrueMagicRules.damage(2.0, true, false) > TrueMagicRules.damage(2.0, false, false) * 1.5);
        assertTrue(TrueMagicRules.damage(2.0, false, true) < TrueMagicRules.damage(2.0, false, false), "a boss takes less");
        assertTrue(TrueMagicRules.RAW_DAMAGE > 6.0 * 3, "far stronger than Mana Strike (7) or one bolt of the volley (6)");
        assertTrue(TrueMagicRules.devilPath("devil_avatar") && TrueMagicRules.devilPath("zagred") && TrueMagicRules.devilPath("greater_daemon") && TrueMagicRules.devilPath("demon_lord"));
        assertFalse(TrueMagicRules.devilPath("zombie") || TrueMagicRules.devilPath(null));
    }
}
