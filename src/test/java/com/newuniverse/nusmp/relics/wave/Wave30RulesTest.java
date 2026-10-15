package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 30: the set checks, the dice, the damage guards and the clocks of the special wave. */
class Wave30RulesTest {
    // ------------------------------------------------------------ sets
    @Test
    void vecnaSetNeedsBothHalves() {
        assertTrue(Wave30Rules.vecnaSet(true, true));
        assertFalse(Wave30Rules.vecnaSet(true, false));
        assertFalse(Wave30Rules.vecnaSet(false, true));
        assertFalse(Wave30Rules.vecnaSet(false, false));
    }

    @Test
    void stonesAreCountedOnceEachAndOnlyTheSix() {
        assertEquals(0, Wave30Rules.stoneCount(List.of()));
        assertEquals(0, Wave30Rules.stoneCount(Set.of("the_infinity_gauntlet", "paradox")));
        assertEquals(2, Wave30Rules.stoneCount(Set.of(Wave30Tune.ID_SPACE, Wave30Tune.ID_SOUL, "paradox")));
        assertEquals(6, Wave30Rules.stoneCount(List.of(Wave30Tune.STONES)));
        assertTrue(Wave30Rules.allStones(List.of(Wave30Tune.STONES)));
        assertFalse(Wave30Rules.allStones(List.of(Wave30Tune.ID_SPACE)));
        assertEquals(6, Wave30Tune.STONES.length);
        assertEquals(new java.util.HashSet<>(List.of(Wave30Tune.STONES)).size(), 6);
    }

    @Test
    void theGauntletBoostGrowsWithTheStonesAndNeedsTheGauntlet() {
        assertEquals(1.0, Wave30Rules.gauntletBoost(false, 6), 1e-9);
        assertEquals(1.0, Wave30Rules.gauntletBoost(true, 0), 1e-9);
        assertEquals(1.12, Wave30Rules.gauntletBoost(true, 1), 1e-9);
        assertEquals(1.72, Wave30Rules.gauntletBoost(true, 6), 1e-9);
        assertEquals(1.72, Wave30Rules.gauntletBoost(true, 40), 1e-9, "never more than six count");
        assertEquals(1.0, Wave30Rules.gauntletBoost(true, -3), 1e-9);
    }

    @Test
    void theSnapAndTheGreaterWishNeedTheGauntletAndAllSix() {
        assertTrue(Wave30Rules.snapReady(true, 6));
        assertFalse(Wave30Rules.snapReady(true, 5));
        assertFalse(Wave30Rules.snapReady(false, 6));
    }

    @Test
    void stoneIndexFollowsTheKnuckleOrder() {
        assertEquals(0, Wave30Rules.stoneIndex(Wave30Tune.ID_SPACE));
        assertEquals(5, Wave30Rules.stoneIndex(Wave30Tune.ID_SOUL));
        assertEquals(-1, Wave30Rules.stoneIndex("paradox"));
        assertEquals(6, Wave30Rules.stoneNames().size());
    }

    // ------------------------------------------------------------ dice
    @Test
    void savingThrowsHaveNaturalTwentyAndOne() {
        assertTrue(Wave30Rules.saves(20, -5, 30), "a natural 20 always holds");
        assertFalse(Wave30Rules.saves(1, 15, 5), "a natural 1 always fails");
        assertTrue(Wave30Rules.saves(17, 3, 20));
        assertFalse(Wave30Rules.saves(16, 3, 20));
        assertTrue(Wave30Rules.saves(10, 0, 10));
        int holds = 0;
        for (int d = 1; d <= 20; d++) if (Wave30Rules.saves(d, 0, 20)) holds++;
        assertEquals(1, holds, "DC 20 with no bonus holds only on a natural 20");
        holds = 0;
        for (int d = 1; d <= 20; d++) if (Wave30Rules.saves(d, 3, 20)) holds++;
        assertEquals(4, holds);
    }

    @Test
    void hungerAndWhisperAreFivePercentAndTwentyPercent() {
        int hunger = 0, whisper = 0;
        for (int i = 0; i < 1000; i++) {
            double r = i / 1000.0;
            if (Wave30Rules.hungerStrikes(r)) hunger++;
            if (Wave30Rules.whispers(r)) whisper++;
        }
        assertEquals(50, hunger);
        assertEquals(200, whisper);
        assertFalse(Wave30Rules.hungerStrikes(0.05));
        assertTrue(Wave30Rules.hungerStrikes(0.0));
    }

    @Test
    void theMadnessTableFollowsTheCard() {
        assertEquals(Wave30Rules.Madness.PRONE, Wave30Rules.madness(0));
        assertEquals(Wave30Rules.Madness.SHORT, Wave30Rules.madness(1));
        assertEquals(Wave30Rules.Madness.SHORT, Wave30Rules.madness(49));
        assertEquals(Wave30Rules.Madness.LONG, Wave30Rules.madness(50));
        assertEquals(Wave30Rules.Madness.LONG, Wave30Rules.madness(99));
        assertEquals(Wave30Rules.Madness.INDEFINITE, Wave30Rules.madness(100));
    }

    @Test
    void madnessLastsAreBounded() {
        assertEquals(60, Wave30Rules.madnessTicks(Wave30Rules.Madness.PRONE, 5));
        assertEquals(200, Wave30Rules.madnessTicks(Wave30Rules.Madness.SHORT, 1));
        assertEquals(2000, Wave30Rules.madnessTicks(Wave30Rules.Madness.SHORT, 10));
        assertEquals(2000, Wave30Rules.madnessTicks(Wave30Rules.Madness.SHORT, 99), "a d10 above ten is clamped");
        assertEquals(6000, Wave30Rules.madnessTicks(Wave30Rules.Madness.LONG, 3));
        assertEquals(12000, Wave30Rules.madnessTicks(Wave30Rules.Madness.INDEFINITE, 3), "never forever");
    }

    @Test
    void paradoxDice() {
        assertEquals(6, Wave30Rules.paradoxRegain(1, 1));
        assertEquals(20, Wave30Rules.paradoxRegain(8, 8));
        assertEquals(20, Wave30Rules.paradoxRegain(80, 80), "dice are clamped");
        assertEquals(2f, Wave30Rules.paradoxExtra(1, 1), 0f);
        assertEquals(8f, Wave30Rules.paradoxExtra(4, 4), 0f);
        assertTrue(Wave30Rules.paradoxCrit(19));
        assertTrue(Wave30Rules.paradoxCrit(20));
        assertFalse(Wave30Rules.paradoxCrit(18));
    }

    // ------------------------------------------------------------ damage guards
    @Test
    void aPlayerIsNeverTakenBelowAHeart() {
        assertEquals(5f, Wave30Rules.playerHit(5f, 20f, 20f, 0.45, 2f), 1e-6f);
        assertEquals(9f, Wave30Rules.playerHit(40f, 20f, 20f, 0.45, 2f), 1e-6f, "the cap: 45% of 20");
        assertEquals(3f, Wave30Rules.playerHit(40f, 20f, 5f, 0.45, 2f), 1e-6f, "five health: three may be taken");
        assertEquals(0f, Wave30Rules.playerHit(40f, 20f, 2f, 0.45, 2f), 1e-6f, "a heart: nothing");
        assertEquals(0f, Wave30Rules.playerHit(40f, 20f, 1f, 0.45, 2f), 1e-6f);
        for (float h = 0.5f; h <= 40f; h += 0.5f)
            for (float a = 0f; a <= 100f; a += 5f) {
                float taken = Wave30Rules.playerHit(a, 40f, h, 0.45, 2f);
                assertTrue(taken >= 0f);
                assertTrue(h - taken >= Math.min(h, 2f) - 1e-4f, "health " + h + " amount " + a);
                assertTrue(taken <= 40f * 0.45f + 1e-4f);
            }
    }

    @Test
    void jellyIsThirtyPercentTenOnABossAndSafeOnAPlayer() {
        assertEquals(30f, Wave30Rules.jelly(100f, 100f, false, false), 1e-4f);
        assertEquals(10f, Wave30Rules.jelly(100f, 100f, false, true), 1e-4f);
        assertEquals(6f, Wave30Rules.jelly(20f, 20f, true, false), 1e-4f, "30% of 20 on a player");
        assertEquals(0f, Wave30Rules.jelly(20f, 2f, true, false), 1e-4f);
        assertTrue(20f - Wave30Rules.jelly(20f, 4f, true, false) >= 2f);
    }

    @Test
    void theSnapTakesHalfAndNeverTheWhole() {
        assertEquals(50f, Wave30Rules.snap(100f, 100f, false), 1e-4f);
        assertEquals(10f, Wave30Rules.snap(100f, 100f, true), 1e-4f);
        assertEquals(0f, Wave30Rules.snap(100f, 1f, false), 1e-4f);
        assertTrue(Wave30Rules.snap(300f, 7f, true) <= 6f);
        for (float h = 1f; h < 80f; h += 3f) assertTrue(h - Wave30Rules.snap(80f, h, false) >= 1f);
    }

    @Test
    void summonHitsAreNeverLethal() {
        assertEquals(0f, Wave30Rules.summonHit(9f, 20f, 2f), 1e-6f);
        assertEquals(6f, Wave30Rules.summonHit(60f, 20f, 20f), 1e-6f, "30% of 20");
        assertEquals(1f, Wave30Rules.summonHit(8f, 20f, 3f), 1e-6f);
    }

    // ------------------------------------------------------------ clocks
    @Test
    void theWishClockIsThirtyRealDays() {
        long day = 24L * 60 * 60 * 1000;
        assertTrue(Wave30Rules.wishReady(0L, 5L));
        assertFalse(Wave30Rules.wishReady(1000L, 1000L + 29 * day));
        assertTrue(Wave30Rules.wishReady(1000L, 1000L + 30 * day));
        assertTrue(Wave30Rules.wishReady(1000L, 500L), "a clock set back does not lock the player out");
        assertEquals(0, Wave30Rules.wishDaysLeft(0L, 5L));
        assertEquals(30, Wave30Rules.wishDaysLeft(1000L, 1000L));
        assertEquals(1, Wave30Rules.wishDaysLeft(1000L, 1000L + 30 * day - 5));
        assertEquals(10, Wave30Rules.wishDaysLeft(1000L, 1000L + 20 * day));
    }

    @Test
    void surpriseGuardAndSoulReviveClocks() {
        assertTrue(Wave30Rules.surpriseReady(100, 100));
        assertFalse(Wave30Rules.surpriseReady(99, 100));
        assertTrue(Wave30Rules.soulReviveOk(1, 3, 4));
        assertFalse(Wave30Rules.soulReviveOk(0, 3, 4));
        assertFalse(Wave30Rules.soulReviveOk(5, 4, 4));
        assertEquals(10f, Wave30Rules.revivedHealth(20f), 1e-6f);
        assertEquals(1f, Wave30Rules.revivedHealth(1f), 1e-6f);
    }

    @Test
    void theUndoHealsBackOnlyARecentBlow() {
        assertEquals(6f, Wave30Rules.undoAmount(6f, 10, 10f, 20f), 1e-6f);
        assertEquals(3f, Wave30Rules.undoAmount(6f, 10, 17f, 20f), 1e-6f, "never past the maximum");
        assertEquals(0f, Wave30Rules.undoAmount(6f, 200, 10f, 20f), 1e-6f, "too long ago");
        assertEquals(0f, Wave30Rules.undoAmount(0f, 5, 10f, 20f), 1e-6f);
        assertEquals(0f, Wave30Rules.undoAmount(6f, -1, 10f, 20f), 1e-6f);
    }

    @Test
    void theWishMenusCycle() {
        assertEquals(4, Wave30Rules.VecnaWish.values().length);
        assertEquals(5, Wave30Rules.GreatWish.values().length);
        assertEquals(1, Wave30Rules.nextIndex(0, 4));
        assertEquals(0, Wave30Rules.nextIndex(3, 4));
        assertEquals(0, Wave30Rules.nextIndex(7, 0));
        assertEquals(0, Wave30Rules.nextIndex(4, 5));
        assertEquals(0, Wave30Rules.nextIndex(-1, 5), "a stray negative index still lands inside the menu");
    }

    @Test
    void tuneNumbersAreSane() {
        assertTrue(Wave30Tune.HUNGER_CHANCE == 0.05);
        assertEquals(30L * 24 * 3600 * 1000, Wave30Tune.WISH_COOLDOWN_MILLIS);
        assertTrue(Wave30Tune.LEAVE_HEALTH >= 2f, "a heart is left");
        assertTrue(Wave30Tune.STAR_CAP <= 3);
        assertEquals(22, Wave30DeckMath.SIZE);
    }
}
