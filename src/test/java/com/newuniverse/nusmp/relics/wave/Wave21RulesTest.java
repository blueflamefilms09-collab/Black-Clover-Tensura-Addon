package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: wave 21's pure rules (seasons, forms, charges, wither, cones, rifts) are the numbers every weapon of the wave leans on, so they are checked without Minecraft. */
class Wave21RulesTest {
    @Test
    void seasonsRunEightDaysEachAndWrap() {
        assertEquals(Wave21Rules.Season.SPRING, Wave21Rules.season(0));
        assertEquals(Wave21Rules.Season.SPRING, Wave21Rules.season(7));
        assertEquals(Wave21Rules.Season.SUMMER, Wave21Rules.season(8));
        assertEquals(Wave21Rules.Season.AUTUMN, Wave21Rules.season(16));
        assertEquals(Wave21Rules.Season.WINTER, Wave21Rules.season(31));
        assertEquals(Wave21Rules.Season.SPRING, Wave21Rules.season(32));
        assertEquals(Wave21Rules.Season.SPRING, Wave21Rules.season(-5));
        assertEquals("minecraft:in_fire", Wave21Rules.seasonDamageType(Wave21Rules.Season.SUMMER));
        assertEquals("Winter", Wave21Rules.seasonName(Wave21Rules.Season.WINTER));
    }

    @Test
    void theStickCyclesFiveForms() {
        int f = 0;
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < 5; i++) {
            seen.add(Wave21Rules.formName(f));
            f = Wave21Rules.nextForm(f);
        }
        assertEquals(5, seen.size());
        assertEquals(0, f);
        assertEquals(1.0f, Wave21Rules.formScale(0));
        assertTrue(Wave21Rules.formScale(4) < 1.0f);
        assertTrue(Wave21Rules.formScale(2) > Wave21Rules.formScale(1));
    }

    @Test
    void chargesComeBackOnePerPeriodAndStopAtTheMax() {
        assertTrue(java.util.Arrays.equals(new int[]{4, 0}, Wave21Rules.charge(4, 100, 5000, 3400, 4)));
        assertTrue(java.util.Arrays.equals(new int[]{2, 0}, Wave21Rules.charge(1, 0, 3400, 3400, 4)));
        assertTrue(java.util.Arrays.equals(new int[]{1, 3000}, Wave21Rules.charge(1, 2000, 1000, 3400, 4)));
        assertTrue(java.util.Arrays.equals(new int[]{4, 0}, Wave21Rules.charge(3, 3000, 1000, 3400, 4)));
        assertTrue(java.util.Arrays.equals(new int[]{0, 5}, Wave21Rules.charge(-3, 0, 5, 3400, 4)));
        assertTrue(Wave21Rules.canSpend(5, 5));
        assertFalse(Wave21Rules.canSpend(4, 5));
        assertFalse(Wave21Rules.canSpend(9, 0));
    }

    @Test
    void theScytheWithersByStepsUpToACap() {
        assertEquals(0f, Wave21Rules.witherLoss(0));
        assertEquals(0.04f, Wave21Rules.witherLoss(1), 1e-6);
        assertEquals(Wave21Rules.WITHER_CAP, Wave21Rules.witherLoss(100), 1e-6);
        assertTrue(Wave21Rules.ruinDamage(20f) < 20f * 1.0f + 12f, "never an outright kill formula");
        assertEquals(12f, Wave21Rules.ruinDamage(-5f));
    }

    @Test
    void aHeadlessHammerIsAnImprovisedClub() {
        assertEquals(1.0f, Wave21Rules.headlessScale(false));
        assertEquals(0.4f, Wave21Rules.headlessScale(true));
    }

    @Test
    void theMaulCountsLevelsAndTwoHands() {
        assertEquals(0f, Wave21Rules.libraryBonus(0));
        assertEquals(3.0f, Wave21Rules.libraryBonus(20), 1e-6);
        assertEquals(6.0f, Wave21Rules.libraryBonus(500));
        assertEquals(1.15f, Wave21Rules.handScale(true));
        assertEquals(1.0f, Wave21Rules.handScale(false));
    }

    @Test
    void hammertimeWindowIsTheLastTwoSecondsPlusAGrace() {
        assertFalse(Wave21Rules.smiteReady(100, 0));
        assertFalse(Wave21Rules.smiteReady(100, 200));
        assertTrue(Wave21Rules.smiteReady(160, 200));
        assertTrue(Wave21Rules.smiteReady(200, 200));
        assertTrue(Wave21Rules.smiteReady(220, 200));
        assertFalse(Wave21Rules.smiteReady(221, 200));
    }

    @Test
    void theConeIsSixtyDegreesEitherSideAndTwelveBlocksLong() {
        assertTrue(Wave21Rules.inCone(0, 5, 0, 1, 12, 0.5));
        assertTrue(Wave21Rules.inCone(4, 5, 0, 1, 12, 0.5));
        assertFalse(Wave21Rules.inCone(6, 3, 0, 1, 12, 0.5), "too far to the side");
        assertFalse(Wave21Rules.inCone(0, 13, 0, 1, 12, 0.5), "too far away");
        assertFalse(Wave21Rules.inCone(0, -5, 0, 1, 12, 0.5), "behind");
        assertTrue(Wave21Rules.inCone(0, 0, 0, 1, 12, 0.5), "on top of the caster");
    }

    @Test
    void fearIsASaveAndPlayersGetLess() {
        assertEquals(1200, Wave21Rules.fearTicks(false));
        assertTrue(Wave21Rules.fearTicks(true) < 400);
        assertEquals(0.75, Wave21Rules.fearChance(false, 0), 1e-9);
        assertEquals(0.40, Wave21Rules.fearChance(true, 0), 1e-9);
        assertTrue(Wave21Rules.fearChance(false, 0.5) < Wave21Rules.fearChance(false, 0));
        assertEquals(0.075, Wave21Rules.fearChance(false, 5.0), 1e-9, "resistance is capped at 90 percent");
    }

    @Test
    void theRiftLineAndTheEscapeCircle() {
        assertEquals(0.0, Wave21Rules.distToSegment(5, 0, 0, 0, 0, 0, 10, 0, 0), 1e-9);
        assertEquals(2.0, Wave21Rules.distToSegment(5, 2, 0, 0, 0, 0, 10, 0, 0), 1e-9);
        assertEquals(5.0, Wave21Rules.distToSegment(13, 4, 0, 0, 0, 0, 10, 0, 0), 1e-9);
        assertEquals(1.0, Wave21Rules.distToSegment(1, 0, 0, 0, 0, 0, 0, 0, 0), 1e-9);
        assertTrue(Wave21Rules.inZone(1.0, 64.5, 0.5, 0, 64, 0));
        assertFalse(Wave21Rules.inZone(2.0, 64, 0, 0, 64, 0));
        assertFalse(Wave21Rules.inZone(0, 67, 0, 0, 64, 0));
        assertTrue(Wave21Rules.SCATTER_MIN < Wave21Rules.SCATTER_MAX);
    }
}
