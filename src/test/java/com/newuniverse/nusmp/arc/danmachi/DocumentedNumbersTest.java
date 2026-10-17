package com.newuniverse.nusmp.arc.danmachi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Why: the numbers of the counters are written in three places that the player reads (the docs in {@code docs/arc}, the hints the friends say, the notices in
 * the chat), and the other tests compare the rules with their own constants, so a constant that was changed by a slip would change both sides and no test would
 * notice. This test spells the numbers out as literals, once, so changing one of them is a decision that has to touch this file and the docs together.
 */
class DocumentedNumbersTest {
    @Test
    void theRibsBlockSeventyPercentAndTurnInFourteenTicks() {
        assertEquals(65.0, RibGuard.HALF_ARC_DEG, 0.0);
        assertEquals(100.0, RibGuard.CLOSED_HALF_ARC_DEG, 0.0);
        assertEquals(0.30f, RibGuard.BLOCK_FACTOR, 0.0f);
        assertEquals(0.20f, RibGuard.CLOSED_FACTOR, 0.0f);
        assertEquals(1.6f, RibGuard.STAGGER_FACTOR, 0.0f);
        assertEquals(70, RibGuard.STAGGER_TICKS);
        assertEquals(200, RibGuard.CRACK_MEMORY);
        assertEquals(14, RibGuard.reaction(20));
        assertEquals(6, RibGuard.reaction(100));
        assertEquals(3, RibGuard.crackLimit(80));
        assertEquals(4, RibGuard.crackLimit(100));
    }

    @Test
    void theBlackHideHalvesPlainBlowsAndBreaksUnderThreeHeavyOnesInSixSeconds() {
        assertEquals(0.5f, HideState.PLAIN_FACTOR, 0.0f);
        assertEquals(0.015f, HideState.PLATE_FRACTION, 0.0f);
        assertEquals(3, HideState.PLATES);
        assertEquals(120, HideState.PLATE_WINDOW);
        assertEquals(160, HideState.BROKEN_TICKS);
    }

    @Test
    void theRedHeadFallsToSevenPercentOfCountedBlows() {
        assertEquals(0.07, HeadCut.CUT_FRACTION, 0.0);
        assertEquals(40, HeadCut.LINGER_TICKS);
        assertEquals(80, HeadCut.FORCE_CUT_POWER);
        assertEquals(1.0, HeadCut.weight(HeadCut.Kind.PHYSICAL), 0.0);
        assertEquals(2.0, HeadCut.weight(HeadCut.Kind.ANTI), 0.0);
        assertEquals(0.0, HeadCut.weight(HeadCut.Kind.MAGIC), 0.0);
    }

    @Test
    void theBullIsStunnedAndTakesThirtyPercentMoreWhenItRamsAWall() {
        assertEquals(1.30f, ChargeMath.CRASH_BONUS, 0.0f);
        assertEquals(12, ChargeMath.DASH_TELL);
        assertEquals(60, ChargeMath.crashStun(20));
        assertEquals(36, ChargeMath.crashStun(100));
        assertEquals(0, ChargeMath.rampageDashes(40));
        assertEquals(3, ChargeMath.rampageDashes(60));
        assertEquals(5, ChargeMath.rampageDashes(100));
    }

    @Test
    void theHuntMarksTheWeakestAndAnyoneCanStealTheMark() {
        assertEquals(3, PreyBook.TAUNT_HITS);
        assertEquals(60, PreyBook.TAUNT_WINDOW);
        assertEquals(160, PreyBook.STEAL_TICKS);
        assertEquals(200, PreyBook.REPICK_TICKS);
        assertEquals(0.05, PreyBook.TAUNT_BIG, 0.0);
        assertEquals(1.15f, PreyBook.PREY_BONUS, 0.0f);
    }

    @Test
    void theDungeonLevelDefaultsAreTheDocumentedOnes() {
        assertEquals(1000.0, DungeonLevel.DEFAULT_BASE_EP, 0.0);
        assertEquals(0.5, DungeonLevel.DEFAULT_DECADES, 0.0);
        assertEquals(0.10, DungeonLevel.DEFAULT_DAMAGE_PER_STEP, 0.0);
        assertEquals(0.03, DungeonLevel.DEFAULT_SPEED_PER_STEP, 0.0);
        assertEquals(0, DungeonLevel.DEFAULT_MARGIN);
        assertEquals(3, DungeonLevel.DEFAULT_MAX_STEPS);
        assertEquals(1, DungeonLevel.MIN_LEVEL);
        assertEquals(9, DungeonLevel.MAX_LEVEL);
        assertEquals(1, DungeonLevel.levelOfEp(999));
        assertEquals(2, DungeonLevel.levelOfEp(3163));
        assertEquals(3, DungeonLevel.levelOfEp(10_000));
        assertEquals(5, DungeonLevel.levelOfEp(100_000));
        assertEquals(7, DungeonLevel.levelOfEp(1_000_000));
        assertEquals(9, DungeonLevel.levelOfEp(10_000_000));
    }

    @Test
    void theStatsOfTheFourMonstersAreTheDocumentedOnes() {
        assertEquals(1000.0, DanMachiRoster.MINOTAUR.health(), 0.0);
        assertEquals(1800.0, DanMachiRoster.BLACK_GOLIATH.health(), 0.0);
        assertEquals(1900.0, DanMachiRoster.AMPHISBAENA.health(), 0.0);
        assertEquals(2600.0, DanMachiRoster.UDAEUS.health(), 0.0);
        assertEquals(2, DanMachiRoster.BLACK_GOLIATH.tier());
        assertEquals(3, DanMachiRoster.UDAEUS.tier());
    }

    @Test
    void theLevelUpStepsAreTheOnesTheDocsTabulate() {
        int[][] minotaur = {{1, 0}, {2, 1}, {3, 1}, {4, 2}, {5, 2}, {6, 3}, {9, 3}};
        for (int[] row : minotaur) assertEquals(row[1], DungeonLevel.steps(row[0], 2, 0, 3), "Minotaur (Level 2) against a party of Level " + row[0]);
        int[][] giant = {{4, 0}, {5, 1}, {6, 1}, {7, 2}, {9, 3}};
        for (int[] row : giant) assertEquals(row[1], DungeonLevel.steps(row[0], 5, 0, 3), "Level 5 monster against a party of Level " + row[0]);
        int[][] udaeus = {{5, 0}, {6, 1}, {7, 1}, {8, 2}, {9, 2}};
        for (int[] row : udaeus) assertEquals(row[1], DungeonLevel.steps(row[0], 6, 0, 3), "Udaeus (Level 6) against a party of Level " + row[0]);
        assertEquals(0, DungeonLevel.steps(9, 2, 0, 0), "levelUp off: no steps whatever the party");
        assertEquals(0, DungeonLevel.steps(DungeonLevel.NO_PARTY, 2, 0, 3), "nobody near: nothing to measure");
        assertEquals(1, DungeonLevel.steps(1, 2, -1, 3), "a margin of -1 lets a Level 1 party trigger a Level 2 monster");
        assertEquals(3, DungeonLevel.bossLevelAfter(2, 1));
        assertEquals(9, DungeonLevel.bossLevelAfter(9, 3), "never above the top");
        assertEquals(1.2, DungeonLevel.damageFactor(2, 0.10), 1e-9);
        assertEquals(1.06, DungeonLevel.speedFactor(2, 0.03), 1e-9);
    }
}
