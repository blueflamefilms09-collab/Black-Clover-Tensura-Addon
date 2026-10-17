package com.newuniverse.nusmp.labyrinth.mega.gear;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: {@link ChestRules} decides which loot tables of the Mega-Dungeon may get a piece of gear, and a mistake in either direction is silent and costly: a
 * table wrongly NOT counted means chests that never hold gear, a table wrongly counted means gear in a mob's drops, a block's drops or, worst, in the pedestal
 * that holds exactly one relic. So the rules are pinned with the ids the game really has: the chest tables of this mod (standard), a table that names a vault or a
 * boss (treasure), the two relic tables (never, even when the data file lists nothing and even in another case), a mob's table, a block's table, a
 * fishing table and the "unknown" id NeoForge hands to a modifier when it cannot say which table was rolled (none of them), the data file's additions, and the
 * hard rule that no data file can take the two relic tables out of the exclusion.
 */
class ChestRulesTest {
    private static final ChestRules D = ChestRules.DEFAULT;

    @Test
    void theChestTablesOfThisModAreStandardChests() {
        for (String p : List.of("chests/labyrinth_trial", "chests/library_ruins", "chests/grimoire_tower"))
            assertEquals(ChestRules.Kind.STANDARD, D.classify("nusmp", p), p);
        assertEquals(ChestRules.Kind.STANDARD, D.classify("minecraft", "chests/simple_dungeon"));
        assertEquals(ChestRules.Kind.STANDARD, D.classify("othermod", "some/chests/crate"), "a chests/ segment anywhere in the path counts");
    }

    @Test
    void aTableThatNamesSomethingPreciousIsATreasureChest() {
        for (String w : ChestRules.DEFAULT_TREASURE_WORDS) assertEquals(ChestRules.Kind.TREASURE, D.classify("nusmp", "chests/" + w + "_room"), w);
        assertEquals(ChestRules.Kind.TREASURE, D.classify("minecraft", "chests/ancient_city_ice_box".replace("ice_box", "vault")));
        assertEquals(ChestRules.Kind.TREASURE, D.classify("NUSMP", "CHESTS/Boss_Loot"), "case does not matter");
    }

    @Test
    void theTwoRelicTablesAreNeverTouchedWhateverTheDataSays() {
        for (ChestRules r : List.of(D, new ChestRules(Set.of(), List.of()), new ChestRules(Set.of("minecraft:chests/x"), List.of("vault")))) {
            assertEquals(ChestRules.Kind.NONE, r.classify("nusmp", "chests/dungeon_relic_vault"), "the vault holds relics (and its path names a vault, so it would be a treasure chest)");
            assertEquals(ChestRules.Kind.NONE, r.classify("nusmp", "chests/labyrinth_pedestal"), "the pedestal holds exactly its relic");
            assertEquals(ChestRules.Kind.NONE, r.classify("NuSmp", "Chests/Dungeon_Relic_Vault"), "case does not matter");
        }
        assertEquals(Set.of("nusmp:chests/dungeon_relic_vault", "nusmp:chests/labyrinth_pedestal"), ChestRules.ALWAYS_EXCLUDED);
        assertFalse(D.classify("othermod", "chests/labyrinth_pedestal") == ChestRules.Kind.NONE, "only OUR two tables are relic tables; another mod's table of that name is its own");
    }

    @Test
    void whatIsNoChestTableIsNothing() {
        for (String[] t : new String[][] {{"minecraft", "entities/zombie"}, {"minecraft", "blocks/stone"}, {"minecraft", "gameplay/fishing"}, {"minecraft", "archaeology/desert_pyramid"},
                {"nusmp", "entities/floor_warden"}, {"neoforge", "unknown_loot_table"}, {"nusmp", "library"}, {"nusmp", ""}})
            assertEquals(ChestRules.Kind.NONE, D.classify(t[0], t[1]), t[0] + ":" + t[1]);
        assertEquals(ChestRules.Kind.NONE, D.classify(null, "chests/x"));
        assertEquals(ChestRules.Kind.NONE, D.classify("nusmp", null));
        assertEquals(ChestRules.Kind.NONE, D.classify("minecraft", "chest/x"), "chest, not chests/");
    }

    @Test
    void theDataFileCanAddExclusionsAndWordsAndWordsAreNormalised() {
        ChestRules r = new ChestRules(Set.of(" Nusmp:Chests/Special_Reserve ", "", "  "), List.of(" Hoard ", "", "  "));
        assertEquals(ChestRules.Kind.NONE, r.classify("nusmp", "chests/special_reserve"));
        assertEquals(ChestRules.Kind.TREASURE, r.classify("nusmp", "chests/dragon_hoard"));
        assertEquals(ChestRules.Kind.STANDARD, r.classify("nusmp", "chests/boss_loot"), "the file REPLACED the words, so 'boss' is no longer one");
        assertEquals(List.of("hoard"), r.treasureWords());
        assertEquals(Set.of("nusmp:chests/special_reserve"), r.excluded());
        ChestRules nulls = new ChestRules(null, null);
        assertEquals(ChestRules.Kind.STANDARD, nulls.classify("nusmp", "chests/labyrinth_trial"));
    }

    @Test
    void theDefaultWordsAreTheShippedOnes() {
        assertEquals(List.of("treasure", "vault", "boss", "guardian", "crucible", "antechamber", "reward"), ChestRules.DEFAULT_TREASURE_WORDS);
        assertTrue(D.excluded().isEmpty());
    }
}
