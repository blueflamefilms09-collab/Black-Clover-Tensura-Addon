package com.newuniverse.nusmp.labyrinth.props;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the rare stand must wear armour that belongs to its floor and exists in the dungeon's loot system (docs/labyrinth/rooms_rebuilt.md): iron on the first ten floors, Drake-Scale from
 * floor 10, Shadowsteel from floor 30, two to four pieces, always the chest piece, rolled with a rarity that rises with the band. The bands are the loot system's own cut points; the item ids
 * are checked against the loot roster file, so a renamed item fails the build and not the game.
 */
class RareStandSetTest {
    @Test
    void theBandsAreTheLootSystemsBands() {
        int[][] cases = {{0, 0}, {9, 0}, {10, 1}, {29, 1}, {30, 2}, {99, 2}, {100, 3}, {299, 3}, {300, 4}, {2000, 4}, {-5, 0}, {-30, 2}, {-1000, 4}};
        for (int[] c : cases) assertEquals(c[1], RareStandSet.band(c[0]), "depth " + c[0]);
    }

    @Test
    void theSetAndTheRarityFollowTheBand() {
        assertEquals("iron", RareStandSet.setOf(0));
        assertEquals("drakescale", RareStandSet.setOf(1));
        for (int b = 2; b <= 4; b++) assertEquals("shadowsteel", RareStandSet.setOf(b));
        assertEquals("", RareStandSet.rarityOf(0));
        assertEquals("uncommon", RareStandSet.rarityOf(1));
        assertEquals("rare", RareStandSet.rarityOf(2));
        assertEquals("epic", RareStandSet.rarityOf(3));
        assertEquals("legendary", RareStandSet.rarityOf(4));
        assertEquals("legendary", RareStandSet.rarityOf(99));
        assertEquals("", RareStandSet.rarityOf(-4));
    }

    @Test
    void aStandWearsTwoToFourPiecesAlwaysTheChestAndOneOfEachSlot() {
        int[] count = new int[5];
        for (int depth : new int[] {1, 12, 35, 150, 500}) {
            for (long seed = 0; seed < 2000; seed++) {
                List<RareStandSet.Piece> p = RareStandSet.pieces(depth, seed);
                assertTrue(p.size() >= 2 && p.size() <= 4, "pieces " + p.size());
                count[p.size()]++;
                Set<String> slots = new HashSet<>();
                for (RareStandSet.Piece x : p) assertTrue(slots.add(x.slot()), "one piece per slot: " + p);
                assertTrue(slots.contains("chest"), "always the chest piece: " + p);
                assertEquals(p, RareStandSet.pieces(depth, seed), "stable");
            }
        }
        // 40 / 40 / 20 percent
        double n = count[2] + count[3] + count[4];
        assertTrue(Math.abs(count[2] / n - 0.4) < 0.03 && Math.abs(count[3] / n - 0.4) < 0.03 && Math.abs(count[4] / n - 0.2) < 0.03, "the shares of 2, 3 and 4 pieces: " + count[2] / n + " " + count[3] / n + " " + count[4] / n);
    }

    @Test
    void everyItemIsOneOfTheDungeonsOwnArmourOrVanillaIron() throws IOException {
        JsonObject roster = JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/nusmp/loot/roster.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        Set<String> loot = new HashSet<>();
        JsonArray armour = roster.getAsJsonArray("armour");
        for (JsonElement e : armour) loot.add("nusmp:" + e.getAsJsonObject().get("id").getAsString());
        Set<String> iron = Set.of("minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots");
        for (int depth : new int[] {0, 12, 35, 150, 500}) {
            for (long seed = 0; seed < 300; seed++) {
                for (RareStandSet.Piece p : RareStandSet.pieces(depth, seed)) {
                    assertTrue(loot.contains(p.item()) || iron.contains(p.item()), "unknown armour item " + p.item());
                    if (RareStandSet.band(depth) == 0) assertTrue(iron.contains(p.item()) && p.rarity().isEmpty(), p.toString());
                    else assertTrue(loot.contains(p.item()) && !p.rarity().isEmpty(), p.toString());
                }
            }
        }
        // every slot of every set resolves
        for (String set : List.of("iron", "drakescale", "shadowsteel")) {
            for (String slot : List.of("head", "chest", "legs", "feet")) {
                String id = RareStandSet.itemOf(set, slot);
                assertTrue(loot.contains(id) || iron.contains(id), set + "/" + slot + " -> " + id);
            }
        }
        assertFalse(loot.isEmpty());
    }
}
