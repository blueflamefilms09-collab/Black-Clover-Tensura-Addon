package com.newuniverse.nusmp.labyrinth.mega.eco;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.dungeon.sentient.StoneTable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the faction drops add physical loot to a death, so the promises are checked with seeded dice: the cut chance only ever rises with depth and
 * Looting and never passes its cap, a faction with no chance never leaves a cut, the count never passes the configured maximum, a creature with no
 * flesh leaves no cut, a turf-war death (no player) leaves nothing, and the depth bonus is exactly one item of the right band, rarer the better it is.
 */
class FactionLootTest {
    private static Dice seeded(long seed) { return Fx.seeded(seed); }

    private static final FactionDef BEAST = Fx.dropper("beast", "raw_direwolf_haunch", 0.55);

    @Test void theCutChanceRisesWithDepthAndLootingAndIsCapped() {
        double base = FactionLoot.meatChance(0.40, 1, 0);
        assertEquals(0.40, base);
        assertTrue(FactionLoot.meatChance(0.40, 10, 0) > base);
        assertTrue(FactionLoot.meatChance(0.40, 1, 3) > base);
        assertEquals(0.40 * (1.0 + 9 * FactionLoot.MEAT_PER_THREAT), FactionLoot.meatChance(0.40, 10, 0), 1e-9, "3 percent of itself per threat level above 1");
        assertEquals(0.03, FactionLoot.MEAT_PER_THREAT, "the documented step");
        assertEquals(0.40 + 0.15, FactionLoot.meatChance(0.40, 1, 3), 1e-9, "5 points per Looting level");
        assertEquals(FactionLoot.MAX_MEAT_CHANCE, FactionLoot.meatChance(0.90, 40, 5));
        for (int t = 1; t < 60; t++) assertTrue(FactionLoot.meatChance(0.5, t + 1, 2) >= FactionLoot.meatChance(0.5, t, 2), "monotonic at threat " + t);
    }

    @Test void aFactionWithNoChanceNeverLeavesACut() {
        assertEquals(0.0, FactionLoot.meatChance(0.0, 30, 5));
        assertEquals(0.0, FactionLoot.meatChance(-1.0, 30, 5));
        for (int i = 0; i < 500; i++)
            assertTrue(FactionLoot.roll(Fx.dropper("elemental", "raw_monster_meat", 0.0), "blaze_like", 20, 3, true, 8, 0.0, seeded(i)).meat() == 0);
    }

    @Test void theCountIsOneToTheMaximumAndLootingHelps() {
        int shallow = 0, deepLoot = 0;
        for (int i = 0; i < 4000; i++) {
            int a = FactionLoot.meatCount(1, 0, 3, seeded(i)), b = FactionLoot.meatCount(30, 3, 3, seeded(i));
            assertTrue(a == 1, "no depth bonus and no Looting is always one cut, was " + a);
            assertTrue(b >= 1 && b <= 3, "never above the maximum, was " + b);
            shallow += a;
            deepLoot += b;
        }
        assertTrue(deepLoot > shallow * 2, "deep and looted pays clearly more: " + deepLoot + " vs " + shallow);
        for (int i = 0; i < 200; i++) assertTrue(FactionLoot.meatCount(40, 5, 1, seeded(i)) == 1, "a maximum of one is one");
        assertEquals(0, FactionLoot.meatCount(40, 5, 0, seeded(1)), "a maximum of zero switches the cuts off");
    }

    @Test void bandsFollowTheThreat() {
        assertEquals(0, FactionLoot.band(0));
        assertEquals(0, FactionLoot.band(5));
        assertEquals(1, FactionLoot.band(6));
        assertEquals(1, FactionLoot.band(13));
        assertEquals(2, FactionLoot.band(14));
        assertEquals(2, FactionLoot.band(25));
        assertEquals(3, FactionLoot.band(26));
        assertEquals(3, FactionLoot.band(500));
    }

    @Test void theBonusChanceIsTwoPercentPerThreatCappedAndRarerForBetterItems() {
        assertEquals(0.02, FactionLoot.stoneBonusChance(1, 0.02), 1e-12);
        assertEquals(0.10, FactionLoot.stoneBonusChance(5, 0.02), 1e-12);
        assertEquals(0.0, FactionLoot.stoneBonusChance(0, 0.02));
        assertEquals(0.0, FactionLoot.stoneBonusChance(10, 0.0));
        assertEquals(0.0, FactionLoot.stoneBonusChance(-3, 0.02));
        assertEquals(0.12 / 4, FactionLoot.stoneBonusChance(6, 0.02), 1e-12, "a low crystal is a quarter as likely as a stone would be");
        assertEquals(0.5 / 16, FactionLoot.stoneBonusChance(25, 0.02), 1e-12, "capped at one half before the medium crystal's rarity divides it");
        assertEquals(0.5 / 64, FactionLoot.stoneBonusChance(80, 0.5), 1e-12);
        for (int t = 1; t < 100; t++) assertTrue(FactionLoot.stoneBonusChance(t, 0.02) <= FactionLoot.STONE_BONUS_CAP, "cap at threat " + t);
        for (int t = 1; t < 5; t++) assertTrue(FactionLoot.stoneBonusChance(t + 1, 0.02) > FactionLoot.stoneBonusChance(t, 0.02), "rises inside a band");
    }

    @Test void theBonusIsExactlyOneItemOfTheBand() {
        int[] threats = {3, 10, 20, 40};
        for (int band = 0; band < 4; band++) {
            int hits = 0, trials = 20000;
            for (int i = 0; i < trials; i++) {
                StoneTable.Haul h = FactionLoot.stoneBonus(threats[band], 0.5, seeded(i * 31L + band));
                if (h.isEmpty()) continue;
                hits++;
                int total = h.stones() + h.low() + h.medium() + h.high();
                assertEquals(1, total, "one item per bonus");
                switch (band) {
                    case 0 -> assertEquals(1, h.stones());
                    case 1 -> assertEquals(1, h.low());
                    case 2 -> assertEquals(1, h.medium());
                    default -> assertEquals(1, h.high());
                }
            }
            double expected = FactionLoot.stoneBonusChance(threats[band], 0.5);
            assertEquals(expected, hits / (double) trials, 0.015, "band " + band + " hit rate");
        }
    }

    @Test void noPlayerNoLoot() {
        for (int i = 0; i < 200; i++) assertTrue(FactionLoot.roll(BEAST, "direwolf", 30, 3, false, 4, 0.5, seeded(i)).isEmpty());
        assertTrue(FactionLoot.roll(null, "direwolf", 30, 3, true, 4, 0.5, seeded(1)).isEmpty());
        assertEquals(FactionLoot.Extra.NONE, FactionLoot.roll(BEAST, "direwolf", 30, 3, false, 4, 0.5, seeded(1)));
    }

    @Test void aCreatureWithNoFleshLeavesNoCutButStillPaysTheBonus() {
        int bonus = 0;
        for (int i = 0; i < 2000; i++) {
            FactionLoot.Extra x = FactionLoot.roll(Fx.dropper("undead", "raw_monster_meat", 1.0), "skeleton", 10, 0, true, 4, 0.5, seeded(i));
            assertEquals(0, x.meat(), "bones have no meat");
            if (!x.stones().isEmpty()) bonus++;
        }
        assertTrue(bonus > 200, "the depth bonus does not need flesh, was " + bonus);
    }

    @Test void theCutIsTheFactionsOwnKind() {
        FactionLoot.Extra x = null;
        for (int i = 0; i < 100 && (x == null || x.meat() == 0); i++) x = FactionLoot.roll(BEAST, "direwolf", 3, 0, true, 2, 0.02, seeded(i));
        assertTrue(x.meat() > 0);
        assertEquals(3, x.meatKind(), "the beast faction leaves the haunch (kind 3)");
        assertEquals(0, FactionLoot.roll(Fx.dropper("slime", "raw_slime_jelly", 1.0), "slime", 1, 0, true, 2, 0.0, seeded(2)).meatKind());
    }

    @Test void theExtrasAreBoundedForEveryRoll() {
        for (int i = 0; i < 3000; i++) {
            FactionLoot.Extra x = FactionLoot.roll(BEAST, "direwolf", 1 + i % 40, i % 4, true, 2, 0.02, seeded(i));
            assertTrue(x.meat() >= 0 && x.meat() <= 2, "meat " + x.meat());
            int items = x.stones().stones() + x.stones().low() + x.stones().medium() + x.stones().high();
            assertTrue(items <= 1, "at most one bonus item");
        }
    }

    @Test void theItemPathsAreTheBaseEconomysOwn() {
        assertEquals("magic_stone", FactionLoot.STONE_PATH);
        assertEquals("low_quality_magic_crystal", FactionLoot.LOW_PATH);
        assertEquals("medium_quality_magic_crystal", FactionLoot.MEDIUM_PATH);
        assertEquals("high_quality_magic_crystal", FactionLoot.HIGH_PATH);
    }
}
