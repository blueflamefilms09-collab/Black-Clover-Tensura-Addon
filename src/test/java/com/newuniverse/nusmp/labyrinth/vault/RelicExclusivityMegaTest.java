package com.newuniverse.nusmp.labyrinth.vault;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Mega-Dungeon's addition to the exclusivity rules (1.54.0): in the singular dungeon a Crucible is the only source of relics, so neither a Mimic nor
 * the world's loot rolls one there, whatever the config says; everywhere else the old two-argument answers are unchanged.
 */
class RelicExclusivityMegaTest {
    @Test
    void aMimicDropsNothingInTheMegaDungeonWhateverTheConfigSays() {
        assertFalse(RelicExclusivity.allowsMobDrop(true, true, true), "exclusive: a dungeon, but the Mega one");
        assertFalse(RelicExclusivity.allowsMobDrop(true, true, false), "not exclusive: the Crucible is still the only source there");
        assertFalse(RelicExclusivity.allowsMobDrop(false, true, true));
        assertFalse(RelicExclusivity.allowsMobDrop(false, true, false));
    }

    @Test
    void everywhereElseTheOldRuleStands() {
        for (boolean inDungeon : new boolean[] {false, true}) {
            for (boolean exclusive : new boolean[] {false, true}) {
                assertEquals(RelicExclusivity.allowsMobDrop(inDungeon, exclusive), RelicExclusivity.allowsMobDrop(inDungeon, false, exclusive), "dungeon " + inDungeon + ", exclusive " + exclusive);
            }
        }
        assertTrue(RelicExclusivity.allowsMobDrop(true, false, true), "the Labyrinth still gives Mimic relics");
        assertFalse(RelicExclusivity.allowsMobDrop(false, false, true), "and the Overworld still does not while exclusive");
        assertTrue(RelicExclusivity.allowsMobDrop(false, false, false), "or does when not exclusive");
    }

    @Test
    void theWorldsLootRollsNoRelicInTheMegaDungeon() {
        assertFalse(RelicExclusivity.allowsWildRoll(true));
        assertTrue(RelicExclusivity.allowsWildRoll(false));
    }
}
