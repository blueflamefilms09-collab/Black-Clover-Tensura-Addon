package com.newuniverse.nusmp.skilltree.loot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRules;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: skill books in the secret rift chests must pay for the pocket vaults ONLY, because the same two loot tables (the relic vault's and the trial's) also fill ordinary chests of the main
 * dungeon. The decision is pure ({@link RiftBookRules}) so each edge is pinned: the chance is the JSON's number times the server's scale, clamped to 0..1, and 0 for an unlisted table, a zero or
 * negative number or scale, or a NaN; "in a pocket" is exactly the dungeon's own {@code RiftRules.slotAt} (first and last cell, the gaps outside); the roll never fires outside the Mega-Dungeon or
 * outside a pocket, always fires at a chance of 1 and never at 0, and lands on its chance over a long seeded run. The shipped JSON is checked too: the modifier is defined with the tables the pocket
 * builder really uses, every chance is a probability, and the type is switched on in the global list next to the three older entries (add the new, keep the old).
 */
class RiftBookRulesTest {
    private static final Map<String, Double> TABLES = Map.of("nusmp:chests/dungeon_relic_vault", 0.2, "nusmp:chests/labyrinth_trial", 0.08);

    private static Dice fixed(double v) { return () -> v; }

    @Test
    void chanceIsTheJsonNumberTimesTheScaleClamped() {
        assertEquals(0.2, RiftBookRules.chance(TABLES, "nusmp:chests/dungeon_relic_vault", 1.0), 1e-12);
        assertEquals(0.4, RiftBookRules.chance(TABLES, "nusmp:chests/dungeon_relic_vault", 2.0), 1e-12);
        assertEquals(1.0, RiftBookRules.chance(TABLES, "nusmp:chests/dungeon_relic_vault", 50.0), "clamped to a probability");
        assertEquals(0.08, RiftBookRules.chance(TABLES, "nusmp:chests/labyrinth_trial", 1.0), 1e-12);
    }

    @Test
    void unlistedZeroNegativeAndNanAreZero() {
        assertEquals(0.0, RiftBookRules.chance(TABLES, "nusmp:chests/hidden_cache", 1.0), "a table the JSON does not list never pays");
        assertEquals(0.0, RiftBookRules.chance(TABLES, "nusmp:chests/dungeon_relic_vault", 0.0));
        assertEquals(0.0, RiftBookRules.chance(TABLES, "nusmp:chests/dungeon_relic_vault", -1.0));
        assertEquals(0.0, RiftBookRules.chance(TABLES, "nusmp:chests/dungeon_relic_vault", Double.NaN));
        assertEquals(0.0, RiftBookRules.chance(Map.of("a", 0.0), "a", 1.0));
        assertEquals(0.0, RiftBookRules.chance(Map.of("a", -0.5), "a", 1.0));
        assertEquals(0.0, RiftBookRules.chance(null, "a", 1.0));
        assertEquals(0.0, RiftBookRules.chance(TABLES, null, 1.0));
    }

    @Test
    void aPositionIsInAPocketExactlyWhereTheRiftsSayItIs() {
        int x0 = RiftRules.POCKET_X0, z = RiftRules.POCKET_Z, stride = RiftRules.POCKET_STRIDE;
        assertTrue(RiftBookRules.inPocket(x0, z));
        assertTrue(RiftBookRules.inPocket(RiftRules.pocketX(0) + 18, z - 2), "the vault chests stand at x + 18, z - 2 .. z + 2");
        assertTrue(RiftBookRules.inPocket(RiftRules.pocketX(RiftRules.MAX_SLOTS - 1), z));
        assertFalse(RiftBookRules.inPocket(RiftRules.pocketX(RiftRules.MAX_SLOTS) + 10, z), "past the last cell");
        assertFalse(RiftBookRules.inPocket(x0 - stride, z), "before the first cell");
        assertFalse(RiftBookRules.inPocket(0, 0), "the ordinary dungeon");
        assertFalse(RiftBookRules.inPocket(x0, z + stride), "beside the field of pockets");
        assertEquals(RiftRules.slotAt(x0 + 5, z) >= 0, RiftBookRules.inPocket(x0 + 5, z));
    }

    @Test
    void theRollOnlyHappensInsideAPocketOfTheMegaDungeon() {
        assertFalse(RiftBookRules.decide(false, true, 1.0, fixed(0.0)), "not the Mega-Dungeon");
        assertFalse(RiftBookRules.decide(true, false, 1.0, fixed(0.0)), "not inside a pocket");
        assertFalse(RiftBookRules.decide(true, true, 0.0, fixed(0.0)));
        assertFalse(RiftBookRules.decide(true, true, -1.0, fixed(0.0)));
        assertFalse(RiftBookRules.decide(true, true, Double.NaN, fixed(0.0)));
        assertFalse(RiftBookRules.decide(true, true, 1.0, null));
        assertTrue(RiftBookRules.decide(true, true, 1.0, fixed(0.9999)), "a chance of 1 always pays");
        assertTrue(RiftBookRules.decide(true, true, 0.2, fixed(0.1999)));
        assertFalse(RiftBookRules.decide(true, true, 0.2, fixed(0.2)), "the dice is compared with <");
    }

    @Test
    void theRollLandsOnItsChance() {
        Random r = new Random(41);
        int n = 200_000, hits = 0;
        for (int i = 0; i < n; i++) if (RiftBookRules.decide(true, true, 0.2, r::nextDouble)) hits++;
        double sigma = Math.sqrt(n * 0.2 * 0.8);
        assertTrue(Math.abs(hits - n * 0.2) < 5 * sigma, hits + " of " + n);
    }

    // ------------------------------------------------------------------------------------------------------ the shipped json
    private static JsonObject json(String path) throws Exception {
        try (Reader r = new FileReader(new File(path))) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    @Test
    void theShippedModifierListsThePocketTablesWithProbabilities() throws Exception {
        JsonObject def = json("src/main/resources/data/nusmp/loot_modifiers/rift_books.json");
        assertEquals("nusmp:rift_books", def.get("type").getAsString());
        assertTrue(def.has("conditions"), "the standard conditions list is present (the dimension check, like the gear chests')");
        JsonObject tables = def.getAsJsonObject("tables");
        assertTrue(tables.has("nusmp:chests/dungeon_relic_vault") && tables.has("nusmp:chests/labyrinth_trial"), "the two tables of the pocket vault (PocketBuilder.RELIC_VAULT and TRIAL)");
        double perPocket = 0;
        for (var e : tables.entrySet()) {
            double p = e.getValue().getAsDouble();
            assertTrue(p > 0 && p <= 1, e.getKey() + " chance " + p);
            assertTrue(new File("src/main/resources/data/nusmp/loot_table/" + e.getKey().substring("nusmp:".length()) + ".json").isFile(), "no such loot table: " + e.getKey());
        }
        // a pocket vault has one relic-vault chest and two trial chests: the expected books per vault
        perPocket = tables.get("nusmp:chests/dungeon_relic_vault").getAsDouble() + 2 * tables.get("nusmp:chests/labyrinth_trial").getAsDouble();
        assertTrue(perPocket > 0.1 && perPocket < 1.0, "expected books per pocket vault " + perPocket + ": a rare reward, not a given and not a shower");
    }

    @Test
    void theModifierIsSwitchedOnAndTheOlderEntriesStay() throws Exception {
        JsonObject list = json("src/main/resources/data/neoforge/loot_modifiers/global_loot_modifiers.json");
        assertFalse(list.get("replace").getAsBoolean());
        String entries = list.getAsJsonArray("entries").toString();
        for (String e : new String[] {"nusmp:relic_chests", "nusmp:gear_chests", "nusmp:skill_books", "nusmp:rift_books"}) assertTrue(entries.contains("\"" + e + "\""), e + " must be in the global list");
    }
}
