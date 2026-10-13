package com.newuniverse.nusmp.dungeon.sentient;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The Magic Stone table: tier 1 one stone, tier 2 one to two, tier 3 two to three and a crystal now and then, Juggernaut and boss more. */
class StoneTableTest {
    private static Dice dice(long seed) { return new Random(seed)::nextDouble; }

    private static final int N = 20000;

    @Test
    void theGradeFollowsTheTierThenTheHealth() {
        assertEquals(StoneTable.Grade.TIER1, StoneTable.gradeOf(1, 500, false, false));
        assertEquals(StoneTable.Grade.TIER2, StoneTable.gradeOf(2, 5, false, false));
        assertEquals(StoneTable.Grade.TIER3, StoneTable.gradeOf(3, 5, false, false));
        assertEquals(StoneTable.Grade.TIER1, StoneTable.gradeOf(0, 19.9, false, false));
        assertEquals(StoneTable.Grade.TIER2, StoneTable.gradeOf(0, 20, false, false));
        assertEquals(StoneTable.Grade.TIER2, StoneTable.gradeOf(0, 59.9, false, false));
        assertEquals(StoneTable.Grade.TIER3, StoneTable.gradeOf(0, 60, false, false));
        assertEquals(StoneTable.Grade.JUGGERNAUT, StoneTable.gradeOf(3, 300, false, true));
        assertEquals(StoneTable.Grade.BOSS, StoneTable.gradeOf(3, 300, true, false));
        assertEquals(StoneTable.Grade.BOSS, StoneTable.gradeOf(0, 1, true, true), "a boss is a boss before it is a Juggernaut");
    }

    @Test
    void tierOneLeavesExactlyOneStone() {
        Dice d = dice(1);
        for (int i = 0; i < N; i++) {
            StoneTable.Haul h = StoneTable.roll(StoneTable.Grade.TIER1, 0, 1.0, d);
            assertEquals(1, h.stones());
            assertEquals(0, h.low() + h.medium() + h.high());
        }
    }

    @Test
    void tierTwoLeavesOneOrTwoAndBothHappen() {
        Dice d = dice(2);
        int ones = 0, twos = 0;
        for (int i = 0; i < N; i++) {
            StoneTable.Haul h = StoneTable.roll(StoneTable.Grade.TIER2, 0, 1.0, d);
            assertTrue(h.stones() == 1 || h.stones() == 2, "stones " + h.stones());
            if (h.stones() == 1) ones++; else twos++;
            assertEquals(0, h.low() + h.medium() + h.high());
        }
        assertEquals(0.5, ones / (double) N, 0.03);
        assertTrue(twos > 0);
    }

    @Test
    void tierThreeLeavesTwoOrThreeAndAQuarterOfThemAlsoALowCrystal() {
        Dice d = dice(3);
        int crystals = 0;
        boolean two = false, three = false;
        for (int i = 0; i < N; i++) {
            StoneTable.Haul h = StoneTable.roll(StoneTable.Grade.TIER3, 0, 1.0, d);
            assertTrue(h.stones() >= 2 && h.stones() <= 3);
            two |= h.stones() == 2;
            three |= h.stones() == 3;
            assertTrue(h.low() == 0 || h.low() == 1);
            assertEquals(0, h.medium() + h.high());
            crystals += h.low();
        }
        assertTrue(two && three);
        assertEquals(0.25, crystals / (double) N, 0.02);
    }

    @Test
    void aJuggernautLeavesThreeToFiveStonesAndAMediumCrystal() {
        Dice d = dice(4);
        for (int i = 0; i < N; i++) {
            StoneTable.Haul h = StoneTable.roll(StoneTable.Grade.JUGGERNAUT, 0, 1.0, d);
            assertTrue(h.stones() >= 3 && h.stones() <= 5, "stones " + h.stones());
            assertEquals(1, h.medium());
            assertEquals(0, h.low() + h.high());
        }
    }

    @Test
    void aBossLeavesFiveToEightStonesAMediumCrystalAndSometimesAHighOne() {
        Dice d = dice(5);
        int high = 0;
        for (int i = 0; i < N; i++) {
            StoneTable.Haul h = StoneTable.roll(StoneTable.Grade.BOSS, 0, 1.0, d);
            assertTrue(h.stones() >= 5 && h.stones() <= 8, "stones " + h.stones());
            assertEquals(1, h.medium());
            assertTrue(h.high() == 0 || h.high() == 1);
            high += h.high();
        }
        assertEquals(0.15, high / (double) N, 0.02);
    }

    @Test
    void lootingRaisesTheMostByOnePerLevel() {
        for (StoneTable.Grade g : StoneTable.Grade.values()) {
            for (int looting = 0; looting <= 3; looting++) {
                Dice d = dice(10 + looting);
                int max = 0, min = Integer.MAX_VALUE;
                for (int i = 0; i < N; i++) {
                    int s = StoneTable.roll(g, looting, 1.0, d).stones();
                    max = Math.max(max, s);
                    min = Math.min(min, s);
                }
                assertEquals(StoneTable.most(g) + looting, max, g + " looting " + looting);
                assertEquals(StoneTable.least(g), min, g + " never fewer than the least");
            }
        }
    }

    @Test
    void theStoneDropsSettingScalesTheHaul() {
        Dice d = dice(6);
        assertTrue(StoneTable.roll(StoneTable.Grade.BOSS, 2, 0.0, d).isEmpty(), "0 = no stones at all");
        double sum = 0;
        for (int i = 0; i < N; i++) sum += StoneTable.roll(StoneTable.Grade.TIER1, 0, 0.5, d).stones();
        assertEquals(0.5, sum / N, 0.02, "half the stones on average");
        for (int i = 0; i < 200; i++) assertEquals(2, StoneTable.roll(StoneTable.Grade.TIER1, 0, 2.0, d).stones());
        for (int i = 0; i < 200; i++) assertEquals(5, StoneTable.roll(StoneTable.Grade.TIER1, 0, 5.0, d).stones());
    }

    @Test
    void whatNoPlayerKilledLeavesAStoneOneTimeInFour() {
        Dice d = dice(7);
        int stones = 0;
        for (int i = 0; i < N; i++) {
            StoneTable.Haul h = StoneTable.ambient(1.0, d);
            assertTrue(h.stones() == 0 || h.stones() == 1);
            assertEquals(0, h.low() + h.medium() + h.high());
            stones += h.stones();
        }
        assertEquals(0.25, stones / (double) N, 0.02);
        for (int i = 0; i < 500; i++) assertTrue(StoneTable.ambient(0.0, d).isEmpty());
        int richer = 0;
        for (int i = 0; i < N; i++) richer += StoneTable.ambient(5.0, d).stones();
        assertEquals(0.25, richer / (double) N, 0.02, "a setting above 1 does not make the Dungeon's own fights a farm");
    }

    @Test
    void sameSeedSameHaul() {
        for (StoneTable.Grade g : StoneTable.Grade.values()) assertEquals(StoneTable.roll(g, 1, 1.5, dice(99)), StoneTable.roll(g, 1, 1.5, dice(99)));
    }

    @Test
    void anEmptyHaulIsEmpty() {
        assertTrue(StoneTable.Haul.NONE.isEmpty());
        assertFalse(new StoneTable.Haul(1, 0, 0, 0).isEmpty());
        assertFalse(new StoneTable.Haul(0, 0, 0, 1).isEmpty());
    }
}
