package com.newuniverse.nusmp.labyrinth.mega.foundry;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the glue between the Dungeon Master and the Foundry decides two things the owner can see: how often a spawn becomes a foundry mob (the config weight, default 60 percent) and which
 * entity type the Director must create for a foundry key. Both are pure and are pinned here: a weight of 0 never replaces and 100 always does, 60 gives about 60 percent over many draws,
 * a foundry key maps to the one entity type and every other id is passed through unchanged.
 */
public class FoundryEncountersTest {
    @Test
    public void theWeightIsThePercentOfSpawnsThatBecomeFoundryMobs() {
        Random r = new Random(11);
        for (int i = 0; i < 500; i++) {
            assertFalse(FoundryRoster.chance(0, r));
            assertTrue(FoundryRoster.chance(100, r));
        }
        int hit = 0, n = 20000;
        for (int i = 0; i < n; i++) if (FoundryRoster.chance(60, r)) hit++;
        assertTrue(Math.abs(hit / (double) n - 0.60) < 0.02, "60 percent expected, got " + hit / (double) n);
        assertFalse(FoundryRoster.chance(-5, r));
    }

    @Test
    public void aFoundryKeyMapsToTheOneEntityTypeAndOthersPassThrough() {
        assertEquals("nusmp:foundry_mob", FoundryDef.baseId("nusmp:foundry_mob#tunnel_kobold"));
        assertEquals("minecraft:zombie", FoundryDef.baseId("minecraft:zombie"));
        assertEquals("nusmp:foundry_mob#Bad Id", FoundryDef.baseId("nusmp:foundry_mob#Bad Id"));
    }
}
