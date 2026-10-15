package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 29: the pure rules of the four potions (docs: relics4/doc_wave29.md). */
class Wave29RulesTest {
    @Test
    void diceStayInRange() {
        assertEquals(1, Wave29Rules.die(n -> 0, 4));
        assertEquals(4, Wave29Rules.die(n -> n - 1, 4));
        assertEquals(20, Wave29Rules.d20(n -> 99), "a roller that overshoots is clamped");
        assertEquals(3, Wave29Rules.dice(n -> 0, 3, 6));
        assertEquals(18, Wave29Rules.dice(n -> n - 1, 3, 6));
    }

    @Test
    void portalIsATwoBlockDiscOpenTwoRounds() {
        assertEquals(240, Wave29Rules.PORTAL_TICKS);
        assertTrue(Wave29Rules.inPortal(0, 0, 0));
        assertTrue(Wave29Rules.inPortal(2.0, 0, 0), "the rim counts");
        assertFalse(Wave29Rules.inPortal(1.5, 0, 1.5), "past 2 blocks diagonally");
        assertFalse(Wave29Rules.inPortal(0, 3.0, 0), "too high above");
        assertFalse(Wave29Rules.inPortal(0, -1.5, 0), "too far below");
        assertTrue(Wave29Rules.ARRIVAL_GRACE > Wave29Rules.PORTAL_SWEEP, "an arrival is never carried again at the very next sweep");
    }

    @Test
    void vampireTableMatchesTheCard() {
        for (int r = 1; r <= 12; r++) assertSame(Wave29Rules.Vampire.CURSE, Wave29Rules.vampire(r));
        for (int r = 13; r <= 18; r++) assertSame(Wave29Rules.Vampire.CHILDREN, Wave29Rules.vampire(r));
        assertSame(Wave29Rules.Vampire.REGEN, Wave29Rules.vampire(19));
        assertSame(Wave29Rules.Vampire.REGEN, Wave29Rules.vampire(20));
        assertEquals(72000, Wave29Rules.CURSE_TICKS);
        assertEquals(120000, Wave29Rules.CHILDREN_TICKS);
    }

    @Test
    void packIsHalvedAndCappedAtSix() {
        assertEquals(1, Wave29Rules.packSize(false, n -> 0), "2d4 = 2 -> 1");
        assertEquals(4, Wave29Rules.packSize(false, n -> n - 1), "2d4 = 8 -> 4");
        assertEquals(2, Wave29Rules.packSize(true, n -> 0), "3d6 = 3 -> 2");
        assertEquals(6, Wave29Rules.packSize(true, n -> n - 1), "3d6 = 18 -> 9, capped at 6");
        assertEquals(120, Wave29Rules.packDelay(1));
        assertEquals(480, Wave29Rules.packDelay(4));
        assertEquals(480, Wave29Rules.packDelay(9), "clamped to 1d4 rounds");
    }

    @Test
    void curseBurnsButNeverBelowTwoHearts() {
        assertTrue(Wave29Rules.burns(20f));
        assertTrue(Wave29Rules.burns(4.5f));
        assertFalse(Wave29Rules.burns(4f));
        assertFalse(Wave29Rules.burns(1f));
        assertTrue(Wave29Rules.sunlit(true, false, true));
        assertFalse(Wave29Rules.sunlit(true, true, true), "rain hides the sun");
        assertFalse(Wave29Rules.sunlit(false, false, true), "night");
        assertFalse(Wave29Rules.sunlit(true, false, false), "under a roof");
    }

    @Test
    void radiantPathsAreRecognised() {
        assertTrue(Wave29Rules.isRadiantPath("holy_damage"));
        assertTrue(Wave29Rules.isRadiantPath("light_elemental"));
        assertTrue(Wave29Rules.isRadiantPath("sacred_flame"));
        assertFalse(Wave29Rules.isRadiantPath("lightning_bolt"), "lightning is not radiant");
        assertFalse(Wave29Rules.isRadiantPath("on_fire"));
        assertFalse(Wave29Rules.isRadiantPath(null));
    }

    @Test
    void wisdomSaveIsADifficultyClassOfFifteen() {
        assertTrue(Wave29Rules.saves(20, -10), "a natural 20 always saves");
        assertFalse(Wave29Rules.saves(1, 30), "a natural 1 never does");
        assertFalse(Wave29Rules.saves(13, 1));
        assertTrue(Wave29Rules.saves(13, 2), "13+2 meets the DC");
        assertEquals(2, Wave29Rules.saveBonus(20, 0), "a player: one per ten health");
        assertEquals(5, Wave29Rules.saveBonus(20, 1.0), "plus three for a full pool of magicules");
        assertEquals(10, Wave29Rules.saveBonus(400, 0), "capped at ten");
        assertEquals(6000, Wave29Rules.SLEEP_TICKS);
    }

    @Test
    void spiderLastsOneToFourHoursAndClimbsWalls() {
        assertEquals(6000, Wave29Rules.spiderTicks(1));
        assertEquals(24000, Wave29Rules.spiderTicks(4));
        assertEquals(24000, Wave29Rules.spiderTicks(9), "clamped to 1d4");
        assertTrue(Double.isNaN(Wave29Rules.climb(false, false)));
        assertEquals(Wave29Rules.CLIMB, Wave29Rules.climb(true, false));
        assertEquals(0.0, Wave29Rules.climb(true, true), "sneaking clings in place");
        assertTrue(Wave29Rules.SPIDER_SCALE < 0 && Wave29Rules.SPIDER_SCALE > -0.5, "a low long body, never so small it cannot see");
    }
}
