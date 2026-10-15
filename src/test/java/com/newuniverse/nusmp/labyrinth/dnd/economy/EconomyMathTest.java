package com.newuniverse.nusmp.labyrinth.dnd.economy;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.dungeon.sentient.StoneTable;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the drop table is pure so its promises are checked without a server: chance gates, non-player deaths, fleshless creatures, cut kinds, bounds. */
class EconomyMathTest {
    private static final Random R = new Random(2026);
    private static Dice seeded(long seed) { return R::nextDouble; }

    @Test void chanceScalesWithToughnessAndClamps() {
        assertTrue(EconomyMath.stoneChance(StoneTable.Grade.TIER1, 0.85) < EconomyMath.stoneChance(StoneTable.Grade.TIER3, 0.85));
        assertEquals(1.0, EconomyMath.stoneChance(StoneTable.Grade.JUGGERNAUT, 1.0));
        assertEquals(0.0, EconomyMath.stoneChance(StoneTable.Grade.TIER2, 0.0));
        assertEquals(1.0, EconomyMath.meatChance(0.98, 4));
    }

    @Test void zeroChanceDropsNothing() {
        for (int i = 0; i < 200; i++) {
            assertTrue(EconomyMath.stones(StoneTable.Grade.TIER3, true, 3, 0.0, seeded(i)).isEmpty());
            assertTrue(EconomyMath.meat("zombie", StoneTable.Grade.TIER3, true, 3, 0.0, seeded(i)).isEmpty());
        }
    }

    @Test void certainChancePlayerKillAlwaysLeavesStones() {
        for (int i = 0; i < 200; i++) {
            StoneTable.Haul h = EconomyMath.stones(StoneTable.Grade.JUGGERNAUT, true, 0, 1.0, seeded(i));
            assertTrue(h.stones() >= StoneTable.least(StoneTable.Grade.JUGGERNAUT));
            assertTrue(h.stones() <= StoneTable.most(StoneTable.Grade.JUGGERNAUT));
        }
    }

    @Test void nonPlayerDeathIsAtMostOneStoneAndNoMeat() {
        int total = 0;
        for (int i = 0; i < 2000; i++) {
            StoneTable.Haul h = EconomyMath.stones(StoneTable.Grade.TIER3, false, 5, 1.0, seeded(i));
            assertTrue(h.stones() <= 1 && h.low() == 0 && h.medium() == 0 && h.high() == 0);
            total += h.stones();
            assertTrue(EconomyMath.meat("zombie", StoneTable.Grade.TIER3, false, 5, 1.0, seeded(i)).isEmpty());
        }
        assertTrue(total > 300 && total < 700, "about one in four, was " + total);
    }

    @Test void lootingRaisesTheTopOfTheRange() {
        int max0 = 0, max3 = 0;
        for (int i = 0; i < 3000; i++) {
            max0 = Math.max(max0, EconomyMath.stones(StoneTable.Grade.TIER2, true, 0, 1.0, seeded(i)).stones());
            max3 = Math.max(max3, EconomyMath.stones(StoneTable.Grade.TIER2, true, 3, 1.0, seeded(i)).stones());
        }
        assertEquals(StoneTable.most(StoneTable.Grade.TIER2), max0);
        assertEquals(StoneTable.most(StoneTable.Grade.TIER2) + 3, max3);
    }

    @Test void fleshlessLeaveNoMeatButArmorsaurusDoes() {
        assertTrue(EconomyMath.fleshless("skeleton"));
        assertTrue(EconomyMath.fleshless("blaze"));
        assertFalse(EconomyMath.fleshless("zombie"));
        assertFalse(EconomyMath.fleshless("armorsaurus"));
        assertTrue(EconomyMath.meat("skeleton", StoneTable.Grade.TIER3, true, 0, 1.0, () -> 0.0).isEmpty());
    }

    @Test void cutKindFollowsTheCreature() {
        assertEquals(EconomyMath.JELLY, EconomyMath.kindOf("slime"));
        assertEquals(EconomyMath.FLANK, EconomyMath.kindOf("armorsaurus"));
        assertEquals(EconomyMath.FILLET, EconomyMath.kindOf("basilisk"));
        assertEquals(EconomyMath.HAUNCH, EconomyMath.kindOf("direwolf"));
        assertEquals(EconomyMath.PLAIN, EconomyMath.kindOf("zombie"));
        assertEquals(EconomyMath.PLAIN, EconomyMath.kindOf(null));
    }

    @Test void meatCountsAreBounded() {
        for (int i = 0; i < 500; i++) {
            EconomyMath.Meat m = EconomyMath.meat("direwolf", StoneTable.Grade.TIER3, true, 3, 1.0, seeded(i));
            assertTrue(m.count() >= 1 && m.count() <= 4);
            EconomyMath.Meat w = EconomyMath.meat("slime", StoneTable.Grade.TIER1, true, 0, 1.0, seeded(i));
            assertEquals(1, w.count());
        }
    }

    @Test void lightIdsAreRecognised() {
        assertTrue(LightItems.isLight("minecraft", "torch"));
        assertTrue(LightItems.isLight("nusmp", "ki_resonance_lantern"));
        assertTrue(LightItems.isLight("othermod", "crystal_lamp"));
        assertFalse(LightItems.isLight("minecraft", "stick"));
    }
}
