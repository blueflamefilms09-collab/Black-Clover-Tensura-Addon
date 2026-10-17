package com.newuniverse.nusmp.arc.danmachi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Dungeon Level decides whether a boss changes at the Gates, so the Level of a fighter from their EP, the Level of a party and the steps a boss takes are
 * tested at their edges (the exact EP of a new Level, an empty party, a bad config value).
 */
class DungeonLevelTest {
    @Test
    void theDefaultScaleIsOneLevelPerHalfDecadeOfEp() {
        assertEquals(1, DungeonLevel.levelOfEp(0));
        assertEquals(1, DungeonLevel.levelOfEp(-5));
        assertEquals(1, DungeonLevel.levelOfEp(Double.NaN));
        assertEquals(1, DungeonLevel.levelOfEp(999));
        assertEquals(1, DungeonLevel.levelOfEp(3000));
        assertEquals(2, DungeonLevel.levelOfEp(3200));
        assertEquals(2, DungeonLevel.levelOfEp(9000));
        assertEquals(3, DungeonLevel.levelOfEp(10_000));
        assertEquals(3, DungeonLevel.levelOfEp(30_000));
        assertEquals(4, DungeonLevel.levelOfEp(32_000));
        assertEquals(5, DungeonLevel.levelOfEp(100_000));
        assertEquals(6, DungeonLevel.levelOfEp(320_000));
        assertEquals(7, DungeonLevel.levelOfEp(1_000_000));
        assertEquals(8, DungeonLevel.levelOfEp(3_300_000));
        assertEquals(9, DungeonLevel.levelOfEp(10_000_000));
        assertEquals(9, DungeonLevel.levelOfEp(1e15), "the top is Level 9");
    }

    @Test
    void theLevelNeverFallsAsEpRises() {
        int last = 0;
        for (double ep = 1; ep < 1e9; ep *= 1.07) {
            int level = DungeonLevel.levelOfEp(ep);
            assertTrue(level >= last, "monotone at " + ep);
            assertTrue(level >= DungeonLevel.MIN_LEVEL && level <= DungeonLevel.MAX_LEVEL);
            last = level;
        }
        assertEquals(DungeonLevel.MAX_LEVEL, last);
    }

    @Test
    void theScaleFollowsItsConfigAndSurvivesBadValues() {
        assertEquals(1, DungeonLevel.levelOfEp(9_000, 10_000, 1.0));
        assertEquals(1, DungeonLevel.levelOfEp(50_000, 10_000, 1.0), "below one full decade above the base");
        assertEquals(2, DungeonLevel.levelOfEp(100_000, 10_000, 1.0));
        assertEquals(3, DungeonLevel.levelOfEp(1_000_000, 10_000, 1.0));
        assertEquals(9, DungeonLevel.levelOfEp(1e30, 10_000, 1.0));
        // a zero or negative width must not divide by zero or make everyone Level 9 at once
        int bad = DungeonLevel.levelOfEp(5_000, 1000, 0.0);
        assertTrue(bad >= 1 && bad <= 9);
        assertEquals(9, DungeonLevel.levelOfEp(1e9, 1000, -3), "the width is floored at 0.05 decades, so a huge EP still caps at 9");
        assertEquals(4, DungeonLevel.levelOfEp(50, 0.0, 0.5), "a base below 1 is treated as 1, and 50 EP is 1.7 decades above it");
        assertEquals(DungeonLevel.levelOfEp(50, 1.0, 0.5), DungeonLevel.levelOfEp(50, -7.0, 0.5));
    }

    @Test
    void aPartyIsFoldedByTheChosenMode() {
        int[] lv = {1, 2, 2, 6};
        assertEquals(2, DungeonLevel.partyLevel(lv, DungeonLevel.Mode.MEAN), "mean rounds down: 11 / 4");
        assertEquals(2, DungeonLevel.partyLevel(lv, DungeonLevel.Mode.MEDIAN));
        assertEquals(6, DungeonLevel.partyLevel(lv, DungeonLevel.Mode.STRONGEST));
        assertEquals(1, DungeonLevel.partyLevel(lv, DungeonLevel.Mode.WEAKEST));
        assertEquals(5, DungeonLevel.partyLevel(new int[]{5}, DungeonLevel.Mode.MEAN));
        assertEquals(DungeonLevel.NO_PARTY, DungeonLevel.partyLevel(new int[0], DungeonLevel.Mode.MEAN));
        assertEquals(DungeonLevel.NO_PARTY, DungeonLevel.partyLevel(null, DungeonLevel.Mode.STRONGEST));
        assertEquals(2, DungeonLevel.partyLevel(lv, null), "no mode means the mean");
        // out of range inputs are pulled into 1..9 instead of poisoning the mean
        assertEquals(9, DungeonLevel.partyLevel(new int[]{40, 40}, DungeonLevel.Mode.MEAN));
        assertEquals(1, DungeonLevel.partyLevel(new int[]{-3, 0}, DungeonLevel.Mode.WEAKEST));
    }

    @Test
    void modeNamesParseAnyCaseAndTypoFallsBackToTheMean() {
        assertEquals(DungeonLevel.Mode.STRONGEST, DungeonLevel.Mode.parse("Strongest"));
        assertEquals(DungeonLevel.Mode.WEAKEST, DungeonLevel.Mode.parse(" weakest "));
        assertEquals(DungeonLevel.Mode.MEDIAN, DungeonLevel.Mode.parse("MEDIAN"));
        assertEquals(DungeonLevel.Mode.MEAN, DungeonLevel.Mode.parse("average"));
        assertEquals(DungeonLevel.Mode.MEAN, DungeonLevel.Mode.parse(null));
        assertEquals("strongest", DungeonLevel.Mode.STRONGEST.id());
    }

    @Test
    void aBossLevelsUpOnlyWhenThePartyHasReachedItsLevelAndByMoreWhenTheyAreFurtherAhead() {
        assertEquals(0, DungeonLevel.steps(1, 2, 0, 3), "a weaker party meets the boss as it is");
        assertEquals(0, DungeonLevel.steps(DungeonLevel.NO_PARTY, 2, 0, 3), "nobody to measure");
        assertEquals(1, DungeonLevel.steps(2, 2, 0, 3), "level with the boss: one step");
        assertEquals(1, DungeonLevel.steps(3, 2, 0, 3));
        assertEquals(2, DungeonLevel.steps(4, 2, 0, 3));
        assertEquals(2, DungeonLevel.steps(5, 2, 0, 3));
        assertEquals(3, DungeonLevel.steps(6, 2, 0, 3));
        assertEquals(3, DungeonLevel.steps(9, 2, 0, 3), "never more than the cap");
        assertEquals(1, DungeonLevel.steps(9, 2, 0, 1));
        assertEquals(0, DungeonLevel.steps(9, 2, 0, 0), "a cap of 0 switches the Level-Up off");
        assertEquals(0, DungeonLevel.steps(9, 2, 0, -2));
        // the margin moves the line
        assertEquals(0, DungeonLevel.steps(2, 2, 2, 3));
        assertEquals(1, DungeonLevel.steps(4, 2, 2, 3));
        assertEquals(1, DungeonLevel.steps(1, 2, -1, 3), "a negative margin lets a weaker party trigger it");
    }

    @Test
    void stepsGrowMonotonicallyWithThePartyLevel() {
        for (int boss = 1; boss <= 9; boss++) {
            int last = 0;
            for (int party = 0; party <= 9; party++) {
                int s = DungeonLevel.steps(party, boss, 0, 3);
                assertTrue(s >= last, "monotone for boss " + boss + " party " + party);
                assertTrue(s >= 0 && s <= 3);
                last = s;
            }
        }
    }

    @Test
    void theFactorsAndTheLabel() {
        assertEquals(1.0, DungeonLevel.damageFactor(0, 0.10), 1e-12);
        assertEquals(1.3, DungeonLevel.damageFactor(3, 0.10), 1e-12);
        assertEquals(1.0, DungeonLevel.damageFactor(2, -0.5), 1e-12, "a negative step is no step");
        assertEquals(1.0, DungeonLevel.damageFactor(-4, 0.10), 1e-12);
        assertEquals(1.06, DungeonLevel.speedFactor(2, 0.03), 1e-12);
        assertEquals(5, DungeonLevel.bossLevelAfter(2, 3));
        assertEquals(9, DungeonLevel.bossLevelAfter(8, 3));
        assertEquals(2, DungeonLevel.bossLevelAfter(2, -4));
        assertEquals("Lv.4", DungeonLevel.label(4));
        assertEquals("Lv.9", DungeonLevel.label(30));
        assertEquals("Lv.1", DungeonLevel.label(-2));
        assertTrue(DungeonLevel.frightens(3, 2));
        assertFalse(DungeonLevel.frightens(3, 3), "a fighter at the monster's Level is not frightened");
        assertFalse(DungeonLevel.frightens(3, 5));
    }
}
