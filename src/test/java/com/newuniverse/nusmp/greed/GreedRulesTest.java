package com.newuniverse.nusmp.greed;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The pure numbers of the Black Sword Greed: tiers, forms, devouring caps, hunger, sacrifice values. */
class GreedRulesTest {
    @Test
    void eightFormsWithUniqueItemIdsAndSavedNumbers() {
        GreedRules.Form[] forms = GreedRules.Form.values();
        assertEquals(GreedRules.FORMS, forms.length);
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (int i = 0; i < forms.length; i++) {
            assertEquals(i, forms[i].id(), "the form number is saved: entries are only appended");
            assertTrue(ids.add(forms[i].itemId()));
            assertTrue(GreedRules.isGreedPath(forms[i].itemId()));
            assertTrue(forms[i].share() > 0 && forms[i].share() < 1);
        }
        assertEquals("black_sword_greed", GreedRules.Form.SWORD.itemId());
        assertEquals("black_sword_greed_dual_blades", GreedRules.Form.DUAL_BLADES.itemId());
        assertEquals(GreedRules.Form.SWORD, GreedRules.Form.byId(99));
    }

    @Test
    void tiersOpenFormsInOrder() {
        assertEquals(0, GreedRules.tierFor(0));
        assertEquals(0, GreedRules.tierFor(2_499));
        assertEquals(1, GreedRules.tierFor(2_500));
        assertEquals(7, GreedRules.tierFor(2_000_000));
        assertEquals(7, GreedRules.tierFor(1e12));
        for (int t = 1; t <= GreedRules.MAX_TIER; t++) assertTrue(GreedRules.tierThreshold(t) > GreedRules.tierThreshold(t - 1));
        assertTrue(GreedRules.formOpen(0, 0));
        assertFalse(GreedRules.formOpen(1, 0));
        assertTrue(GreedRules.formOpen(7, 7));
        assertFalse(GreedRules.formOpen(8, 7));
        assertFalse(GreedRules.formOpen(-1, 7));
    }

    @Test
    void devouringIsCappedAndGrowsSlowly() {
        double[] big = GreedRules.gains(1e15);
        assertEquals(6.0, big[0]);
        assertEquals(2.5, big[1]);
        assertEquals(1.5, big[2]);
        double[] zero = GreedRules.gains(-5);
        assertEquals(0.0, zero[0]);
        assertTrue(GreedRules.gains(10_000)[0] < 2 * GreedRules.gains(2_500)[0] + 1e-9, "square-root growth");
        assertTrue(GreedRules.feed(0) >= 3.0 && GreedRules.feed(1e15) <= 25.0);
        assertTrue(GreedRules.absorbShare(true) > GreedRules.absorbShare(false));
    }

    @Test
    void hungerIsBoundedAndEndsAtTheTopTier() {
        double calm = GreedRules.decayPerSecond(0, 0, 0, false, 0, 100);
        double heavy = GreedRules.decayPerSecond(GreedRules.MAX_HP, GreedRules.MAX_ATK, GreedRules.MAX_ARMOR, true, 0, 100);
        assertTrue(calm > 0 && heavy > calm && heavy <= 2.0);
        assertEquals(0.0, GreedRules.decayPerSecond(100, 30, 10, true, GreedRules.MAX_TIER, 100));
        assertEquals(0.0, GreedRules.decayPerSecond(100, 30, 10, true, 0, 0));
        assertTrue(GreedRules.starving(10f, 3));
        assertFalse(GreedRules.starving(10f, GreedRules.MAX_TIER));
        assertFalse(GreedRules.starving(50f, 0));
    }

    @Test
    void sacrificeValuesRiseWithSkillType() {
        assertTrue(GreedRules.sacrificeEp("ULTIMATE") > GreedRules.sacrificeEp("UNIQUE"));
        assertTrue(GreedRules.sacrificeEp("UNIQUE") > GreedRules.sacrificeEp("EXTRA"));
        assertTrue(GreedRules.sacrificeEp("EXTRA") > GreedRules.sacrificeEp("COMMON"));
        assertEquals(GreedRules.sacrificeEp("unique"), GreedRules.sacrificeEp("UNIQUE"));
        assertTrue(GreedRules.sacrificeEp(null) > 0);
        assertTrue(GreedRules.epOfPower(GreedRules.power(10, 5, 2)) > 0);
    }
}
