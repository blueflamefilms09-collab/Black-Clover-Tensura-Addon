package com.newuniverse.nusmp.relics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Tensura grades and the card-rarity mapping. */
class GradeTest {
    @Test
    void ofCardMapsThePrintedRarity() {
        assertEquals(Grade.NORMAL, Grade.ofCard("common"));
        assertEquals(Grade.SPECIAL, Grade.ofCard("uncommon"));
        assertEquals(Grade.RARE, Grade.ofCard("rare"));
        assertEquals(Grade.UNIQUE, Grade.ofCard("very rare"));
        assertEquals(Grade.UNIQUE, Grade.ofCard("Very Rare"));
        assertEquals(Grade.UNIQUE, Grade.ofCard("very_rare"));
        assertEquals(Grade.LEGEND, Grade.ofCard("epic"));
        assertEquals(Grade.LEGEND, Grade.ofCard("legendary"));
        assertEquals(Grade.LEGEND, Grade.ofCard("  LEGENDARY "));
    }

    @Test
    void unknownRarityIsRare() {
        assertEquals(Grade.RARE, Grade.ofCard(null));
        assertEquals(Grade.RARE, Grade.ofCard(""));
        assertEquals(Grade.RARE, Grade.ofCard("artifact"));
        assertEquals(Grade.RARE, Grade.ofCard("varies"));
    }

    @Test
    void theOrderIsTheTensuraOrder() {
        Grade[] g = Grade.values();
        assertEquals(7, g.length);
        assertEquals(Grade.NORMAL, g[0]);
        assertEquals(Grade.GENESIS, g[6]);
        for (int i = 0; i < g.length; i++) assertEquals(i + 1, g[i].tier());
        assertTrue(Grade.GOD.atLeast(Grade.LEGEND));
        assertTrue(Grade.LEGEND.atLeast(Grade.LEGEND));
        assertFalse(Grade.UNIQUE.atLeast(Grade.LEGEND));
    }

    @Test
    void onlyLegendAndAboveCanCarryADjinn() {
        for (Grade g : Grade.values()) assertEquals(g.ordinal() >= Grade.LEGEND.ordinal(), g.djinnCapable(), g.name());
    }

    @Test
    void epBonusAndLootWeightMoveTheRightWay() {
        double ep = -1, weight = Double.MAX_VALUE;
        for (Grade g : Grade.values()) {
            assertTrue(g.epBonus() >= ep, "EP bonus never falls with the grade");
            assertTrue(g.weight() < weight, "rarer grades are rarer in loot");
            ep = g.epBonus();
            weight = g.weight();
        }
        assertEquals(0.12, Grade.GENESIS.epBonus(), 1e-9);
        assertEquals(0.0, Grade.NORMAL.epBonus(), 1e-9);
    }

    @Test
    void labelsAndStars() {
        assertEquals("Rare Grade", Grade.RARE.label());
        assertEquals("Rare", Grade.RARE.shortName());
        assertEquals(7, Grade.GENESIS.stars().length());
        assertEquals(3, Grade.RARE.stars().chars().filter(c -> c == '★').count());
        assertEquals(Grade.LEGEND, Grade.byName("legend grade"));
        assertEquals(Grade.GOD, Grade.byName("GOD"));
        assertNull(Grade.byName("mythic"));
        assertEquals(Grade.GOD, Grade.max(Grade.RARE, Grade.GOD));
        assertEquals(Grade.RARE, Grade.max(null, Grade.RARE));
    }
}
