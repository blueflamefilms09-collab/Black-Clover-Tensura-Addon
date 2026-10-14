package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of the wave 16 charms: the grenade's d4+1, the sigil's sunset, the perfume's save, Po'eur's DC, the coin's streak, the wallet's gold (docs/doc_wave16.md). */
class Wave16RulesTest {
    @Test
    void theGrenadeRollsTwoToFiveAndAnUnworthyThrowerAlwaysRollsFive() {
        assertEquals(Wave16Rules.Grenade.DUD, Wave16Rules.grenade(2));
        assertEquals(Wave16Rules.Grenade.BLAST, Wave16Rules.grenade(3));
        assertEquals(Wave16Rules.Grenade.NOTHING, Wave16Rules.grenade(4));
        assertEquals(Wave16Rules.Grenade.BACKFIRE, Wave16Rules.grenade(5));
        assertEquals(Wave16Rules.Grenade.DUD, Wave16Rules.grenade(-9), "out of range is clamped");
        assertEquals(Wave16Rules.Grenade.BACKFIRE, Wave16Rules.grenade(99));
        for (int face = 0; face < 4; face++) {
            int f = face;
            assertEquals(2 + face, Wave16Rules.grenadeRoll(n -> f, false));
            assertEquals(5, Wave16Rules.grenadeRoll(n -> f, true));
        }
    }

    @Test
    void theGrenadesOwnBlastNeverKillsTheThrower() {
        assertEquals(33f, Wave16Rules.backfireSelf(100f));
        assertEquals(19f, Wave16Rules.backfireSelf(20f), "a full-health player keeps a heart");
        assertEquals(0f, Wave16Rules.backfireSelf(1f));
        assertEquals(0f, Wave16Rules.backfireSelf(0.2f));
    }

    @Test
    void theSigilsChargesAnswerOnlyWhileTheSunIsUp() {
        assertTrue(Wave16Rules.sunUp(0));
        assertTrue(Wave16Rules.sunUp(11999));
        assertFalse(Wave16Rules.sunUp(12000));
        assertFalse(Wave16Rules.sunUp(23999));
        assertTrue(Wave16Rules.sunUp(24000 * 5L + 100), "the day number does not matter");
        assertEquals(1, Wave16Rules.reprisalCost(1f));
        assertEquals(2, Wave16Rules.reprisalCost(9f));
        assertEquals(3, Wave16Rules.reprisalCost(40f));
        assertEquals(6f, Wave16Rules.reprisalDamage(1));
        assertEquals(10f, Wave16Rules.reprisalDamage(3));
        assertEquals(3, Wave16Rules.sigilGain(n -> 0), "1d8+2: the lowest is 3");
        assertEquals(10, Wave16Rules.sigilGain(n -> n - 1), "and the highest 10");
    }

    @Test
    void thePerfumeSaveGrowsWithHealthAndBossesCannotBeHeld() {
        assertEquals(0, Wave16Rules.saveBonus(20f));
        assertEquals(4, Wave16Rules.saveBonus(100f));
        assertEquals(8, Wave16Rules.saveBonus(1000f));
        assertFalse(Wave16Rules.saved(20f, 16), "DC 17 against a bonus of 0 needs a 17");
        assertTrue(Wave16Rules.saved(20f, 17));
        assertTrue(Wave16Rules.saved(100f, 13));
        assertFalse(Wave16Rules.saved(100f, 12));
        assertTrue(Wave16Rules.saved(400f, 1));
    }

    @Test
    void poeursDcRisesWithTheSpellAndSmallSpellsDoNotCount() {
        assertEquals(0, Wave16Rules.poeurDc(4.9));
        assertEquals(12, Wave16Rules.poeurDc(5));
        assertEquals(14, Wave16Rules.poeurDc(10));
        assertEquals(16, Wave16Rules.poeurDc(15));
        assertEquals(18, Wave16Rules.poeurDc(20));
        assertEquals(20, Wave16Rules.poeurDc(25));
        assertEquals(20, Wave16Rules.poeurDc(90));
        assertTrue(Wave16Rules.freeCast(5, 8, 4));
        assertFalse(Wave16Rules.freeCast(5, 7, 4));
        assertFalse(Wave16Rules.freeCast(2, 20, 4), "a cantrip is never refunded");
        assertTrue(Wave16Rules.freeCast(60, 16, 4));
        assertFalse(Wave16Rules.freeCast(60, 15, 4));
    }

    @Test
    void theCoinCracksOnlyAfterThreeTailsInARowAndANaturalOne() {
        assertTrue(Wave16Rules.heads(0));
        assertFalse(Wave16Rules.heads(1));
        assertEquals(1, Wave16Rules.streakAfter(false, 0, true));
        assertEquals(3, Wave16Rules.streakAfter(false, 2, true));
        assertEquals(0, Wave16Rules.streakAfter(true, 2, true), "a head ends the run");
        assertEquals(1, Wave16Rules.streakAfter(false, 2, false), "a new day starts again");
        assertFalse(Wave16Rules.cracks(2, 1));
        assertFalse(Wave16Rules.cracks(3, 2));
        assertTrue(Wave16Rules.cracks(3, 1));
        assertTrue(Wave16Rules.cracks(5, 1));
    }

    @Test
    void theIdolKeepsTheHigherOfTwoRolls() {
        int[] seq = {1, 3};
        int[] at = {0};
        assertEquals(3, Wave16Rules.idolLevel(n -> seq[at[0]++ % 2]));
        assertEquals(0, Wave16Rules.idolLevel(n -> 0));
        assertEquals(3, Wave16Rules.idolLevel(n -> 99), "out of range is clamped");
    }

    @Test
    void theWildMagicTableHasTenRowsAndAWish() {
        assertEquals(0, Wave16Rules.wildRow(1));
        assertEquals(0, Wave16Rules.wildRow(10));
        assertEquals(1, Wave16Rules.wildRow(11));
        assertEquals(9, Wave16Rules.wildRow(99));
        assertEquals(-1, Wave16Rules.wildRow(100));
    }

    @Test
    void theWalletPaysChallengeRatingTimesTwoDiceAndNeverNothing() {
        assertEquals(1.0, Wave16Rules.challenge(15f), 1.0e-9);
        assertEquals(0.25, Wave16Rules.challenge(1f), 1.0e-9);
        assertEquals(14, Wave16Rules.gp(15f, 3, 4) * 2, "CR 1 x 7 = 7 gp");
        assertEquals(1, Wave16Rules.gp(1f, 1, 1), "at least 1");
        assertEquals(600, Wave16Rules.gp(5000f, 6, 6), "at most 600");
        assertEquals(3, Wave16Rules.goldCoins(350));
        assertEquals(50, Wave16Rules.silverCoins(350));
        assertEquals(0, Wave16Rules.goldCoins(-5));
    }

    @Test
    void theSatchelsObjectIsFreeOutOrMissing() {
        assertEquals(Wave16Rules.Slot.FREE, Wave16Rules.satchelSlot(0, 0, 100, 5));
        assertEquals(Wave16Rules.Slot.OUT, Wave16Rules.satchelSlot(500, 0, 100, 5));
        assertEquals(Wave16Rules.Slot.FREE, Wave16Rules.satchelSlot(100, 0, 100, 5), "the day is up");
        assertEquals(Wave16Rules.Slot.MISSING, Wave16Rules.satchelSlot(0, 8, 100, 5));
        assertEquals(Wave16Rules.Slot.MISSING, Wave16Rules.satchelSlot(500, 8, 100, 5), "missing wins over out");
        assertEquals(Wave16Rules.Slot.FREE, Wave16Rules.satchelSlot(0, 8, 100, 8), "the third dawn returns it");
    }

    @Test
    void theVinesLetGoAfterThreeDays() {
        assertFalse(Wave16Rules.vinesLetGo(0, 999999));
        assertFalse(Wave16Rules.vinesLetGo(1000, 1000 + Wave16Tune.DRYAD_FREE_AFTER));
        assertTrue(Wave16Rules.vinesLetGo(1000, 1001 + Wave16Tune.DRYAD_FREE_AFTER));
    }

    @Test
    void everyIdIsAValidRegistryPathAndAllAreDifferent() {
        String[] ids = {Wave16Tune.ID_SIGIL, Wave16Tune.ID_CLASP, Wave16Tune.ID_POEUR, Wave16Tune.ID_GRENADE, Wave16Tune.ID_DOLL, Wave16Tune.ID_PERFUME,
                Wave16Tune.ID_SATCHEL, Wave16Tune.ID_SUNSCREEN, Wave16Tune.ID_GOD, Wave16Tune.ID_BATS, Wave16Tune.ID_BLUE, Wave16Tune.ID_DRYAD,
                Wave16Tune.ID_WEAVER, Wave16Tune.ID_COIN, Wave16Tune.ID_WHITE, Wave16Tune.ID_IDOL, Wave16Tune.ID_WALLET};
        assertEquals(17, ids.length);
        assertEquals(17, java.util.Set.of(ids).size());
        for (String id : ids) assertTrue(id.matches("[a-z0-9_]+"), id);
    }
}
