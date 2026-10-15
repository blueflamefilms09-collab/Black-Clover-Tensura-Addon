package com.newuniverse.nusmp.labyrinth.vault;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The pure exclusivity rules: which chest tables and which mob deaths may yield a relic, exclusive and not. */
class RelicExclusivityRulesTest {
    @Test
    void exclusiveAllowsOnlyTheTwoLabyrinthTables() {
        assertTrue(RelicExclusivity.allowsChest("nusmp", "chests/dungeon_relic_vault", true));
        assertTrue(RelicExclusivity.allowsChest("nusmp", "chests/labyrinth_pedestal", true));
        assertFalse(RelicExclusivity.allowsChest("nusmp", "chests/labyrinth_trial", true), "the trial chests are mundane");
        assertFalse(RelicExclusivity.allowsChest("nusmp", "chests/library_ruins", true));
        assertFalse(RelicExclusivity.allowsChest("minecraft", "chests/simple_dungeon", true));
        assertFalse(RelicExclusivity.allowsChest("minecraft", "chests/ancient_city", true));
        assertFalse(RelicExclusivity.allowsChest("minecraft", "chests/trial_chambers/reward", true));
        assertFalse(RelicExclusivity.allowsChest("othermod", "chests/dungeon_relic_vault", true), "same path, another namespace");
        assertFalse(RelicExclusivity.allowsChest("nusmp", "dungeon_relic_vault", true), "the folder is part of the id");
        assertFalse(RelicExclusivity.allowsChest("nusmp", null, true));
        assertFalse(RelicExclusivity.allowsChest(null, "chests/dungeon_relic_vault", true));
    }

    @Test
    void notExclusiveIsTheOldBehaviour() {
        assertTrue(RelicExclusivity.allowsChest("minecraft", "chests/simple_dungeon", false));
        assertTrue(RelicExclusivity.allowsChest("minecraft", "chests/ancient_city", false));
        assertTrue(RelicExclusivity.allowsChest("nusmp", "chests/labyrinth_trial", false));
        assertTrue(RelicExclusivity.allowsChest("x", null, false), "the old modifier decided by the path words, not here");
    }

    @Test
    void mobDropsOnlyInsideADungeonWhileExclusive() {
        assertTrue(RelicExclusivity.allowsMobDrop(true, true));
        assertFalse(RelicExclusivity.allowsMobDrop(false, true));
        assertTrue(RelicExclusivity.allowsMobDrop(true, false));
        assertTrue(RelicExclusivity.allowsMobDrop(false, false));
    }

    @Test
    void theAllowedTablesAreExactlyTheVaultAndThePedestal() {
        assertEquals(2, RelicExclusivity.VAULT_TABLES.size());
        assertEquals("nusmp", RelicExclusivity.NAMESPACE);
    }
}
