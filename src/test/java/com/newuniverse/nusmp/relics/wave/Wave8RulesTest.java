package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Relics of Many Worlds, wave 8: the pure rules of the ten blades and axes (soul fragments, charges, combo, saves, cones, tables). */
class Wave8RulesTest {
    @Test
    void soulMilestonesFollowTheCard() {
        assertEquals(0, Wave8Rules.maxCharges(4));
        assertEquals(2, Wave8Rules.maxCharges(5));
        assertEquals(2, Wave8Rules.maxCharges(14));
        assertEquals(3, Wave8Rules.maxCharges(15));
        assertFalse(Wave8Rules.darkAura(9));
        assertTrue(Wave8Rules.darkAura(10));
        assertFalse(Wave8Rules.canRaise(19));
        assertTrue(Wave8Rules.canRaise(20));
        assertFalse(Wave8Rules.lichSpeaks(24));
        assertTrue(Wave8Rules.lichSpeaks(25));
        assertFalse(Wave8Rules.lichCalls(29));
        assertTrue(Wave8Rules.lichCalls(30));
    }

    @Test
    void chargesComeBackAtDawn() {
        long d = Wave8Rules.dayOf(24000L * 3 + 5);
        assertEquals(3, d);
        assertEquals(2, Wave8Rules.chargesLeft(5, d, 0, d));
        assertEquals(1, Wave8Rules.chargesLeft(5, d, 1, d));
        assertEquals(0, Wave8Rules.chargesLeft(5, d, 2, d));
        assertEquals(2, Wave8Rules.chargesLeft(5, d - 1, 2, d), "a new day refills");
        assertEquals(0, Wave8Rules.chargesLeft(3, d, 0, d), "no fragments, no charges");
        assertEquals(3, Wave8Rules.chargesLeft(20, d, 0, d));
        assertEquals(1, Wave8Rules.usedAfter(d - 1, 7, d));
        assertEquals(3, Wave8Rules.usedAfter(d, 2, d));
        assertEquals(-1, Wave8Rules.dayOf(-1));
    }

    @Test
    void soulsAreCappedAndCleansed() {
        assertEquals(1, Wave8Rules.addSoul(0));
        assertEquals(Wave8Rules.MAX_SOULS, Wave8Rules.addSoul(Wave8Rules.MAX_SOULS));
        assertEquals(0, Wave8Rules.cleanse(12, false), "a golden apple clears a quiet curse");
        assertEquals(26, Wave8Rules.cleanse(26, false), "but not once the lich speaks");
        assertEquals(0, Wave8Rules.cleanse(26, true), "an enchanted apple takes it all");
    }

    @Test
    void lifestealHealsFirstThenGivesTemporaryHitPoints() {
        float[] a = Wave8Rules.lifesteal(6f, 10f, 20f, 8f);
        assertEquals(6f, a[0], 1e-6);
        assertEquals(0f, a[1], 1e-6);
        float[] b = Wave8Rules.lifesteal(6f, 18f, 20f, 8f);
        assertEquals(2f, b[0], 1e-6);
        assertEquals(4f, b[1], 1e-6);
        float[] c = Wave8Rules.lifesteal(30f, 20f, 20f, 8f);
        assertEquals(0f, c[0], 1e-6);
        assertEquals(8f, c[1], 1e-6, "temporary hit points are capped");
        float[] z = Wave8Rules.lifesteal(-3f, 5f, 20f, 8f);
        assertEquals(0f, z[0] + z[1], 1e-6);
    }

    @Test
    void twoStrikesInARowOnTheSameTargetBurst() {
        assertEquals(1, Wave8Rules.comboNext(0, 0, 0, 7, 100), "a first strike");
        assertEquals(2, Wave8Rules.comboNext(7, 100, 1, 7, 130), "the same target within the window");
        assertEquals(1, Wave8Rules.comboNext(7, 100, 1, 8, 110), "another target starts over");
        assertEquals(1, Wave8Rules.comboNext(7, 100, 1, 7, 100 + Wave8Rules.COMBO_WINDOW + 1), "too late");
        assertEquals(1, Wave8Rules.comboNext(7, 100, 1, 7, 90), "time never runs backwards");
        assertEquals(1, Wave8Rules.comboNext(7, 100, 0, 7, 110), "after a burst the count is 0 and it starts over");
    }

    @Test
    void savingThrowsDependOnToughness() {
        assertTrue(Wave8Rules.saves(10, 40));
        assertFalse(Wave8Rules.saves(40, 40));
        assertFalse(Wave8Rules.saves(0, 0));
        assertTrue(Wave8Rules.saves(99, 100));
        assertEquals(40, Wave8Rules.savePercent(false, false, 10f, 40));
        assertEquals(48, Wave8Rules.savePercent(false, false, 30f, 40));
        assertEquals(55, Wave8Rules.savePercent(false, false, 100f, 40));
        assertEquals(30, Wave8Rules.savePercent(true, false, 10f, 40), "players are a little easier to hold");
        assertTrue(Wave8Rules.savePercent(false, true, 500f, 40) >= 75, "a boss saves at least three times in four");
        assertTrue(Wave8Rules.savePercent(true, true, 20f, 5) >= 75);
        assertTrue(Wave8Rules.savePercent(true, false, 1f, 0) >= 5, "never certain");
    }

    @Test
    void conesAndSegments() {
        assertTrue(Wave8Rules.inCone(0, 0, 5, 0, 0, 1, 10, 0.8));
        assertFalse(Wave8Rules.inCone(0, 0, 11, 0, 0, 1, 10, 0.8), "out of range");
        assertFalse(Wave8Rules.inCone(5, 0, 1, 0, 0, 1, 10, 0.8), "off to the side");
        assertFalse(Wave8Rules.inCone(0, 0, -5, 0, 0, 1, 10, 0.8), "behind");
        assertTrue(Wave8Rules.inCone(0, 0, 0, 0, 0, 1, 10, 0.8), "on the user");
        assertFalse(Wave8Rules.inCone(0, 0, 3, 0, 0, 0, 10, 0.8), "no look vector, no cone");
        assertEquals(0.0, Wave8Rules.segDist2(5, 0, 0, 0, 0, 0, 10, 0, 0), 1e-9);
        assertEquals(4.0, Wave8Rules.segDist2(5, 2, 0, 0, 0, 0, 10, 0, 0), 1e-9);
        assertEquals(9.0 + 16.0, Wave8Rules.segDist2(-3, 4, 0, 0, 0, 0, 10, 0, 0), 1e-9, "beyond the end: distance to the end point");
        assertEquals(25.0, Wave8Rules.segDist2(3, 4, 0, 0, 0, 0, 0, 0, 0), 1e-9, "a degenerate segment is a point");
    }

    @Test
    void theSmallTables() {
        assertEquals(1, Wave8Rules.copperIngots(1, 1));
        assertEquals(3, Wave8Rules.copperIngots(4, 4));
        assertEquals(2, Wave8Rules.copperIngots(2, 3));
        for (int a = 1; a <= 4; a++) for (int b = 1; b <= 4; b++) {
            int n = Wave8Rules.copperIngots(a, b);
            assertTrue(n >= 1 && n <= 3, "2d4 stays 1 to 3 ingots");
        }
        assertEquals(3f, Wave8Rules.bonusVs(6f, true, 3f), 1e-6);
        assertEquals(6f, Wave8Rules.bonusVs(6f, false, 3f), 1e-6);
        assertEquals(8f, Wave8Rules.twoHanded(10f, true), 1e-6);
        assertEquals(10f, Wave8Rules.twoHanded(10f, false), 1e-6);
        assertEquals(0, Wave8Rules.frenzyLevel(-1));
        assertEquals(1, Wave8Rules.frenzyLevel(0));
        assertEquals(2, Wave8Rules.frenzyLevel(1));
        assertEquals(2, Wave8Rules.frenzyLevel(2), "capped at level III");
        assertTrue(Wave8Rules.dim(7));
        assertFalse(Wave8Rules.dim(8));
        assertEquals(3.5f, Wave8Rules.dualityBonus(true, false), 1e-6);
        assertEquals(3.5f, Wave8Rules.dualityBonus(false, true), 1e-6);
        assertEquals(2.0f, Wave8Rules.dualityBonus(false, false), 1e-6);
        double[] b = Wave8Rules.behind(10, 10, 0, 1, 1.6);
        assertEquals(10.0, b[0], 1e-9);
        assertEquals(8.4, b[1], 1e-9, "behind a target that faces +z is at lower z");
        double[] c = Wave8Rules.behind(0, 0, 0, 0, 2.0);
        assertEquals(2.0, c[1], 1e-9, "a target with no facing: any side will do");
    }
}
