package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 12: the ten weapons' pure rules (jaw charges, falling tree, daily missiles, crystal arm and moon, hooks, spores, blast). */
class Wave12RulesTest {
    @Test
    void jawbladeDamageGrowsFrom3d6To6d6AndPlayersAreProtected() {
        assertEquals(1.0, Wave12Rules.jawMultiplier(0), 1e-9);
        assertEquals(4.0 / 3.0, Wave12Rules.jawMultiplier(1), 1e-9);
        assertEquals(5.0 / 3.0, Wave12Rules.jawMultiplier(2), 1e-9);
        assertEquals(2.0, Wave12Rules.jawMultiplier(3), 1e-9);
        assertEquals(2.0, Wave12Rules.jawMultiplier(9), 1e-9, "never above three charges");
        assertEquals(1.0, Wave12Rules.jawMultiplier(-4), 1e-9);
        assertEquals(18f, Wave12Rules.boundedBonus(9f, 18f, 20f, false, Wave12Rules.PLAYER_BONUS_SHARE), 1e-6, "a mob takes the whole blow");
        assertEquals(12f, Wave12Rules.boundedBonus(9f, 18f, 20f, true, Wave12Rules.PLAYER_BONUS_SHARE), 1e-6, "a 20 HP player takes at most 3 extra");
        assertEquals(9f, Wave12Rules.boundedBonus(9f, 9f, 20f, true, Wave12Rules.PLAYER_BONUS_SHARE), 1e-6);
    }

    @Test
    void aFallingTreeHitsHarderTheFartherFromTheStump() {
        assertEquals(7f, Wave12Rules.treeDamage(0), 1e-6);
        assertEquals(7f, Wave12Rules.treeDamage(5.9), 1e-6);
        assertEquals(10.5f, Wave12Rules.treeDamage(6), 1e-6);
        assertEquals(14f, Wave12Rules.treeDamage(12.5), 1e-6);
        assertEquals(24f, Wave12Rules.treeDamage(500), 1e-6, "capped");
        assertEquals(7f, Wave12Rules.treeDamage(-3), 1e-6);
        assertTrue(Wave12Rules.looksLikeTree(5, 3));
        assertFalse(Wave12Rules.looksLikeTree(5, 0), "a log cabin has no natural leaves");
        assertFalse(Wave12Rules.looksLikeTree(1, 9), "a single log is not a tree");
    }

    @Test
    void theWarmageHoldsFiveMissilesAndRefillsAtDawn() {
        assertEquals(5, Wave12Rules.missilesLeft(0, 0, 7, false), "nothing stored: a full sword");
        assertEquals(2, Wave12Rules.missilesLeft(2, 7, 7, true), "same day");
        assertEquals(5, Wave12Rules.missilesLeft(0, 6, 7, true), "a new day refills");
        assertEquals(5, Wave12Rules.missilesLeft(99, 7, 7, true), "never above five");
        assertEquals(0, Wave12Rules.missilesLeft(-3, 7, 7, true));
        assertEquals(2f, Wave12Rules.missileDamage(1), 1e-6);
        assertEquals(5f, Wave12Rules.missileDamage(4), 1e-6);
        assertEquals(5f, Wave12Rules.missileDamage(40), 1e-6);
        assertArrayEquals(new int[]{2, 2, 1}, Wave12Rules.spread(5, 3));
        assertArrayEquals(new int[]{5}, Wave12Rules.spread(5, 1));
        assertArrayEquals(new int[0], Wave12Rules.spread(5, 0));
    }

    @Test
    void theCrystalArmWearsOutAndBreaksWithoutKilling() {
        float hp = Wave12Rules.CRYSTAL_HP;
        int blows = 0;
        while (hp > 0f) {
            hp = Wave12Rules.crystalAfterBlow(hp);
            blows++;
            assertTrue(blows < 100);
        }
        assertEquals(25, blows);
        assertEquals(2f, Wave12Rules.shatterBackfire(1, 1), 1e-6);
        assertEquals(8f, Wave12Rules.shatterBackfire(4, 4), 1e-6);
        assertEquals(4f, Wave12Rules.safeBackfire(8f, 5f), 1e-6, "leaves one point of health");
        assertEquals(0f, Wave12Rules.safeBackfire(8f, 1f), 1e-6);
        assertEquals(3f, Wave12Rules.safeBackfire(3f, 20f), 1e-6);
    }

    @Test
    void theMoonGivesOneChargePerNight() {
        assertFalse(Wave12Rules.moonlit(6000));
        assertTrue(Wave12Rules.moonlit(18000));
        assertTrue(Wave12Rules.moonlit(24000L * 3 + 14000));
        assertFalse(Wave12Rules.moonlit(24000L * 3 + 2000));
        assertEquals(3, Wave12Rules.nightIndex(24000L * 3 + 14000));
        assertEquals(3, Wave12Rules.nightIndex(24000L * 4 + 2000), "the early morning belongs to the night before");
        assertEquals(4, Wave12Rules.nightIndex(24000L * 4 + 13500));
        assertTrue(Wave12Rules.regains(3, 2, 3));
        assertFalse(Wave12Rules.regains(3, 3, 3), "once per night");
        assertFalse(Wave12Rules.regains(4, 2, 3), "never above four");
    }

    @Test
    void theHooksTakeTwoLeggedAndWingedFoesOnly() {
        assertTrue(Wave12Rules.hookable(0.6, 1.95, false), "a zombie");
        assertTrue(Wave12Rules.hookable(0.9, 0.5, true), "a bat or a phantom");
        assertFalse(Wave12Rules.hookable(0.9, 1.4, false), "a cow");
        assertFalse(Wave12Rules.hookable(1.4, 0.9, false), "a spider");
        assertTrue(Wave12Rules.hookHolds(20f, false));
        assertFalse(Wave12Rules.hookHolds(300f, false));
        assertFalse(Wave12Rules.hookHolds(20f, true));
    }

    @Test
    void naturalOneAndTwentyAndTheBlast() {
        assertTrue(Wave12Rules.fumble(1));
        assertFalse(Wave12Rules.fumble(2));
        assertTrue(Wave12Rules.critical(20));
        assertFalse(Wave12Rules.critical(19));
        assertEquals(6f, Wave12Rules.blastDamage(0.5), 1e-6);
        assertEquals(6f, Wave12Rules.blastDamage(2.0), 1e-6);
        assertEquals(3f, Wave12Rules.blastDamage(3.5), 1e-6, "half for those who stand back");
        assertTrue(Wave12Rules.coldBlade(0));
        assertFalse(Wave12Rules.coldBlade(1));
        assertTrue(Wave12Rules.coldBlade(2));
        assertEquals(0.4f, Wave12Rules.EMPTY_BLOW, 1e-6);
    }
}
