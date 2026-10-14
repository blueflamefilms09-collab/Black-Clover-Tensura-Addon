package com.newuniverse.nusmp.relics.socket;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Where Cores may drop and which one a roll picks. */
class SocketLootRulesTest {
    @Test
    void chestPaths() {
        assertTrue(SocketLootRules.chest("chests/ancient_city"));
        assertTrue(SocketLootRules.chest("chests/stronghold_corridor"));
        assertTrue(SocketLootRules.chest("chests/simple_dungeon"));
        assertFalse(SocketLootRules.chest("chests/village_blacksmith"));
        assertFalse(SocketLootRules.chest("entities/zombie"));
        assertFalse(SocketLootRules.chest(null));
        assertTrue(SocketLootRules.deep("chests/trial_chambers/reward"));
        assertFalse(SocketLootRules.deep("chests/simple_dungeon"));
    }

    @Test
    void theWeightsAreLow() {
        assertTrue(SocketLootRules.CHEST_SPIRIT < 0.02f && SocketLootRules.CHEST_DEEP_SPIRIT < 0.05f && SocketLootRules.CHEST_DEEP_DEVIL < 0.02f);
        assertTrue(SocketLootRules.BOSS_DEVIL < SocketLootRules.BOSS_SPIRIT);
    }

    @Test
    void spiritPickCoversAllSixAndFavoursTheCommonFour() {
        int[] hits = new int[6];
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String id = SocketLootRules.pickSpirit(i / 1000.0);
            seen.add(id);
            SocketDef d = SocketCatalog.byId(id);
            assertNotNull(d);
            hits[SocketCatalog.spirits().indexOf(d)]++;
        }
        assertEquals(6, seen.size());
        assertTrue(hits[0] > hits[4] * 2, "fire is much likelier than light");
        assertNotNull(SocketCatalog.byId(SocketLootRules.pickSpirit(1.0)));
        assertNotNull(SocketCatalog.byId(SocketLootRules.pickSpirit(-1.0)));
    }

    @Test
    void devilPickIsEvenAndComplete() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 90; i++) seen.add(SocketLootRules.pickDevil(i / 90.0));
        assertEquals(9, seen.size());
        assertNotNull(SocketCatalog.byId(SocketLootRules.pickDevil(1.0)));
    }
}
