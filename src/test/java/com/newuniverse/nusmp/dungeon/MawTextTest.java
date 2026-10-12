package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.dungeon.MawShapes.Kind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The owner's broadcast wordings (docs/living_dungeon.md section 2), word for word. */
class MawTextTest {
    @Test
    void aTowerOpensWithTheOwnersWords() {
        assertEquals("[Server] A spatial distortion has materialized. A new Dungeon Tower has breached the Plains biome near (1200, -300)...",
                MawText.opened(Kind.TOWER, "plains", 1200, -300));
    }

    @Test
    void theWordFollowsTheManifestation() {
        assertTrue(MawText.opened(Kind.RUINS, "plains", 0, 0).contains("A new Dungeon Ruin has breached"));
        assertTrue(MawText.opened(Kind.GATE, "plains", 0, 0).contains("A new Dungeon Gate has breached"));
        assertTrue(MawText.opened(Kind.TOWER, "plains", 0, 0).contains("A new Dungeon Tower has breached"));
        assertTrue(MawText.opened(Kind.HOLE, "plains", 0, 0).contains("A new Dungeon Maw has breached"), "the plain mouth is not a tower");
        for (Kind k : Kind.values()) assertTrue(MawText.opened(k, "plains", 5, 6).startsWith("[Server] A spatial distortion has materialized. A new Dungeon "));
    }

    @Test
    void theBiomeIsTitleCased() {
        assertEquals("Plains", MawText.biomeName("plains"));
        assertEquals("Dark Forest", MawText.biomeName("dark_forest"));
        assertEquals("Windswept Gravelly Hills", MawText.biomeName("windswept_gravelly_hills"));
        assertEquals("Old Growth Pine Taiga", MawText.biomeName("old_growth_pine_taiga"));
        assertEquals("The End", MawText.biomeName("the_end"));
        assertEquals("Nether Wastes", MawText.biomeName("nether_wastes"));
        assertEquals("Deep Lukewarm Ocean", MawText.biomeName("deep_lukewarm_ocean"));
        assertTrue(MawText.opened(Kind.RUINS, "snowy_taiga", 100, 200).contains("breached the Snowy Taiga biome near (100, 200)..."));
    }

    @Test
    void oddBiomeNamesStillReadWell() {
        assertEquals("Foo Bar Baz", MawText.biomeName("foo/bar_baz"), "a modded path with folders");
        assertEquals("Foo Bar", MawText.biomeName("__foo--bar__"), "stray separators");
        assertEquals("Cave 2", MawText.biomeName("cave_2"));
        assertEquals("A B", MawText.biomeName("a///b"));
        assertEquals(MawText.UNKNOWN_BIOME, MawText.biomeName(""));
        assertEquals(MawText.UNKNOWN_BIOME, MawText.biomeName(null));
        assertEquals(MawText.UNKNOWN_BIOME, MawText.biomeName("___"));
        assertEquals("Plains", MawText.biomeName("PLAINS"), "the case of the rest is lowered");
    }

    @Test
    void theFamineNamesTheTier() {
        assertEquals("[Server] A localized famine in the Volcanic Vents has caused a mass predator die-off.", MawText.famine(DungeonTier.VOLCANIC_VENTS.label));
        assertEquals("[Server] A localized famine in the Deep Waterways has caused a mass predator die-off.", MawText.famine(DungeonTier.DEEP_WATERWAYS.label));
        assertEquals("[Server] A localized famine in the Upper Catacombs has caused a mass predator die-off.", MawText.famine(DungeonTier.UPPER_CATACOMBS.label));
        assertEquals("[Server] A localized famine in the Variable Core has caused a mass predator die-off.", MawText.famine("The Variable Core"), "no 'the The'");
        assertEquals("[Server] A localized famine in the Dungeon has caused a mass predator die-off.", MawText.famine(null));
        for (DungeonTier t : DungeonTier.values()) assertFalse(MawText.famine(t.label).contains("the The "), t.name());
    }

    @Test
    void aLeadingTheIsNotRepeated() {
        assertEquals("the Variable Core", MawText.the("The Variable Core"));
        assertEquals("the Upper Catacombs", MawText.the("Upper Catacombs"));
    }
}
