package com.newuniverse.nusmp.dungeon.nexus.nav;

import com.newuniverse.nusmp.dungeon.wild.WildRules;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Where the cleaver strikes and what it finds there, and the avian compass drop. */
class OrganRulesTest {
    // ---------------------------------------------------------------- which carcass holds what
    @Test
    void aSpiderLeavesItsOwnKindOfCarcassAndOnlyThoseHoldAGland() {
        for (String id : new String[]{"tensura:black_spider", "tensura:knight_spider", "minecraft:spider", "minecraft:cave_spider"})
            assertEquals(OrganRules.ARACHNID, WildRules.carcassKind(id), id);
        assertEquals(OrganRules.BASILISK, WildRules.carcassKind("tensura:basilisk"));
        assertEquals(OrganRules.SLIME, WildRules.carcassKind("tensura:slime"));
        // every creature that is neither a spider nor a basilisk has no gland, whatever kind of carcass it leaves
        for (String id : new String[]{"tensura:goblin", "tensura:direwolf", "tensura:armorsaurus", "tensura:giant_bat", "iceandfire:hydra", "minecraft:zombie", null})
            assertFalse(OrganRules.hasGland(WildRules.carcassKind(id)), String.valueOf(id));
        for (int kind = 0; kind <= OrganRules.ARACHNID; kind++) {
            if (kind != OrganRules.BASILISK && kind != OrganRules.ARACHNID) assertFalse(OrganRules.hasGland(kind), "kind " + kind);
        }
    }

    @Test
    void aSlimeIsJellyAllThroughSoEveryCutOfItIsTheMeatCut() {
        assertFalse(OrganRules.hasHide(OrganRules.SLIME));
        for (CutZone z : CutZone.values()) assertEquals(CutZone.MEAT, OrganRules.settle(z, OrganRules.SLIME), z.name());
    }

    @Test
    void everythingElseKeepsItsOrgansAndItsHide() {
        for (int kind = 1; kind <= OrganRules.ARACHNID; kind++) {
            assertTrue(OrganRules.hasHide(kind), "kind " + kind);
            assertEquals(CutZone.HIDE, OrganRules.settle(CutZone.HIDE, kind), "kind " + kind);
            assertEquals(CutZone.ORGANS, OrganRules.settle(CutZone.ORGANS, kind), "kind " + kind);
            assertEquals(CutZone.MEAT, OrganRules.settle(CutZone.MEAT, kind), "the meat cut is always the meat cut");
        }
    }

    @Test
    void aCarcassOfAnUnknownKindIsCutForMeatAtTheOrgans() {
        assertEquals(CutZone.MEAT, OrganRules.settle(CutZone.ORGANS, -1));
        assertEquals(CutZone.MEAT, OrganRules.settle(CutZone.ORGANS, 99));
    }

    // ---------------------------------------------------------------- where the cleaver strikes
    @Test
    void theCarcassIsCutInThirdsFromTheBottom() {
        double h = CutZone.HEIGHT;
        assertEquals(6.0 / 16.0, h, 1e-12);
        assertEquals(CutZone.HIDE, CutZone.of(0.0, false));
        assertEquals(CutZone.HIDE, CutZone.of(h / 3 - 0.001, false));
        assertEquals(CutZone.MEAT, CutZone.of(h / 3 + 0.001, false));
        assertEquals(CutZone.MEAT, CutZone.of(h * 2 / 3 - 0.001, false));
        assertEquals(CutZone.ORGANS, CutZone.of(h * 2 / 3 + 0.001, false));
        assertEquals(CutZone.ORGANS, CutZone.of(h, false));
    }

    @Test
    void theUpperThirdOfTheFlankIsTheOrgans() {
        // a click 5 pixels up of the 6 is high on the carcass; 3 pixels is the middle; 1 pixel is low
        assertEquals(CutZone.ORGANS, CutZone.of(5 / 16.0, false));
        assertEquals(CutZone.MEAT, CutZone.of(3 / 16.0, false));
        assertEquals(CutZone.HIDE, CutZone.of(1 / 16.0, false));
    }

    @Test
    void aClickOnTheTopKeepsTheOldMeatCut() {
        for (double y : new double[]{0.0, 0.1, CutZone.HEIGHT, 5.0}) assertEquals(CutZone.MEAT, CutZone.of(y, true));
    }

    @Test
    void aClickOutsideTheShapeIsClamped() {
        assertEquals(CutZone.HIDE, CutZone.of(-3.0, false));
        assertEquals(CutZone.ORGANS, CutZone.of(7.0, false));
        assertEquals(CutZone.HIDE, CutZone.of(Double.NEGATIVE_INFINITY, false));
    }

    // ---------------------------------------------------------------- what the organs hold
    @Test
    void onlyBasiliskAndSpiderCarcassesHaveGlands() {
        assertFalse(OrganRules.hasGland(0));
        assertFalse(OrganRules.hasGland(1));
        assertTrue(OrganRules.hasGland(OrganRules.BASILISK));
        assertFalse(OrganRules.hasGland(3));
        assertFalse(OrganRules.hasGland(4));
        assertTrue(OrganRules.hasGland(OrganRules.ARACHNID));
        assertFalse(OrganRules.hasGland(-1));
        assertFalse(OrganRules.hasGland(99));
    }

    @Test
    void aSlimeNeverHoldsAMagicStoneAndTheStrongOnesDoAbout35Percent() {
        assertEquals(0.0, OrganRules.stoneChance(0));
        assertEquals(0.35, OrganRules.stoneChance(1), 1e-12);
        assertEquals(0.35, OrganRules.stoneChance(2), 1e-12);
        assertEquals(0.35, OrganRules.stoneChance(3), 1e-12);
        assertEquals(0.35, OrganRules.stoneChance(OrganRules.ARACHNID), 1e-12);
        assertTrue(OrganRules.stoneChance(4) > 0 && OrganRules.stoneChance(4) < 0.35, "unknown tier: a smaller chance");
        assertEquals(0.0, OrganRules.stoneChance(-1));
    }

    @Test
    void theRollsDecideTheHaul() {
        OrganRules.Haul h = OrganRules.organs(OrganRules.BASILISK, 0.0, 0.0);
        assertTrue(h.gland() && h.stone());
        h = OrganRules.organs(OrganRules.BASILISK, 0.99, 0.99);
        assertTrue(h.empty());
        h = OrganRules.organs(OrganRules.BASILISK, 0.5, 0.5);
        assertTrue(h.gland() && !h.stone());
        h = OrganRules.organs(OrganRules.BASILISK, 0.7, 0.2);
        assertTrue(!h.gland() && h.stone());
        h = OrganRules.organs(3, 0.0, 0.0);
        assertTrue(!h.gland() && h.stone(), "a direwolf has no gland");
        h = OrganRules.organs(0, 0.0, 0.0);
        assertTrue(h.empty(), "a slime has neither");
    }

    @Test
    void overManyCutsTheStoneComesAboutAThirdOfTheTime() {
        Random rnd = new Random(42);
        int stones = 0, glands = 0, n = 20000;
        for (int i = 0; i < n; i++) {
            OrganRules.Haul h = OrganRules.organs(OrganRules.BASILISK, rnd.nextDouble(), rnd.nextDouble());
            if (h.stone()) stones++;
            if (h.gland()) glands++;
        }
        assertEquals(0.35, stones / (double) n, 0.02);
        assertEquals(OrganRules.GLAND_CHANCE, glands / (double) n, 0.02);
    }

    // ---------------------------------------------------------------- the hide and bone
    @Test
    void theHideGivesALittle() {
        assertEquals(OrganRules.Hide.BONE_MEAL_1, OrganRules.hide(0.0));
        assertEquals(OrganRules.Hide.BONE_MEAL_2, OrganRules.hide(0.5));
        assertEquals(OrganRules.Hide.LEATHER, OrganRules.hide(0.9));
        Random rnd = new Random(5);
        int bone = 0, leather = 0, n = 10000;
        for (int i = 0; i < n; i++) {
            switch (OrganRules.hide(rnd.nextDouble())) {
                case BONE_MEAL_1, BONE_MEAL_2 -> bone++;
                case LEATHER -> leather++;
            }
        }
        assertTrue(bone > leather, "bone meal is the usual haul, leather the rarer");
        assertEquals(0.35, leather / (double) n, 0.02);
    }

    // ---------------------------------------------------------------- the avian compass drop
    @Test
    void theCompassDropsFourPercentOfTheTime() {
        assertEquals(0.04, AvianRules.chance(0), 1e-12);
        assertTrue(AvianRules.drops(0, 0.0399));
        assertFalse(AvianRules.drops(0, 0.04));
        assertFalse(AvianRules.drops(0, 0.99));
    }

    @Test
    void lootingAddsToTheChanceButNeverPastTheCeiling() {
        assertTrue(AvianRules.chance(1) > AvianRules.chance(0));
        assertTrue(AvianRules.chance(3) > AvianRules.chance(2));
        assertEquals(0.04 + 3 * 0.015, AvianRules.chance(3), 1e-12);
        assertEquals(AvianRules.CEILING, AvianRules.chance(500), 1e-12);
        assertEquals(AvianRules.chance(0), AvianRules.chance(-4), 1e-12, "a negative level is none");
        assertTrue(AvianRules.drops(3, 0.08));
        assertFalse(AvianRules.drops(0, 0.08));
    }

    @Test
    void overManyKillsTheRateMatches() {
        Random rnd = new Random(99);
        int n = 50000, hits = 0, hitsLooting = 0;
        for (int i = 0; i < n; i++) {
            double r = rnd.nextDouble();
            if (AvianRules.drops(0, r)) hits++;
            if (AvianRules.drops(3, r)) hitsLooting++;
        }
        assertEquals(0.04, hits / (double) n, 0.004);
        assertEquals(0.085, hitsLooting / (double) n, 0.006);
    }
}
